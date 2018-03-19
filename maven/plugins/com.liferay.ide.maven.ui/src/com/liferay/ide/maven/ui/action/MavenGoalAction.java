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

package com.liferay.ide.maven.ui.action;


import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.maven.core.ILiferayMavenConstants;
import com.liferay.ide.maven.core.MavenUtil;
import com.liferay.ide.maven.core.aether.AetherUtil;
import com.liferay.ide.maven.ui.LiferayMavenUI;
import com.liferay.ide.maven.ui.MavenUIProjectBuilder;
import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.ui.action.AbstractObjectAction;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.shared.utils.ReaderFactory;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * @author Gregory Amerson
 * @author Terry Jia
 * @author Charles Wu
 */
@SuppressWarnings("restriction")
public abstract class MavenGoalAction extends AbstractObjectAction {

	private IProject _project;
	public MavenGoalAction() {
	}

	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection)fSelection).toArray();

			IFile pomXml = null;
			IProject project = null;

			Object elem = elems[0];

			if (elem instanceof IFile) {
				pomXml = (IFile)elem;

				_project = pomXml.getProject();
			}
			else if (elem instanceof IProject) {
				_project = (IProject)elem;

				pomXml = _project.getFile(IMavenConstants.POM_FILE_NAME);
			}

			if (FileUtil.exists(pomXml)) {
				IFile pomXmlFile = pomXml;

				try {
					String pluginKey =
						ILiferayMavenConstants.LIFERAY_MAVEN_PLUGINS_GROUP_ID + ":" +
							ILiferayMavenConstants.LIFERAY_MAVEN_PLUGIN_ARTIFACT_ID;

					plugin = MavenUtil.getPlugin(MavenUtil.getProjectFacade(_project), pluginKey, new NullProgressMonitor());

					if (plugin == null) {
						plugin = MavenUtil.getPlugin(
							MavenUtil.getProjectFacade(_project), getGroupId() + ":" + getPluginKey(),
							new NullProgressMonitor());
					}
				}
				catch (CoreException ce) {
				}

				Job job = new Job(_project.getName() + " - " + getMavenGoals()) {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							if (plugin == null) {
								return ProjectUI.createErrorStatus("Can't find any plugins for " + getMavenGoals());
							}
							anlayzePlugin(plugin);
							monitor.beginTask(getMavenGoals(), 100);

							_runMavenGoal(pomXmlFile, getMavenGoals(), monitor);

							monitor.worked(80);

							_project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

							monitor.worked(10);

							updateProject(_project, monitor);

							monitor.worked(10);
						}
						catch (Exception e) {
							return ProjectUI.createErrorStatus("Error running Maven goal " + getMavenGoals(), e);
						}

						return Status.OK_STATUS;
					}

				};

				job.addJobChangeListener(
					new JobChangeAdapter() {

						public void done(IJobChangeEvent event) {
							afterGoal();
						}

					});

				job.schedule();
			}
		}
	}

    private String getPluginDescriptorLocation()
    {
        return "META-INF/maven/plugin.xml";
    }
    
    private PluginDescriptor extractPluginDescriptor( Artifact pluginArtifact, Plugin plugin )
    		        throws PluginDescriptorParsingException, InvalidPluginDescriptorException
    {
        PluginDescriptor pluginDescriptor = null;

        File pluginFile = pluginArtifact.getFile();

        try
        {
            if ( pluginFile.isFile() )
            {
                JarFile pluginJar = new JarFile( pluginFile, false );
                try
                {
                    ZipEntry pluginDescriptorEntry = pluginJar.getEntry( getPluginDescriptorLocation() );

                    if ( pluginDescriptorEntry != null )
                    {
                        InputStream is = pluginJar.getInputStream( pluginDescriptorEntry );

                        pluginDescriptor = parsePluginDescriptor( is, plugin, pluginFile.getAbsolutePath() );
                    }
                }
                finally
                {
                    pluginJar.close();
                }
            }
            else
            {
                File pluginXml = new File( pluginFile, getPluginDescriptorLocation() );

                if ( pluginXml.isFile() )
                {
                    InputStream is = new BufferedInputStream( new FileInputStream( pluginXml ) );
                    try
                    {
                        pluginDescriptor = parsePluginDescriptor( is, plugin, pluginXml.getAbsolutePath() );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }
                }
            }

            if ( pluginDescriptor == null )
            {
                throw new IOException( "No plugin descriptor found at " + getPluginDescriptorLocation() );
            }
        }
        catch ( IOException e )
        {
            throw new PluginDescriptorParsingException( plugin, pluginFile.getAbsolutePath(), e );
        }
        
        pluginDescriptor.setPluginArtifact( pluginArtifact );

        return pluginDescriptor;
    }
	
	private void anlayzePlugin(Plugin liferayPlugin) {
		if ( liferayPlugin != null) {
			RepositorySystem repositorySystem = AetherUtil.newRepositorySystem();
			RepositorySystemSession session = AetherUtil.newRepositorySystemSession(repositorySystem);
		
			IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
			IMavenProjectFacade projectFacade = projectManager.create(_project.getFile("pom.xml"), false, new NullProgressMonitor());
			List<RemoteRepository> remotePluginRepositories = projectFacade.getMavenProject().getRemotePluginRepositories();
			
			
			DefaultArtifact pluginArtifact = toArtifact( liferayPlugin, session );
            try
            {
                ArtifactRequest artifactRequest = new ArtifactRequest( pluginArtifact, remotePluginRepositories, "plugin"  );
                org.eclipse.aether.artifact.Artifact artifact2 = repositorySystem.resolveArtifact( session, artifactRequest ).getArtifact();
                Artifact mavenArtifact = RepositoryUtils.toArtifact( artifact2 );
                PluginDescriptor extractPluginDescriptor = extractPluginDescriptor( mavenArtifact, liferayPlugin );
    			if ( extractPluginDescriptor != null ) {
    				System.out.println(extractPluginDescriptor.getGoalPrefix());
    			}
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
		}
	}

	
	   private PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

	    private PluginDescriptor parsePluginDescriptor( InputStream is, Plugin plugin, String descriptorLocation )
	        throws PluginDescriptorParsingException
	    {
	        try
	        {
	            Reader reader = ReaderFactory.newXmlReader( is );

	            PluginDescriptor pluginDescriptor = builder.build( reader, descriptorLocation );

	            return pluginDescriptor;
	        }
	        catch ( IOException e )
	        {
	            throw new PluginDescriptorParsingException( plugin, descriptorLocation, e );
	        }
	        catch ( PlexusConfigurationException e )
	        {
	            throw new PluginDescriptorParsingException( plugin, descriptorLocation, e );
	        }
	    }
		
	    private org.eclipse.aether.artifact.DefaultArtifact toArtifact( Plugin plugin, RepositorySystemSession session )
	    {
	    	
	        return new  org.eclipse.aether.artifact.DefaultArtifact( plugin.getGroupId(), plugin.getArtifactId(), null, "jar", plugin.getVersion(),
	                                    session.getArtifactTypeRegistry().get( "maven-plugin" ) );
	    }
		
	public Plugin plugin = null;

	protected void afterGoal() {
	}

	protected String getGroupId() {
		return ILiferayMavenConstants.NEW_LIFERAY_MAVEN_PLUGINS_GROUP_ID;
	}

	protected abstract String getMavenGoals();

	protected String getPluginKey() {
		return "";
	}

	protected void updateProject(IProject p, IProgressMonitor monitor) {
		try {
			p.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		catch (CoreException ce) {
			LiferayMavenUI.logError("Error refreshing project after " + getMavenGoals(), ce);
		}
	}

	private void _runMavenGoal(IFile pomFile, String goal, IProgressMonitor monitor) throws CoreException {
		IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

		IMavenProjectFacade projectFacade = projectManager.create(pomFile, false, new NullProgressMonitor());

		MavenUIProjectBuilder builder = new MavenUIProjectBuilder(pomFile.getProject());

		builder.runMavenGoal(projectFacade, goal, "run", monitor);
	}

}