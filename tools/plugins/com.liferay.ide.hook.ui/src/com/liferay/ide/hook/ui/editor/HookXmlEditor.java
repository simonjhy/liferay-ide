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

package com.liferay.ide.hook.ui.editor;

import static com.liferay.ide.core.util.CoreUtil.empty;

import com.liferay.ide.core.ILiferayPortal;
import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.IWebProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.SapphireContentAccessor;
import com.liferay.ide.core.util.SapphireUtil;
import com.liferay.ide.hook.core.model.CustomJsp;
import com.liferay.ide.hook.core.model.CustomJspDir;
import com.liferay.ide.hook.core.model.Hook;
import com.liferay.ide.hook.core.model.Hook6xx;
import com.liferay.ide.hook.core.util.HookUtil;
import com.liferay.ide.hook.ui.HookUI;
import com.liferay.ide.ui.util.UIUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.ElementHandle;
import org.eclipse.sapphire.ElementList;
import org.eclipse.sapphire.FilteredListener;
import org.eclipse.sapphire.Listener;
import org.eclipse.sapphire.PropertyContentEvent;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.modeling.Path;
import org.eclipse.sapphire.ui.swt.xml.editor.SapphireEditorForXml;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @author Kamesh Sampath
 * @author Gregory Amerson
 * @author Simon Jiang
 */
public class HookXmlEditor extends SapphireEditorForXml implements SapphireContentAccessor {

	public HookXmlEditor() {
		super(Hook6xx.TYPE, null);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (customModelDirty) {
			Hook hook = getModelElement().nearest(Hook.class);

			ElementList<CustomJsp> customJsps = hook.getCustomJsps();

			ILiferayProject liferayProject = LiferayCore.create(ILiferayProject.class, getProject());

			ILiferayPortal portal = liferayProject.adapt(ILiferayPortal.class);

			if (portal != null) {
				IPath portalDir = portal.getAppServerPortalDir();

				if (portalDir != null) {
					_copyCustomJspsToProject(portalDir, customJsps);
				}
			}

			customModelDirty = false;

			super.doSave(monitor);

			firePropertyChange(IEditorPart.PROP_DIRTY);

			ElementHandle<CustomJspDir> customJspDir = hook.getCustomJspDir();

			if ((customJspDir != null) && !customJspDir.empty()) {
				CustomJspDir jspDir = customJspDir.content();

				Value<Path> customJspPath = jspDir.getValue();

				Path path = customJspPath.content();

				path = path.makeRelative();

				String customeJspValue = path.toPortableString();

				_configureCustomJspValidation(getProject(), customeJspValue);
			}
		}
		else {
			super.doSave(monitor);
		}
	}

	public InputStream getFileContents() throws CoreException, IOException, MalformedURLException {
		IEditorInput editorInput = getEditorInput();

		if (editorInput instanceof FileEditorInput) {
			IFile file = ((FileEditorInput)editorInput).getFile();

			return file.getContents();
		}
		else if (editorInput instanceof IStorageEditorInput) {
			IStorage storage = ((IStorageEditorInput)editorInput).getStorage();

			return storage.getContents();
		}
		else if (editorInput instanceof FileStoreEditorInput) {
			URI uri = ((FileStoreEditorInput)editorInput).getURI();

			URL url = uri.toURL();

			return url.openStream();
		}
		else {
			return null;
		}
	}

	@Override
	public boolean isDirty() {
		if (customModelDirty) {
			return true;
		}

		return super.isDirty();
	}

	@Override
	protected void adaptModel(Element model) {
		super.adaptModel(model);

		Listener listener = new FilteredListener<PropertyContentEvent>() {

			@Override
			public void handleTypedEvent(PropertyContentEvent event) {
				handleCustomJspsPropertyChangedEvent(event);
			}

		};

		_ignoreCustomModelChanges = true;
		model.attach(listener, Hook.PROP_CUSTOM_JSPS.name() + "/*"); //$NON-NLS-1$
		_ignoreCustomModelChanges = false;
	}

	@Override
	protected void createFormPages() throws PartInitException {
		addDeferredPage(1, "Overview", "HookConfigurationPage");
	}

	protected void handleCustomJspsPropertyChangedEvent(PropertyContentEvent event) {
		if (_ignoreCustomModelChanges) {
			return;
		}

		customModelDirty = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	protected void pageChange(int pageIndex) {
		_ignoreCustomModelChanges = true;
		super.pageChange(pageIndex);

		_ignoreCustomModelChanges = false;
	}

	protected boolean customModelDirty = false;

	private void _configureCustomJspValidation(IProject project, String customerJspPath) {
		IFolder docFolder = CoreUtil.getDefaultDocrootFolder(project);

		if (docFolder != null) {
			IPath newPath = org.eclipse.core.runtime.Path.fromOSString(customerJspPath);

			IPath fullPath = docFolder.getFullPath();

			IPath pathValue = fullPath.append(newPath);

			IFolder customJspFolder = project.getFolder(pathValue.makeRelativeTo(project.getFullPath()));

			boolean needAddCustomJspValidation = HookUtil.configureJSPSyntaxValidationExclude(
				project, customJspFolder, false);

			if (!needAddCustomJspValidation) {
				UIUtil.async(
					new Runnable() {

						public void run() {
							boolean addDisableCustomJspValidation = MessageDialog.openQuestion(
								UIUtil.getActiveShell(), Msgs.disableCustomValidationTitle,
								Msgs.disableCustomValidationMsg);

							if (addDisableCustomJspValidation) {
								new WorkspaceJob(
									" disable custom jsp validation for " + project.getName()
								) {

									@Override
									public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
										HookUtil.configureJSPSyntaxValidationExclude(project, customJspFolder, true);
										project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

										return Status.OK_STATUS;
									}

								}.schedule();
							}
						}

					});
			}
		}
	}

	private void _copyCustomJspsToProject(IPath portalDir, ElementList<CustomJsp> customJsps) {
		try {
			Hook hook = getModelElement().nearest(Hook.class);

			ElementHandle<CustomJspDir> element = hook.getCustomJspDir();

			CustomJspDir customJspDirElement = element.content();

			if (SapphireUtil.ok(customJspDirElement)) {
				Path customJspDir = get(customJspDirElement.getValue());

				IWebProject webproject = LiferayCore.create(IWebProject.class, getProject());

				if (webproject != null) {
					IFolder defaultDocroot = webproject.getDefaultDocrootFolder();

					IFolder customJspFolder = defaultDocroot.getFolder(customJspDir.toPortableString());

					for (CustomJsp customJsp : customJsps) {
						String content = get(customJsp.getValue());

						if (!empty(content)) {
							IFile customJspFile = customJspFolder.getFile(content);

							if (FileUtil.notExists(customJspFile)) {
								IPath portalJsp = portalDir.append(content);

								try {
									CoreUtil.makeFolders((IFolder)customJspFile.getParent());

									if (FileUtil.exists(portalJsp)) {
										File portalJspFile = portalJsp.toFile();

										try (InputStream jspInputStream = FileUtil.newInputStream(
												portalJspFile.toPath())) {

											customJspFile.create(jspInputStream, true, null);
										}
									}
									else {
										CoreUtil.createEmptyFile(customJspFile);
									}
								}
								catch (Exception e) {
									HookUI.logError(e);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			HookUI.logError(e);
		}
	}

	private boolean _ignoreCustomModelChanges;

	private static class Msgs extends NLS {

		public static String disableCustomValidationMsg;
		public static String disableCustomValidationTitle;

		static {
			initializeMessages(HookXmlEditor.class.getName(), Msgs.class);
		}

	}

}