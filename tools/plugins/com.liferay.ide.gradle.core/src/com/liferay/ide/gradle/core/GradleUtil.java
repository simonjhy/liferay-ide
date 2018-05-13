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

import com.google.common.base.Optional;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.gradleware.tooling.toolingutils.binding.Validator;
import com.liferay.ide.core.TargetPlatformDependency;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.server.util.JavaUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.configuration.BuildConfiguration;
import org.eclipse.buildship.core.configuration.GradleProjectNature;
import org.eclipse.buildship.core.configuration.WorkspaceConfiguration;
import org.eclipse.buildship.core.launch.GradleRunConfigurationAttributes;
import org.eclipse.buildship.core.projectimport.ProjectImportConfiguration;
import org.eclipse.buildship.core.util.binding.Validators;
import org.eclipse.buildship.core.util.gradle.GradleDistributionSerializer;
import org.eclipse.buildship.core.util.gradle.GradleDistributionValidator;
import org.eclipse.buildship.core.util.gradle.GradleDistributionWrapper;
import org.eclipse.buildship.core.util.progress.AsyncHandler;
import org.eclipse.buildship.core.util.variable.ExpressionUtils;
import org.eclipse.buildship.core.workspace.GradleBuild;
import org.eclipse.buildship.core.workspace.NewProjectHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.internal.IMemento;
import org.eclipse.wst.server.core.internal.XMLMemento;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.eclipse.EclipseExternalDependency;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * @author Andy Wu
 * @author Lovett Li
 */
@SuppressWarnings("restriction")
public class GradleUtil {

	public static IStatus importGradleProject(File dir, IProgressMonitor monitor) throws CoreException {
		Validator<File> projectDirValidator = Validators.and(
			Validators.requiredDirectoryValidator("Project root directory"),
			Validators.nonWorkspaceFolderValidator("Project root directory"));

		Validator<GradleDistributionWrapper> gradleDistributionValidator =
			GradleDistributionValidator.gradleDistributionValidator();

		Validator<Boolean> applyWorkingSetsValidator = Validators.nullValidator();
		Validator<List<String>> workingSetsValidator = Validators.nullValidator();
		Validator<File> gradleUserHomeValidator = Validators.optionalDirectoryValidator("Gradle user home");

		ProjectImportConfiguration configuration = new ProjectImportConfiguration(
			projectDirValidator, gradleDistributionValidator, gradleUserHomeValidator, applyWorkingSetsValidator,
			workingSetsValidator);

		// read configuration from gradle preference

		WorkspaceConfiguration gradleConfig = CorePlugin.configurationManager().loadWorkspaceConfiguration();

		configuration.setProjectDir(dir);
		configuration.setOverwriteWorkspaceSettings(false);
		configuration.setGradleDistribution(GradleDistributionWrapper.from(gradleConfig.getGradleDistribution()));
		configuration.setGradleUserHome(gradleConfig.getGradleUserHome());
		configuration.setApplyWorkingSets(false);
		configuration.setBuildScansEnabled(gradleConfig.isBuildScansEnabled());
		configuration.setOfflineMode(gradleConfig.isOffline());
		configuration.setAutoSync(true);

		BuildConfiguration buildConfig = configuration.toBuildConfig();

		GradleBuild build = CorePlugin.gradleWorkspaceManager().getGradleBuild(buildConfig);

		build.synchronize(NewProjectHandler.IMPORT_AND_MERGE, AsyncHandler.NO_OP);

		waitImport();

		return Status.OK_STATUS;
	}

	public static boolean isBuildFile(IFile buildFile) {
		if (FileUtil.exists(buildFile) && "build.gradle".equals(buildFile.getName()) &&
			buildFile.getParent() instanceof IProject) {

			return true;
		}

		return false;
	}

	public static boolean isGradleProject(Object resource) throws CoreException {
		IProject project = null;

		if (resource instanceof IFile) {
			project = ((IFile)resource).getProject();
		}
		else if (resource instanceof IProject) {
			project = (IProject)resource;
		}

		return GradleProjectNature.isPresentOn(project);
	}

	public static void refreshGradleProject(IProject project) {
		Optional<GradleBuild> optional = CorePlugin.gradleWorkspaceManager().getGradleBuild(project);

		GradleBuild build = optional.get();

		build.synchronize(NewProjectHandler.IMPORT_AND_MERGE);
	}

	public static void runGradleTask(IProject project, String task, IProgressMonitor monitor) throws CoreException {
		runGradleTask(project, new String[] {task}, monitor);
	}

	public static void runGradleTask(IProject project, String[] tasks, IProgressMonitor monitor) throws CoreException {
		ILaunchConfiguration launchConfiguration =
			CorePlugin.gradleLaunchConfigurationManager().getOrCreateRunConfiguration(
				_getRunConfigurationAttributes(project, tasks));

		final ILaunchConfigurationWorkingCopy launchConfigurationWC = launchConfiguration.getWorkingCopy();

		launchConfigurationWC.setAttribute("org.eclipse.debug.ui.ATTR_CAPTURE_IN_CONSOLE", Boolean.TRUE);
		launchConfigurationWC.setAttribute("org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", Boolean.TRUE);
		launchConfigurationWC.setAttribute("org.eclipse.debug.ui.ATTR_PRIVATE", Boolean.TRUE);

		launchConfigurationWC.doSave();

		launchConfigurationWC.launch(ILaunchManager.RUN_MODE, monitor);
	}

	public static void waitImport() {
		IWorkspaceRoot root = null;

		try {
			ResourcesPlugin.getWorkspace().checkpoint(true);
			Job.getJobManager().join(CorePlugin.GRADLE_JOB_FAMILY, new NullProgressMonitor());
			Job.getJobManager().join(GradleCore.JOB_FAMILY_ID, new NullProgressMonitor());
			Thread.sleep(200);
			Job.getJobManager().beginRule(root = ResourcesPlugin.getWorkspace().getRoot(), null);
		}
		catch (InterruptedException ie) {
		}
		catch (IllegalArgumentException iae) {
		}
		catch (OperationCanceledException oce) {
		}
		finally {
			if (root != null) {
				Job.getJobManager().endRule(root);
			}
		}
	}

	private static GradleRunConfigurationAttributes _getRunConfigurationAttributes(IProject project, String[] tasks) {
		File rootDir = project.getLocation().toFile();

		BuildConfiguration buildConfig = CorePlugin.configurationManager().loadBuildConfiguration(rootDir);

		String projectDirectoryExpression = null;

		Optional<IProject> gradleProject = CorePlugin.workspaceOperations().findProjectByLocation(rootDir);

		if (gradleProject.isPresent()) {
			projectDirectoryExpression = ExpressionUtils.encodeWorkspaceLocation(gradleProject.get());
		}
		else {
			projectDirectoryExpression = rootDir.getAbsolutePath();
		}

		String gradleUserHome =
			buildConfig.getGradleUserHome() == null ? "" : buildConfig.getGradleUserHome().getAbsolutePath();

		List<String> taskList = new ArrayList<>();

		for (String task : tasks) {
			taskList.add(task);
		}

		String serializeString = GradleDistributionSerializer.INSTANCE.serializeToString(
			buildConfig.getGradleDistribution());

		return new GradleRunConfigurationAttributes(
			taskList, projectDirectoryExpression, serializeString, gradleUserHome, null,
			Collections.<String>emptyList(), Collections.<String>emptyList(), true, true,
			buildConfig.isOverrideWorkspaceSettings(), buildConfig.isOfflineMode(), buildConfig.isBuildScansEnabled());
	}
	
	@SuppressWarnings("unchecked")
	public static Set<String[]> getTargetplatformBomDependencies(File bomFile) {

		Set<String[]> artifactsSet = Sets.newConcurrentHashSet();
		SAXBuilder builder = new SAXBuilder(false);

		builder.setValidation(false);
		builder.setFeature("http://xml.org/sax/features/validation", false);
		builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		try (InputStream bomInput = Files.newInputStream(bomFile.toPath())) {
			Document doc = builder.build(bomInput);

			Element elementRoot = doc.getRootElement();
			List<Element> elements = elementRoot.getChildren();

			for (Iterator<Element> elementsIterator = elements.iterator(); elementsIterator.hasNext();) {
				Element childElement = elementsIterator.next();

				if (!childElement.getName().equals("dependencyManagement")) {
					continue;
				}

				List<Element> dependenciesElements = childElement.getChildren();

				for (Iterator<Element> dependenciesIterator = dependenciesElements.iterator(); dependenciesIterator
					.hasNext();) {
					Element dependenciesElement = dependenciesIterator.next();

					if (!dependenciesElement.getName().equals("dependencies")) {
						continue;
					}
					List<Element> dependencyElements = dependenciesElement.getChildren();
					Iterator<Element> dependencyIterator = dependencyElements.iterator();

					while (dependencyIterator.hasNext()) {
						Element dependencyElement = dependencyIterator.next();

						if (!dependencyElement.getName().equals("dependency")) {
							continue;
						}

						List<Element> contentsElement = dependencyElement.getContent();
						String artifactId = null;
						String groupId = null;
						String version = null;
						for (Object obj : contentsElement) {
							if (!(obj instanceof Element)) {
								continue;
							}
							Element element = (Element) obj;
							if (element.getName().equals("artifactId")) {
								artifactId = element.getText();
							}
							if (element.getName().equals("groupId")) {
								groupId = element.getText();
							}
							if (element.getName().equals("version")) {
								version = element.getText();
							}
						}

						if (groupId.equals("com.liferay.plugins")) {
							continue;
						}
						artifactsSet.add(new String[] {
							groupId, artifactId, version
						});
					}
				}
			}
		}
		catch (Exception e) {
			GradleCore.logError("Failed to parse target platform bom file.", e);
		}
		return artifactsSet;
	}
	
	public static ConcurrentMap<String, TargetPlatformDependency> initializeDependencyFileMap(String workspaceProjectLocation) {
		ConcurrentMap<String, TargetPlatformDependency> dependencyFileMap = new MapMaker()
			.concurrencyLevel(8)
			.makeMap();
		
		String targetVersion = LiferayWorkspaceUtil.getGradleProperty(workspaceProjectLocation, "", "7.0.5");
		String targetBomFileName = MessageFormat.format(ProjectCore.target_file_name, targetVersion);
		File targetFile = new File(ProjectCore.bomCacheDir.getAbsolutePath(), targetBomFileName);	

		if ( FileUtil.exists(targetFile)) {
			try(InputStream newInputStream = Files.newInputStream(targetFile.toPath())) {
				IMemento existingTargetMemento = XMLMemento.loadMemento(newInputStream);

				if (existingTargetMemento != null) {
					IMemento[] dependencyChildren = existingTargetMemento.getChildren("Dependency");

					if (ListUtil.isNotEmpty(dependencyChildren)) {
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

								dependencyFileMap.computeIfAbsent(libKey, new Function<String, TargetPlatformDependency>(){

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
			catch (IOException e) {
				GradleCore.logError("Failed to initialize target platform dependency file.", e);
			}
		}
		return dependencyFileMap;
	}	
}