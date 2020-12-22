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

package com.liferay.ide.gradle.ui.upgrade;

import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.core.workspace.WorkspaceConstants;
import com.liferay.ide.gradle.core.GradleUtil;
import com.liferay.ide.gradle.core.model.GradleBuildScript;
import com.liferay.ide.gradle.core.model.GradleDependency;
import com.liferay.ide.gradle.ui.LiferayGradleUI;
import com.liferay.ide.upgrade.commands.core.code.SwitchToUseReleaseAPIDependencyCommandKeys;
import com.liferay.ide.upgrade.plan.core.ResourceSelection;
import com.liferay.ide.upgrade.plan.core.UpgradeCommand;
import com.liferay.ide.upgrade.plan.core.UpgradeCommandPerformedEvent;
import com.liferay.ide.upgrade.plan.core.UpgradePlanner;
import com.liferay.ide.upgrade.plan.core.UpgradePreview;
import com.liferay.ide.upgrade.plan.core.UpgradeProblemSupport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Seiphon Wang
 */
@Component(
	property = "id=" + SwitchToUseReleaseAPIDependencyCommandKeys.ID, scope = ServiceScope.PROTOTYPE,
	service = UpgradeCommand.class
)
public class SwitchToUseReleaseAPIDependencyCommand implements UpgradeCommand, UpgradePreview, UpgradeProblemSupport {

	@Override
	public IStatus perform(IProgressMonitor progressMonitor) {
		List<IProject> projects = _resourceSelection.selectProjects(
			"Select a Liferay Project", true, ResourceSelection.LIFERAY_PROJECTS);

		Collection<File> buildGradleFiles = GradleUtil.getBuildGradleFiles(projects);

		if (buildGradleFiles == null) {
			return Status.CANCEL_STATUS;
		}

		Stream<File> buildGradleStrem = buildGradleFiles.stream();

		buildGradleStrem.forEach(this::_replaceDependencisWithReleaseAPI);

		GradleUtil.refreshProject(LiferayWorkspaceUtil.getWorkspaceProject());

		_upgradePlanner.dispatch(new UpgradeCommandPerformedEvent(this, Collections.singletonList(buildGradleFiles)));

		return Status.OK_STATUS;
	}

	@Override
	public void preview(IProgressMonitor progressMonitor) {
	}

	private List<String> _getAllDependenciesArtifactIds() {
		IProject workspace = LiferayWorkspaceUtil.getWorkspaceProject();

		IPath location = workspace.getLocation();

		String productKey = LiferayWorkspaceUtil.getGradleProperty(
			location.toOSString(), WorkspaceConstants.WORKSPACE_PRODUCT_PROPERTY, null);

		List<String> allArtifactIds = _getAllDependenciesFromLocal(productKey);

		if (allArtifactIds.isEmpty()) {
			allArtifactIds = _getAllDependenciesFromJarFile(productKey);
		}

		return allArtifactIds;
	}

	private List<String> _getAllDependenciesFromJarFile(String productKey) {
		List<String> allArtifactIds = new ArrayList<>();

		File cacheDir = new File(System.getProperty("user.home"), ".liferay-ide/release-api/" + productKey);

		if (!cacheDir.exists()) {
			return allArtifactIds;
		}

		String[] jarFileNames = cacheDir.list();

		List<String> jarFileNameList = new ArrayList<>();

		for (String jarFileName : jarFileNames) {
			if (jarFileName.startsWith("release.dxp.api-") || jarFileName.startsWith("release.portal.api-")) {
				jarFileNameList.add(jarFileName);
			}
		}

		File releaseApiJar = null;

		if (!jarFileNameList.isEmpty()) {
			releaseApiJar = new File(cacheDir, jarFileNameList.get(0));
		}

		if ((releaseApiJar != null) && releaseApiJar.exists()) {
			try (ZipFile zipFile = new ZipFile(releaseApiJar)) {
				Enumeration<? extends ZipEntry> entries = zipFile.entries();

				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();

					String entryName = entry.getName();

					if (Objects.equals(entryName, "versions.txt")) {
						try (InputStream inputStream = zipFile.getInputStream(entry);
							BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {

							String dependency = null;

							while ((dependency = bufferedReader.readLine()) != null) {
								String[] segments = dependency.split(":");

								allArtifactIds.add(segments[1]);
							}
						}
					}
				}
			}
			catch (IOException ioe) {
			}
		}

		return allArtifactIds;
	}

	private List<String> _getAllDependenciesFromLocal(String productKey) {
		List<String> allArtifactIds = new ArrayList<>();

		Class<?> clazz = SwitchToUseReleaseAPIDependencyCommand.class;

		try (InputStream inputStream = clazz.getResourceAsStream("/release-api/" + productKey + "/versions.txt");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {

			String dependency = null;

			while ((dependency = bufferedReader.readLine()) != null) {
				String[] segments = dependency.split(":");

				allArtifactIds.add(segments[1]);
			}
		}
		catch (Exception e) {
		}

		return allArtifactIds;
	}

	private GradleBuildScript _getGradleBuildScript(File file) {
		GradleBuildScript gradleBuildScript = null;

		try {
			gradleBuildScript = new GradleBuildScript(file);
		}
		catch (IOException ioe) {
			return null;
		}

		return gradleBuildScript;
	}

	private void _replaceDependencisWithReleaseAPI(File buildGradleFile) {
		GradleBuildScript gradleBuildScript = _getGradleBuildScript(buildGradleFile);

		List<String> allArtifactIds = _getAllDependenciesArtifactIds();

		if ((gradleBuildScript == null) || allArtifactIds.isEmpty()) {
			return;
		}

		List<GradleDependency> gradleDependencies = gradleBuildScript.getDependencies();

		Stream<GradleDependency> stream = gradleDependencies.stream();

		List<GradleDependency> dependencies = stream.filter(
			dep -> allArtifactIds.contains(dep.getName())
		).collect(
			Collectors.toList()
		);

		try {
			gradleBuildScript.deleteDependency(dependencies);

			FileUtils.writeLines(buildGradleFile, gradleBuildScript.getFileContents());
		}
		catch (Exception e) {
			LiferayGradleUI.logError(e);
		}

		String configuration = "compileOnly";
		String groupId = "com.liferay.portal";
		String artifactId = _portalArtifactId;

		IWorkspaceProject gradleWorkspace = LiferayWorkspaceUtil.getGradleWorkspaceProject();

		if (gradleWorkspace != null) {
			String productVersion = gradleWorkspace.getProperty(WorkspaceConstants.WORKSPACE_PRODUCT_PROPERTY, null);

			if ((productVersion != null) && productVersion.startsWith("dxp")) {
				artifactId = _dxpArtifactId;
			}
		}

		GradleDependency newDependency = new GradleDependency(configuration, groupId, artifactId, null, 0, 0);

		gradleBuildScript = _getGradleBuildScript(buildGradleFile);

		try {
			gradleBuildScript.insertDependency(newDependency);

			FileUtils.writeLines(buildGradleFile, gradleBuildScript.getFileContents());
		}
		catch (IOException e) {
			LiferayGradleUI.logError(e);
		}
	}

	private String _dxpArtifactId = "release.dxp.api";
	private String _portalArtifactId = "release.portal.api";

	@Reference
	private ResourceSelection _resourceSelection;

	@Reference
	private UpgradePlanner _upgradePlanner;

}