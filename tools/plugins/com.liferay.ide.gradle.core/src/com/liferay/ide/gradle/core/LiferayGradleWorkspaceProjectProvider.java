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

package com.liferay.ide.gradle.core;

import com.liferay.ide.core.AbstractLiferayProjectProvider;
import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.TargetPlatformDependency;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.BladeCLI;
import com.liferay.ide.project.core.modules.BladeCLIException;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.workspace.BaseLiferayWorkspaceOp;
import com.liferay.ide.project.core.workspace.NewLiferayWorkspaceOp;
import com.liferay.ide.project.core.workspace.NewLiferayWorkspaceProjectProvider;
import com.liferay.ide.server.util.JavaUtil;
import com.liferay.ide.server.util.ServerUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.sapphire.platform.PathBridge;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.IMemento;
import org.eclipse.wst.server.core.internal.XMLMemento;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.EclipseExternalDependency;
import org.gradle.tooling.model.eclipse.EclipseProject;

/**
 * @author Andy Wu
 * @author Terry Jia
 */
public class LiferayGradleWorkspaceProjectProvider
	extends AbstractLiferayProjectProvider implements NewLiferayWorkspaceProjectProvider<NewLiferayWorkspaceOp> {

	public LiferayGradleWorkspaceProjectProvider() {
		super(new Class<?>[] {IProject.class, IServer.class});
	}

	@Override
	public IStatus createNewProject(NewLiferayWorkspaceOp op, IProgressMonitor monitor) throws CoreException {
		IPath location = PathBridge.create(op.getLocation().content());
		String wsName = op.getWorkspaceName().toString();

		IPath wsLocation = location.append(wsName);

		String liferayVersion = op.getLiferayVersion().content();

		StringBuilder sb = new StringBuilder();

		sb.append("--base ");
		sb.append("\"");
		sb.append(wsLocation.toFile().getAbsolutePath());
		sb.append("\" ");
		sb.append("init ");
		sb.append("-v ");
		sb.append(liferayVersion);

		try {
			BladeCLI.execute(sb.toString());
		}
		catch (BladeCLIException bclie) {
			return ProjectCore.createErrorStatus(bclie);
		}

		String workspaceLocation = location.append(wsName).toPortableString();
		boolean initBundle = op.getProvisionLiferayBundle().content();
		String bundleUrl = op.getBundleUrl().content(false);

		return importProject(workspaceLocation, monitor, initBundle, bundleUrl);
	}

	@Override
	public String getInitBundleUrl(String workspaceLocation) {
		return LiferayWorkspaceUtil.getGradleProperty(
			workspaceLocation, LiferayWorkspaceUtil.LIFERAY_WORKSPACE_BUNDLE_URL,
			BaseLiferayWorkspaceOp.LIFERAY_70_BUNDLE_URL);
	}

	@Override
	public IStatus importProject(String location, IProgressMonitor monitor, boolean initBundle, String bundleUrl) {
		try {
			final IStatus importJob = GradleUtil.importGradleProject(new File(location), monitor);

			if (!importJob.isOK() || (importJob.getException() != null)) {
				return importJob;
			}

			IPath path = new Path(location);

			IProject project = CoreUtil.getProject(path.lastSegment());

			if (initBundle) {
				if (bundleUrl != null) {
					final IFile gradlePropertiesFile = project.getFile("gradle.properties");

					try (InputStream gradleStream = gradlePropertiesFile.getContents()) {
						String content = FileUtil.readContents(gradleStream);

						String bundleUrlProp = LiferayWorkspaceUtil.LIFERAY_WORKSPACE_BUNDLE_URL + "=" + bundleUrl;

						String separator = System.getProperty("line.separator", "\n");

						String newContent = content + separator + bundleUrlProp;

						try (InputStream inputStream = new ByteArrayInputStream(newContent.getBytes())) {
							gradlePropertiesFile.setContents(inputStream, IResource.FORCE, monitor);
						}
					}
				}

				GradleUtil.runGradleTask(project, "initBundle", monitor);

				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}

			LiferayWorkspaceUtil.saveGradleProperty(location, "liferay.workspace.target.platform.version", "7.0.5");
			GradleUtil.refreshGradleProject(project);

			String targetVersion = LiferayWorkspaceUtil.getGradleProperty(location, "", "7.0.5");
			

			
			Job initlizeTargetFileJob = new Job("Intialize target file.") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						String targetBomFileName = MessageFormat.format(ProjectCore.bom_file_name, targetVersion);
						
						File bomFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetBomFileName);
						
						Set<String[]> bomLibs = GradleUtil.getTargetplatformBomDependencies(bomFile);

						ConcurrentMap<String, TargetPlatformDependency> targetDependencyFileMap = 
							GradleUtil.initializeDependencyFileMap(targetVersion);
						
						while (!checkDependencyDownloadStatus(bomLibs, targetDependencyFileMap)) {
							getGradleDependencyLibs(new File(location), targetDependencyFileMap);
							
							Thread.sleep(100);
						}
					}
					catch (Exception e) {
						ProjectCore.logError("Failed to download Lifeay workspace bom file", e);
					}
					return org.eclipse.core.runtime.Status.OK_STATUS;
				}
			};
			
			initlizeTargetFileJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					try {
						initlizeTargetFileJob.schedule();
					}
					catch( Exception e) {
						GradleCore.logError("Failed to wait gradle download all dependency files.", e);
					}
				}				
			});
			
			
			Job downloadBomJob = new Job("Download workspace bom file.") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						downloadBom(targetVersion);
					}
					catch (Exception e) {
						ProjectCore.logError("Failed to download Lifeay workspace bom file", e);
					}
					return org.eclipse.core.runtime.Status.OK_STATUS;
				}
			};

			downloadBomJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					try {
						initlizeTargetFileJob.schedule();
					}
					catch( Exception e) {
						GradleCore.logError("Failed to wait gradle download all dependency files.", e);
					}
				}				
			});

			downloadBomJob.schedule();
		}
		catch (Exception e) {
			return GradleCore.createErrorStatus("import Liferay workspace project error", e);
		}

		return Status.OK_STATUS;
	}
	
	@SuppressWarnings("restriction")
	private void saveTargetDependencyFile(String targetVersion,ConcurrentMap<String, TargetPlatformDependency> dependencyFileMap) {
		try {
			String targetFileName = MessageFormat.format(ProjectCore.target_file_name, targetVersion);
			File targetFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetFileName);	
			
			XMLMemento targetPlaftorm = XMLMemento.createWriteRoot("TargetPlatform");

			Map<String, IMemento> mementos = new HashMap<>();
			
			dependencyFileMap.forEach((libKey, targetPlatformDependency) -> {
				IMemento dependecyItem = targetPlaftorm.createChild("Dependency");

				IMemento groupIdItem = dependecyItem.createChild("Group");
				
				groupIdItem.putString("value", targetPlatformDependency.getGroup());
				
				IMemento nameItem = dependecyItem.createChild("Name");
				
				nameItem.putString("value", targetPlatformDependency.getName());
				
				IMemento versionItem = dependecyItem.createChild("Version");
				
				versionItem.putString("value", targetPlatformDependency.getVersion());
				
				IMemento providerItem = dependecyItem.createChild("ProviderCapability");
				
				providerItem.putString("value", targetPlatformDependency.getProviderCapability());
				
				IMemento fragHostItem = dependecyItem.createChild("FragmentHost");
				
				fragHostItem.putString("value", targetPlatformDependency.getFragmentHost());
				
				IMemento exportPackageItem = dependecyItem.createChild("ExportPackage");
				
				exportPackageItem.putString("value", targetPlatformDependency.getExportPackages());

				IMemento libLocationItem = dependecyItem.createChild("LibLocation");
				
				libLocationItem.putString("value", targetPlatformDependency.getLibFilePath().toString());
				
				
				mementos.put(libKey, exportPackageItem);
			});

			if (!mementos.isEmpty()) {
				try (OutputStream fos = Files.newOutputStream(Paths.get(targetFile.toURI()))) {
					targetPlaftorm.save(fos);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}			
		}
		catch(Exception e) {
			GradleCore.logError("Failed to save target dependency file. ", e);
		}
	}

	
	
	private void getGradleDependencyLibs(File projectFile, ConcurrentMap<String, TargetPlatformDependency> targetDependencyFileMap) {
		GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectFile);

		ProjectConnection connection = null;

		try {
			connection = connector.connect();

			ModelBuilder<EclipseProject> modelBuilder = connection.model( EclipseProject.class );
			
	        EclipseProject buildshipProject = modelBuilder.get();

	        DomainObjectSet<? extends EclipseExternalDependency> classpathes = buildshipProject.getClasspath();
	        for( EclipseExternalDependency dependency : classpathes )
	        {
	        	
	            java.nio.file.Path libPath = dependency.getFile().toPath();
	        	String group = dependency.getGradleModuleVersion().getGroup();
	        	String name = dependency.getGradleModuleVersion().getName();
	        	String version = dependency.getGradleModuleVersion().getVersion();
	        	
	        	String libKey = new String(group + "-" + name + "-" + version);

	        	targetDependencyFileMap.computeIfAbsent(libKey, new Function<String, TargetPlatformDependency>(){

					@Override
					public TargetPlatformDependency apply(String key) {
						TargetPlatformDependency targetDependency = new TargetPlatformDependency();
						String exportPackages = JavaUtil.getJarProperty(libPath.toFile(), "Export-Package");
						String fragmentHost = JavaUtil.getJarProperty(libPath.toFile(), "Fragment-Host");
						String providerCapability = JavaUtil.getJarProperty(libPath.toFile(), "Provide-Capability");

						targetDependency.setExportPackage(exportPackages);
						targetDependency.setFragmentHost(fragmentHost);
						targetDependency.setProviderCapability(providerCapability);
						targetDependency.setLibFilePath(libPath);
						targetDependency.setGroup(group);
						targetDependency.setName(name);
						targetDependency.setVersion(version);
						return targetDependency;
					} 
				});
	        }
		}
		catch(Exception e) {
			GradleCore.logError("Failed to get gradle dependency libs.", e);
		}
	}	
	
	private boolean checkDependencyDownloadStatus(Set<String[]> bomLibs, Map<String, TargetPlatformDependency> dependencyLibMaps) {
		int i = 0;
		for( String[] bomLib : bomLibs) {
			String groupId = bomLib[0];
			String artifactId = bomLib[1];
			String version = bomLib[2];
			
			String libKey = groupId + "-" + artifactId + "-" + version;
			TargetPlatformDependency targetPlatformDependency = dependencyLibMaps.get(libKey);
			System.out.print("!!!!!!!!!!!!!! number " + i++ + " Finding  " + libKey);
			if ( targetPlatformDependency != null ) {
				System.out.println(":------------found ");
				continue;
			}
			else {
				System.out.println(":------------ not found ");
				return false;	
			}
			
		}
		return true;
	}
	@Override
	public synchronized ILiferayProject provide(Object adaptable) {
		if (adaptable instanceof IProject) {
			final IProject project = (IProject)adaptable;

			if (LiferayWorkspaceUtil.isValidWorkspace(project)) {
				return new LiferayWorkspaceProject(project);
			}
		}

		return Optional.ofNullable(
			adaptable
		).filter(
			i -> i instanceof IServer
		).map(
			IServer.class::cast
		).map(
			ServerUtil::getLiferayRuntime
		).map(
			liferayRuntime -> liferayRuntime.getLiferayHome()
		).map(
			LiferayGradleWorkspaceProjectProvider::_getWorkspaceProjectFromLiferayHome
		).orElse(
			null
		);
	}

	@Override
	public IStatus validateProjectLocation(String projectName, IPath path) {
		IStatus retval = Status.OK_STATUS;

		// TODO validation gradle project location

		return retval;
	}

	private static IWorkspaceProject _getWorkspaceProjectFromLiferayHome(final IPath liferayHome) {
		return Optional.ofNullable(
			LiferayWorkspaceUtil.getWorkspaceProject()
		).filter(
			workspaceProject -> {
				IPath workspaceProjectLocation = workspaceProject.getRawLocation();

				if (workspaceProjectLocation == null) {
					return false;
				}

				return workspaceProjectLocation.isPrefixOf(liferayHome);
			}
		).map(
			workspaceProject -> LiferayCore.create(IWorkspaceProject.class, workspaceProject)
		).orElse(
			null
		);
	}

	private static void downloadBom(String targetplatformVersion) throws Exception {
		MessageFormat msgFormat= new MessageFormat(ProjectCore.bom_file_name);

		String targetBomFileName = msgFormat.format(targetplatformVersion);
		
		File bomFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetBomFileName);
		
		if ( FileUtil.notExists(bomFile)) {
			MessageFormat bomUrlFormat= new MessageFormat(ProjectCore.bomDownloadUrl);

			String bomDownloadUrl = bomUrlFormat.format(targetplatformVersion);

			URL url = new URL(bomDownloadUrl);

			FileUtils.copyURLToFile(url, bomFile);
		}
	}
}