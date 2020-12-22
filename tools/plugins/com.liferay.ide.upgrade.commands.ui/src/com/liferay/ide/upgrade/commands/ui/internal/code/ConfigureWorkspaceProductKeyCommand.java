/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.upgrade.commands.ui.internal.code;

import com.liferay.ide.core.ILiferayProjectProvider;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.util.DownloadUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.JobUtil;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.core.workspace.WorkspaceConstants;
import com.liferay.ide.project.core.modules.BladeCLI;
import com.liferay.ide.ui.dialog.StringsFilteredDialog;
import com.liferay.ide.ui.util.UIUtil;
import com.liferay.ide.upgrade.commands.core.code.ConfigureWorkspaceProductKeyCommandKeys;
import com.liferay.ide.upgrade.commands.ui.internal.UpgradeCommandsUIPlugin;
import com.liferay.ide.upgrade.plan.core.ResourceSelection;
import com.liferay.ide.upgrade.plan.core.UpgradeCommand;
import com.liferay.ide.upgrade.plan.core.UpgradeCompare;
import com.liferay.ide.upgrade.plan.core.UpgradePlan;
import com.liferay.ide.upgrade.plan.core.UpgradePlanner;
import com.liferay.ide.upgrade.plan.core.UpgradePreview;

import java.io.File;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.configuration.PropertiesConfiguration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Seiphon Wang
 */
@Component(
	property = "id=" + ConfigureWorkspaceProductKeyCommandKeys.ID, scope = ServiceScope.PROTOTYPE,
	service = {UpgradeCommand.class, UpgradePreview.class}
)
public class ConfigureWorkspaceProductKeyCommand implements UpgradeCommand, UpgradePreview {

	@Override
	public IStatus perform(IProgressMonitor progressMonitor) {
		File gradleProperties = _getGradlePropertiesFile();

		if (gradleProperties == null) {
			return Status.CANCEL_STATUS;
		}

		IProject workspace = LiferayWorkspaceUtil.getWorkspaceProject();

		IPath location = workspace.getLocation();

		IStatus retVal = _updateWorkspaceProductKeyValue(gradleProperties);

		String productKey = LiferayWorkspaceUtil.getGradleProperty(
			location.toOSString(), WorkspaceConstants.WORKSPACE_PRODUCT_PROPERTY, null);

		try {
			_downloadReleaseApiJar(productKey);
		}
		catch (Exception e) {
		}

		return retVal;
	}

	@Override
	public void preview(IProgressMonitor progressMonitor) {
		File gradeProperties = _getGradlePropertiesFile();

		if (gradeProperties == null) {
			return;
		}

		File tempDir = getTempDir();

		FileUtil.copyFileToDir(gradeProperties, "gradle.properties-preview", tempDir);

		File tempFile = new File(tempDir, "gradle.properties-preview");

		_updateWorkspaceProductKeyValue(tempFile);

		UIUtil.async(() -> _upgradeCompare.openCompareEditor(gradeProperties, tempFile));
	}

	public class AsyncStringsSelectionValidator implements ISelectionStatusValidator {

		public AsyncStringsSelectionValidator(boolean multiSelect) {
		}

		public IStatus validate(Object[] selection) {
			if ((selection != null) && (selection.length > 0)) {
				String selectionItem = (String)selection[0];

				if (Objects.equals("Loading Data......", selectionItem)) {
					return new Status(IStatus.ERROR, "unknown", 1, "", null);
				}
			}
			else {
				return new Status(IStatus.ERROR, "unknown", 1, "", null);
			}

			return Status.OK_STATUS;
		}

	}

	private void _downloadReleaseApiJar(String productKey) throws URISyntaxException {
		if (productKey == null) {
			return;
		}

		String productType = productKey.substring(0, productKey.indexOf("-"));

		IWorkspaceProject gradleWorkspace = LiferayWorkspaceUtil.getGradleWorkspaceProject();

		String targetPlatformVersion = gradleWorkspace.getOriginalTargetPlatformVersion();

		_downloadReleaseCompileOnlyPom(targetPlatformVersion, productType);

		Job job = new Job("download release api job") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String releaseApiUrl = _getDownloadReleaseApiUrl(targetPlatformVersion, productType);

					URI uri = new URI(releaseApiUrl);

					File cacheDir = new File(System.getProperty("user.home"), ".liferay-ide/release-api/" + productKey);

					DownloadUtil.download(uri, cacheDir.toPath());

					File tempCacheDir = new File(System.getProperty("user.home"), ".liferay-ide/temp");

					FileUtil.deleteDirContents(tempCacheDir);
				}
				catch (Exception e) {
				}

				return Status.OK_STATUS;
			}

		};

		job.schedule();
	}

	private void _downloadReleaseCompileOnlyPom(String targetPlatformVersion, String productType)
		throws URISyntaxException {

		String pomUrl = _getReleaseCompileOnlyPomUrl(targetPlatformVersion, productType);

		URI uri = new URI(pomUrl);

		File tempCacheDir = new File(System.getProperty("user.home"), ".liferay-ide/temp");

		Job job = new Job("download release api job") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					DownloadUtil.download(uri, tempCacheDir.toPath());
				}
				catch (Exception e) {
				}

				return Status.OK_STATUS;
			}

		};

		job.setProperty(ILiferayProjectProvider.LIFERAY_PROJECT_JOB, new Object());

		job.schedule();
	}

	private String _getDownloadReleaseApiUrl(String targetPlatformVersion, String productType) {
		StringBuilder urlStringBuilder = new StringBuilder(_urlBaseString);

		urlStringBuilder.append(productType);
		urlStringBuilder.append(".api/");

		String releaseApiVersion = _getReleaseApiVersion(targetPlatformVersion, productType);

		urlStringBuilder.append(releaseApiVersion);

		urlStringBuilder.append("/release.");
		urlStringBuilder.append(productType);
		urlStringBuilder.append(".api-");
		urlStringBuilder.append(releaseApiVersion);
		urlStringBuilder.append(".jar");

		return urlStringBuilder.toString();
	}

	private File _getGradlePropertiesFile() {
		List<IProject> projects = _resourceSelection.selectProjects(
			"Select Liferay Workspace Project", false, ResourceSelection.WORKSPACE_PROJECTS);

		if (projects.isEmpty()) {
			return null;
		}

		IProject project = projects.get(0);

		return FileUtil.getFile(project.getFile("gradle.properties"));
	}

	private String _getReleaseApiVersion(String targetPlatformVersion, String productType) {
		String artifactId = "release." + productType + ".api";

		File pomFile = new File(
			System.getProperty("user.home"),
			".liferay-ide/temp/release." + productType + ".bom.compile.only-" + targetPlatformVersion + ".pom");

		if (!pomFile.exists()) {
			JobUtil.waitForLiferayProjectJob();
		}

		Document document = FileUtil.readXMLFile(pomFile);

		NodeList dependencyNodeList = document.getElementsByTagName("dependency");

		for (int i = 0; i < dependencyNodeList.getLength(); i++) {
			Node dependencyNode = dependencyNodeList.item(i);

			if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
				Element dependencyElement = (Element)dependencyNode;

				NodeList artifactIdNodeList = dependencyElement.getElementsByTagName("artifactId");

				Element artifactIdElement = (Element)artifactIdNodeList.item(0);

				if (Objects.equals(artifactId, artifactIdElement.getTextContent())) {
					NodeList versionNodeList = dependencyElement.getElementsByTagName("version");

					Element versionElement = (Element)versionNodeList.item(0);

					return versionElement.getTextContent();
				}
			}
		}

		return null;
	}

	private String _getReleaseCompileOnlyPomUrl(String targetPlatformVersion, String productType) {
		StringBuilder urlStringBuilder = new StringBuilder(
			"https://repository-cdn.liferay.com/nexus/content/repositories/liferay-public-releases/com/liferay/portal" +
				"/release.");

		urlStringBuilder.append(productType);
		urlStringBuilder.append(".bom.compile.only/");
		urlStringBuilder.append(targetPlatformVersion);
		urlStringBuilder.append("/release.");
		urlStringBuilder.append(productType);
		urlStringBuilder.append(".bom.compile.only-");
		urlStringBuilder.append(targetPlatformVersion);
		urlStringBuilder.append(".pom");

		return urlStringBuilder.toString();
	}

	private void _loadWorkspaceProduct(TreeViewer viewer, String targetPlatformVersion) {
		try {
			UIUtil.async(
				() -> {
					if (viewer == null) {
						return;
					}

					Tree tree = viewer.getTree();

					if (tree.isDisposed()) {
						return;
					}

					try {
						String[] filterProductKeys = Stream.of(
							BladeCLI.getWorkspaceProducts(true)
						).filter(
							key -> key.contains(targetPlatformVersion)
						).collect(
							Collectors.toList()
						).toArray(
							new String[0]
						);

						viewer.setInput(filterProductKeys);
					}
					catch (Exception e) {
						UpgradeCommandsUIPlugin.logError("Failed to load workspace product keys", e);
					}
				});
		}
		catch (Exception e) {
			UpgradeCommandsUIPlugin.logError(e.getMessage());
		}
	}

	private IStatus _updateWorkspaceProductKeyValue(File gradeProperties) {
		UpgradePlan upgradePlan = _upgradePlanner.getCurrentUpgradePlan();

		String targetVersion = upgradePlan.getTargetVersion();

		try {
			final AtomicInteger returnCode = new AtomicInteger();

			final AtomicReference<String> productKey = new AtomicReference<>();

			UIUtil.sync(
				() -> {
					IWorkbench workbench = PlatformUI.getWorkbench();

					IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

					AsyncStringFilteredDialog dialog = new AsyncStringFilteredDialog(
						workbenchWindow.getShell(), targetVersion);

					dialog.setInput(new String[] {"Loading Data......"});
					dialog.setMessage("Liferay Product Key Selection");
					dialog.setTitle("Please select a Liferay Product Key:");
					dialog.setStatusLineAboveButtons(false);
					dialog.setHelpAvailable(false);

					returnCode.set(dialog.open());
					productKey.set((String)dialog.getFirstResult());
				});

			if (returnCode.get() == Window.OK) {
				try {
					PropertiesConfiguration config = new PropertiesConfiguration(gradeProperties);

					config.setProperty(WorkspaceConstants.WORKSPACE_PRODUCT_PROPERTY, productKey);

					config.save();
				}
				catch (Exception e) {
					return UpgradeCommandsUIPlugin.createErrorStatus("Unable to save workspace product key", e);
				}
			}

			return Status.OK_STATUS;
		}
		catch (Exception e) {
			return UpgradeCommandsUIPlugin.createErrorStatus("Unable to configure workspace product key", e);
		}
	}

	@Reference
	private ResourceSelection _resourceSelection;

	@Reference
	private UpgradeCompare _upgradeCompare;

	@Reference
	private UpgradePlanner _upgradePlanner;

	private String _urlBaseString =
		"https://repository-cdn.liferay.com/nexus/content/repositories/liferay-public-releases/com/liferay/portal" +
			"/release.";

	@SuppressWarnings("restriction")
	private class AsyncStringFilteredDialog extends StringsFilteredDialog {

		public AsyncStringFilteredDialog(Shell shell, String targetPlatforVersion) {
			super(shell, null);

			setValidator(new AsyncStringsSelectionValidator(false));

			_targetPlatformVersion = targetPlatforVersion;
		}

		@Override
		protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
			TreeViewer treeViewer = super.doCreateTreeViewer(parent, style);

			_loadWorkspaceProduct(treeViewer, _targetPlatformVersion);

			return treeViewer;
		}

		@Override
		protected void updateStatus(IStatus status) {
			updateButtonsEnableState(status);
		}

		private String _targetPlatformVersion;

	}

}