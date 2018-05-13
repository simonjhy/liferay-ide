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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.re2j.Pattern;
import com.liferay.ide.core.BaseLiferayProject;
import com.liferay.ide.core.ILiferayPortal;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.TargetPlatformDependency;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.project.core.IProjectBuilder;
import com.liferay.ide.project.core.IWorkspaceProjectBuilder;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.core.portal.PortalBundle;
import com.liferay.ide.server.util.JavaUtil;

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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
 */
@SuppressWarnings("restriction")
public class LiferayWorkspaceProject extends BaseLiferayProject implements IWorkspaceProject {

	private LoadingCache<String, Set<String[]>> _bomCache = CacheBuilder.newBuilder().build(
		new CacheLoader<String, Set<String[]>>() {
	
			@Override
			public Set<String[]> load(String targetplatformVersion) throws Exception {
				try{
					String targetBomFileName = MessageFormat.format(ProjectCore.bom_file_name, targetplatformVersion);
					
					File bomFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetBomFileName);
					
					if ( FileUtil.notExists(bomFile)) {
						String bomDownloadUrl = MessageFormat.format(ProjectCore.bomDownloadUrl, new Object[]{targetplatformVersion, targetplatformVersion} );

						URL url = new URL(bomDownloadUrl);
			
						FileUtils.copyURLToFile(url, bomFile);
					}
					
					Set<String[]> bomLibs = GradleUtil.getTargetplatformBomDependencies(bomFile);
					
					return bomLibs; 					
				}
				catch(Exception e) {
					e.printStackTrace();
				}

				return Sets.newConcurrentHashSet();
			}
		});	
	
	public LiferayWorkspaceProject(IProject project) {
		super(project);
	}
	
	@Override
	public <T> T adapt(Class<T> adapterType) {
		if (ILiferayPortal.class.equals(adapterType)) {

			// check for bundles/ directory

			IFolder bundlesFolder = getProject().getFolder("bundles");

			if (FileUtil.exists(bundlesFolder)) {
				PortalBundle portalBundle = LiferayServerCore.newPortalBundle(bundlesFolder.getLocation());

				if (portalBundle != null) {
					return adapterType.cast(portalBundle);
				}
			}
		}

		if (IProjectBuilder.class.equals(adapterType) || IWorkspaceProjectBuilder.class.equals(adapterType)) {
			IProjectBuilder projectBuilder = new GradleProjectBuilder(getProject());

			return adapterType.cast(projectBuilder);
		}

		return super.adapt(adapterType);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return null;
	}

	@Override
	public IFolder[] getSourceFolders() {
		return null;
	}

	@Override
	public TargetPlatformDependency getDependency(String importPackageName) {
		try {
			TargetPlatformDependency dependency = _dependenciesCache.get(importPackageName);

			if (dependency.isDefaultDependency() || dependency.isNoDependency() ) {
				_dependenciesCache.refresh(importPackageName);				
			}
			return dependency;
		}
		catch (ExecutionException e) {
		}

		return null;
	}
	
	

	private static ListeningExecutorService backgroundRefreshPools = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(20));

	private static ConcurrentMap<String, TargetPlatformDependency> _dependencyLibMap = new MapMaker()
			.concurrencyLevel(8)
			.makeMap();
	
	private static ConcurrentMap<String, TargetPlatformDependency> _dependencyFileMap = new MapMaker()
		.concurrencyLevel(8)
		.makeMap();

	public static List<TargetPlatformDependency> getGradleDependencyLibs(IPath projectLocation) {
		List<TargetPlatformDependency> workspaceGradleDependencyLib = new ArrayList<TargetPlatformDependency>();
		GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectLocation.toFile());

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
	        	 
	            workspaceGradleDependencyLib.add( new TargetPlatformDependency(libPath, group, name, version) );

	            _dependencyFileMap.computeIfAbsent(libKey, new Function<String, TargetPlatformDependency>(){

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
		}

		return workspaceGradleDependencyLib;
	}

	private Job internalDownloadDependencyJob;
	
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

	private static Pattern usesPattern = Pattern.compile("(uses:=\\\"((([a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+[0-9a-zA-Z_])+)+[,]*)+\\\")+");
	private LoadingCache<String, TargetPlatformDependency> _dependenciesCache = CacheBuilder.newBuilder()
		.concurrencyLevel(8)
		.refreshAfterWrite(1, TimeUnit.SECONDS)
		.build(
			new CacheLoader<String, TargetPlatformDependency>() {

				@Override
				public TargetPlatformDependency load(String importalPackageName) throws Exception {
					TargetPlatformDependency retVal = null;
					try {
						String worksacpeProjectLocation = getProject().getLocation().toOSString();
						String targetVersion = LiferayWorkspaceUtil.getGradleProperty(worksacpeProjectLocation,"liferay.workspace.target.platform.version", "7.0.5");
						String targetLibFileName = MessageFormat.format(ProjectCore.target_lib_name, targetVersion);
						File targetLibFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetLibFileName);						
						if ( FileUtil.exists(targetLibFile) && _dependencyLibMap.size() ==0 ) {
							try(InputStream newInputStream = Files.newInputStream(targetLibFile.toPath())) {
								IMemento existingTargetLibMemento = XMLMemento.loadMemento(newInputStream);

								if (existingTargetLibMemento != null) {
									IMemento[] dependencyLibChildren = existingTargetLibMemento.getChildren("Package");

									if (ListUtil.isNotEmpty(dependencyLibChildren)) {
										for (IMemento dependencyLib : dependencyLibChildren) {
										
											String name = dependencyLib.getString("name");
											String groupId = dependencyLib.getString("groupId");
											String artifactId = dependencyLib.getString("artifactId");
											String version = dependencyLib.getString("version");

											_dependencyLibMap.computeIfAbsent(name, new Function<String, TargetPlatformDependency>(){

												@Override
												public TargetPlatformDependency apply(String key) {
													TargetPlatformDependency targetDependency = new TargetPlatformDependency(groupId, artifactId, version);
													return targetDependency;
												} 
											});
										}
									}
								}
							}
						}						
						int removeLastSegIndex = importalPackageName.lastIndexOf(".");
						String packageName = importalPackageName.substring(0, removeLastSegIndex);						
						retVal = _dependencyLibMap.get(packageName);
						
						if ( retVal == null ) {
							retVal = new TargetPlatformDependency();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					return retVal;
				}

				@Override
				public Map<String, TargetPlatformDependency> loadAll(Iterable<? extends String> keys) throws Exception {
					// TODO Auto-generated method stub
					return super.loadAll(keys);
				}

				@Override
				public ListenableFuture<TargetPlatformDependency> reload(String importPackageName, TargetPlatformDependency odlDepndency)
					throws Exception {
					return backgroundRefreshPools.submit(new Callable<TargetPlatformDependency>() {
	
						@Override
						public TargetPlatformDependency call() throws Exception {
							String worksacpeProjectLocation = getProject().getLocation().toOSString();
							String targetVersion = LiferayWorkspaceUtil.getGradleProperty(worksacpeProjectLocation,"liferay.workspace.target.platform.version", "7.0.5");
							Set<String[]> bomLibs = _bomCache.get(targetVersion);
							
							String targetBomFileName = MessageFormat.format(ProjectCore.target_file_name, targetVersion);
							File targetDependencyFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetBomFileName);
							
							String targetLibFileName = MessageFormat.format(ProjectCore.target_lib_name, targetVersion);
							File targetLibFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetLibFileName);

							if ( FileUtil.exists(targetDependencyFile)) {
								try(InputStream newInputStream = Files.newInputStream(targetDependencyFile.toPath())) {
									IMemento existingTargetMemento = XMLMemento.loadMemento(newInputStream);

									if (existingTargetMemento != null) {
										IMemento[] dependencyChildren = existingTargetMemento.getChildren("Dependency");

										if (ListUtil.isNotEmpty(dependencyChildren)) {
											for (IMemento dependency : dependencyChildren) {
												IMemento groupMemento = dependency.getChild("Group");
												IMemento nameMemento = dependency.getChild("Name");
												IMemento versionMemento = dependency.getChild("Version");
												IMemento providerMemento = dependency.getChild("ProviderCapability");
												IMemento fragmentHostMemento = dependency.getChild("FragmentHost");
												IMemento exportPackageMemento = dependency.getChild("ExportPackage");
												IMemento libLocationMemento = dependency.getChild("LibLocation");
												
												String group = groupMemento.getString("value");
												String name = nameMemento.getString("value");
												String version = versionMemento.getString("value");
												String provider = providerMemento.getString("value");
												String fragment = fragmentHostMemento.getString("value");
												String export = exportPackageMemento.getString("value");
												String libPath = libLocationMemento.getString("value");

												String libKey= group +"-"+ name + "-"+ version;

									            _dependencyFileMap.computeIfAbsent(libKey, new Function<String, TargetPlatformDependency>(){

													@Override
													public TargetPlatformDependency apply(String key) {
														TargetPlatformDependency targetDependency = new TargetPlatformDependency();

														targetDependency.setExportPackage(export);
														targetDependency.setFragmentHost(fragment);
														targetDependency.setProviderCapability(provider);
														targetDependency.setLibFilePath(Paths.get(libPath));
														targetDependency.setGroup(group);
														targetDependency.setName(name);
														targetDependency.setVersion(version);
														return targetDependency;
													} 
												});
											}
										}
									}
								}								
							}

							if ( !checkDependencyDownloadStatus(bomLibs, _dependencyFileMap)) {
								internalDownloadDependencyJob = new Job("Continue to wait dependency lib initlialize work to finish......") {
									@Override
									protected IStatus run(IProgressMonitor monitor) {
										try{
											getGradleDependencyLibs(getProject().getLocation());
										}
										catch (Exception e) {
											ProjectCore.logError("Failed to download Lifeay workspace bom file", e);
										}
										return org.eclipse.core.runtime.Status.OK_STATUS;
									}
								};

								internalDownloadDependencyJob.addJobChangeListener(new JobChangeAdapter() {
									
									@Override
									public void done(IJobChangeEvent event) {

										XMLMemento targetPlaftorm = XMLMemento.createWriteRoot("TargetPlatform");

										Map<String, IMemento> mementos = new HashMap<>();
										
										_dependencyFileMap.forEach((libKey, targetPlatformDependency) -> {
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
											try (OutputStream fos = Files.newOutputStream(Paths.get(targetDependencyFile.toURI()))) {
												targetPlaftorm.save(fos);
											}
											catch (IOException e) {
												e.printStackTrace();
											}
										}
									}

								});
								
								internalDownloadDependencyJob.schedule();
							}

							Job parseDependencyLibJob = new Job("Parse to dependency lib initlialize work to finish......") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									try{
										for(String[] bomLib: bomLibs) {
											String groupID = bomLib[0];
											String name = bomLib[1];
											String version = bomLib[2];

											TargetPlatformDependency dependencyItem = _dependencyFileMap.get(groupID+"-"+name+"-"+version);

											if ( dependencyItem == null ) {
												continue;
											}
											String exportPackages = dependencyItem.getExportPackages();
											if (CoreUtil.isNotNullOrEmpty(exportPackages)) {
												String replaceValues = usesPattern.matcher(exportPackages).replaceAll("");
												String[] packagesArray = replaceValues.split(",");
												for (String exportPackage : packagesArray) {
													String[] exportPackageGroup = exportPackage.split(";");
					
													if (ListUtil.isNotEmpty(exportPackageGroup)) {
														String exportPackageName = exportPackageGroup[0];
														
														_dependencyLibMap.computeIfAbsent(exportPackageName, new Function<String, TargetPlatformDependency>(){
															
															@Override
															public TargetPlatformDependency apply(String key) {
																return dependencyItem;
															} 
														});
													}
												}
											}
										}
									}
									catch (Exception e) {
										ProjectCore.logError("Failed to download Lifeay workspace bom file", e);
									}
									return org.eclipse.core.runtime.Status.OK_STATUS;
								}
							};
							
							parseDependencyLibJob.addJobChangeListener(new JobChangeAdapter() {
								@Override
								public void done(IJobChangeEvent event) {

									XMLMemento exportPackagesItem = XMLMemento.createWriteRoot("ExportPackages");

									Map<String, IMemento> mementos = new HashMap<>();
									
									_dependencyLibMap.forEach((exportPackageName, targetPlatformDependency) -> {
								
										IMemento exportPackageItem = exportPackagesItem.createChild("Package");
										
										exportPackageItem.putString("name", exportPackageName);
										exportPackageItem.putString("groupId", targetPlatformDependency.getGroup());
										exportPackageItem.putString("artifactId", targetPlatformDependency.getName());
										exportPackageItem.putString("version", targetPlatformDependency.getVersion());
										
										mementos.put(exportPackageName, exportPackageItem);
									});

									if (!mementos.isEmpty()) {
										try (OutputStream fos = Files.newOutputStream(Paths.get(targetLibFile.toURI()))) {
											exportPackagesItem.save(fos);
										}
										catch (IOException e) {
											e.printStackTrace();
										}
									}
								}
							});

							if ( FileUtil.exists(targetLibFile)) {
								try(InputStream newInputStream = Files.newInputStream(targetLibFile.toPath())) {
									IMemento existingTargetLibMemento = XMLMemento.loadMemento(newInputStream);

									if (existingTargetLibMemento != null) {
										IMemento[] dependencyLibChildren = existingTargetLibMemento.getChildren("Package");

										if (ListUtil.isNotEmpty(dependencyLibChildren)) {
											for (IMemento dependencyLib : dependencyLibChildren) {
											
												String name = dependencyLib.getString("name");
												String groupId = dependencyLib.getString("groupId");
												String artifactId = dependencyLib.getString("artifactId");
												String version = dependencyLib.getString("version");

												_dependencyLibMap.computeIfAbsent(name, new Function<String, TargetPlatformDependency>(){

													@Override
													public TargetPlatformDependency apply(String key) {
														TargetPlatformDependency targetDependency = new TargetPlatformDependency(groupId, artifactId, version);
														return targetDependency;
													} 
												});
											}
										}
									}
								}
								int removeLastSegIndex = importPackageName.lastIndexOf(".");
								String packageName = importPackageName.substring(0, removeLastSegIndex);
								return _dependencyLibMap.get(packageName);								
							}
							else {
								parseDependencyLibJob.schedule();
							}

							return new TargetPlatformDependency();
						}
					});
				}
		});
}