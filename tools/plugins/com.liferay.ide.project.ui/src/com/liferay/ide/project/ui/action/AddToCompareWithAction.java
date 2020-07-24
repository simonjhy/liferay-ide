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

package com.liferay.ide.project.ui.action;

import com.liferay.ide.core.Artifact;
import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.IProjectBuilder;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.StringUtil;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.util.ProjectUtil;
import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.core.portal.PortalRuntime;
import com.liferay.ide.server.util.ServerUtil;
import com.liferay.ide.ui.action.AbstractObjectAction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerCore;

/**
 * @author Ethan Sun
 */
public class AddToCompareWithAction extends AbstractObjectAction {

	@Override
	public void run(IAction action) {
		try (ZipFile zipFile = new ZipFile(_sourceFile)) {
			String entryCanonicalName = _sourceEntry.getName();

			String entryName = entryCanonicalName.substring(entryCanonicalName.lastIndexOf("/") + 1);

			Path sourceEntryPath = Files.createTempFile(entryName, ".tmp");

			File sourceEntryFile = sourceEntryPath.toFile();

			FileUtils.copyInputStreamToFile(zipFile.getInputStream(_sourceEntry), sourceEntryFile);

			CompareItem sourceCompareItem = new CompareItem(sourceEntryFile);

			CompareItem targetCompareItem = new CompareItem(FileUtil.getFile(_selectedFile));

			CompareConfiguration compareConfiguration = new CompareConfiguration();

			compareConfiguration.setLeftLabel("Updated File");

			compareConfiguration.setRightLabel("Original File");

			CompareEditorInput compareEditorInput = new CompareEditorInput(compareConfiguration) {

				public Viewer findStructureViewer(Viewer oldViewer, ICompareInput input, Composite parent) {
					return null;
				}

				@Override
				protected Object prepareInput(IProgressMonitor monitor)
					throws InterruptedException, InvocationTargetException {

					return new DiffNode(targetCompareItem, sourceCompareItem);
				}

			};

			compareEditorInput.setTitle("Compare ('" + _selectedFile.getName() + "'-'" + _sourceFile.getName() + "')");

			CompareUI.openCompareEditor(compareEditorInput);

			_exist = false;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof IFile) {
					_selectedFile = (IFile)obj;

					_project = _selectedFile.getProject();

					try {
						if (ProjectUtil.isFragmentProject(_project)) {
							Map<String, String> fragmentProjectInfo = ProjectUtil.getFragmentProjectInfo(_project);

							String hostBundleName = fragmentProjectInfo.get("HostOSGiBundleName");

							String portalBundleVersion = fragmentProjectInfo.get("Portal-Bundle-Version");

							ProjectCore projectCore = ProjectCore.getDefault();

							IPath projectCoreLocation = projectCore.getStateLocation();

							String hostOsgiJar = hostBundleName + ".jar";

							IWorkspaceProject liferayWorkspaceProject =
								LiferayWorkspaceUtil.getLiferayWorkspaceProject();

							IRuntime[] runtimes = ServerCore.getRuntimes();

							IRuntime value = null;

							for (IRuntime runtime : runtimes) {
								if (LiferayServerCore.newPortalBundle(runtime.getLocation()) == null) {
									continue;
								}

								if (CoreUtil.isNotNullOrEmpty(portalBundleVersion)) {
									PortalRuntime portalRuntime = (PortalRuntime)runtime.loadAdapter(
										PortalRuntime.class, new NullProgressMonitor());

									if (!Objects.equals(portalBundleVersion, portalRuntime.getPortalVersion())) {
										continue;
									}
								}
								else {
									break;
								}

								if (liferayWorkspaceProject != null) {
									IPath bundleHomePath = LiferayWorkspaceUtil.getBundleHomePath(
										liferayWorkspaceProject.getProject());

									if (Objects.isNull(bundleHomePath)) {
										continue;
									}

									IPath runtimeLocation = runtime.getLocation();

									if (bundleHomePath.equals(runtimeLocation)) {
										value = runtime;

										break;
									}
								}
								else {
									value = runtime;

									break;
								}
							}

							_sourceFile = ServerUtil.getModuleFileFrom70Server(value, hostOsgiJar, projectCoreLocation);
						}
						else if (ProjectUtil.isModuleExtProject(_project)) {
							ILiferayProject liferayProject = LiferayCore.create(ILiferayProject.class, _project);

							IProjectBuilder projectBuilder = liferayProject.adapt(IProjectBuilder.class);

							if (projectBuilder == null) {
								ProjectCore.logWarning("Please wait for synchronized jobs to finish.");

								return;
							}

							List<Artifact> dependencies = projectBuilder.getDependencies("originalModule");

							if (!dependencies.isEmpty()) {
								Artifact artifact = dependencies.get(0);

								_sourceFile = artifact.getSource();
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}

					_searchTargetFile(_sourceFile);

					IPath projectRelativePath = _selectedFile.getProjectRelativePath();

					String projectRelativePathStr = projectRelativePath.toString();

					action.setEnabled(false);

					if (_exist && projectRelativePathStr.startsWith("src/main/")) {
						action.setEnabled(true);
					}
				}
			}
		}
	}

	private void _searchTargetFile(File sourceFile) {
		if (FileUtil.exists(sourceFile)) {
			try (ZipFile zipFile = new ZipFile(sourceFile)) {
				ZipEntry entry = null;

				Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

				while (enumeration.hasMoreElements()) {
					entry = enumeration.nextElement();

					String entryCanonicalName = entry.getName();

					IPath relativePath = _selectedFile.getProjectRelativePath();

					if (!entry.isDirectory() && StringUtil.contains(relativePath.toString(), entryCanonicalName)) {
						_exist = true;

						_sourceEntry = entry;

						break;
					}
				}
			}
			catch (IOException ioe) {
				ProjectUI.logError("Failed to compare with original file for project " + _project.getName(), ioe);
			}
		}
	}

	private boolean _exist;
	private IProject _project;
	private IFile _selectedFile;
	private ZipEntry _sourceEntry;
	private File _sourceFile;

	private class CompareItem implements ITypedElement, IStreamContentAccessor, IModificationDate, IEditableContent {

		public CompareItem(File file) {
			_file = file;
		}

		@Override
		public InputStream getContents() throws CoreException {
			try {
				return Files.newInputStream(_file.toPath());
			}
			catch (Exception e) {
			}

			return null;
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public long getModificationDate() {
			return 0;
		}

		@Override
		public String getName() {
			return _file.getName();
		}

		@Override
		public String getType() {
			return TEXT_TYPE;
		}

		@Override
		public boolean isEditable() {
			return false;
		}

		@Override
		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			return null;
		}

		@Override
		public void setContent(byte[] newContent) {
		}

		private File _file;

	}

}