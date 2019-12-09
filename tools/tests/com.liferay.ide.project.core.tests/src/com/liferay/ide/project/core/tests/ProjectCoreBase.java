/*******************************************************************************
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
 *
 *******************************************************************************/
package com.liferay.ide.project.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.validation.internal.operations.ValidatorManager;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.JobUtil;
import com.liferay.ide.core.util.ZipUtil;
import com.liferay.ide.project.core.ProjectRecord;
import com.liferay.ide.project.core.util.ProjectUtil;
import com.liferay.ide.server.core.tests.ServerCoreBase;

/**
 * @author Gregory Amerson
 * @author Terry Jia
 * @author Simon Jiang
 * @author Li Lu
 */
@SuppressWarnings( "restriction" )
public class ProjectCoreBase extends ServerCoreBase
{

    private static final String BUNDLE_ID = "com.liferay.ide.project.core.tests";

    public static void deleteAllWorkspaceProjects() throws Exception
    {
        for( IProject project : CoreUtil.getAllProjects() )
        {
            if ( project != null && project.isAccessible() && project.exists())
            {
                final NullProgressMonitor monitor = new NullProgressMonitor();

                try
                {
                    project.refreshLocal( IResource.DEPTH_INFINITE, monitor );
                }
                catch( Exception e)
                {
                    //ignore
                }

                project.close( monitor );
                project.delete( true, monitor );

                assertFalse( project.exists() );
            }
        }
    }

    protected IProject importProject(String bundleId, String projectName ) throws Exception
    {
    	IPath projectFolder = CoreUtil.getWorkspaceRoot().getLocation();

        final File projectZipFile = getProjectZip( bundleId, projectName );

        ZipUtil.unzip( projectZipFile, projectFolder.toFile() );

        final IPath projectPath = projectFolder.append( projectName );
        assertEquals( true, projectPath.toFile().exists() );

        final ProjectRecord projectRecord = ProjectUtil.getProjectRecordForDir( projectPath.toOSString() );
        assertNotNull( projectRecord );

        final IRuntime runtime = ServerCore.findRuntime( getRuntimeVersion() );
        assertNotNull( runtime );

        final IProject project = CoreUtil.openProject(projectRecord.getProjectName(), projectRecord.getProjectLocation(), new NullProgressMonitor() );

        assertNotNull( project );

        assertEquals( "Expected new project to exist.", true, project.exists() );

        return project;
    }
    
    protected static void waitForBuildAndValidation() throws Exception
    {
        IWorkspaceRoot root = null;

        try
        {
            ResourcesPlugin.getWorkspace().checkpoint(true);
            Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
            Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, new NullProgressMonitor());
            Job.getJobManager().join(ValidatorManager.VALIDATOR_JOB_FAMILY, new NullProgressMonitor());
            Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
            Job.getJobManager().join("org.eclipse.buildship.core.jobs", new NullProgressMonitor());
            JobUtil.waitForLiferayProjectJob();
            Thread.sleep(200);
            Job.getJobManager().beginRule(root = ResourcesPlugin.getWorkspace().getRoot(), null);
        }
        catch (InterruptedException e)
        {
            failTest( e );
        }
        catch (IllegalArgumentException e)
        {
            failTest( e );
        }
        catch (OperationCanceledException e)
        {
            failTest( e );
        }
        finally
        {
            if (root != null) {
                Job.getJobManager().endRule(root);
            }
        }
    }

    protected void waitForBuildAndValidation(IProject project) throws Exception
    {
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
        waitForBuildAndValidation();
    }

    protected IRuntime createNewRuntime( final String name ) throws Exception
    {
        final IPath newRuntimeLocation = new Path( getLiferayRuntimeDir().toString() + "-new" );

        if( ! newRuntimeLocation.toFile().exists() )
        {
            FileUtils.copyDirectory( getLiferayRuntimeDir().toFile(), newRuntimeLocation.toFile() );
        }

        assertEquals( true, newRuntimeLocation.toFile().exists() );

        final NullProgressMonitor npm = new NullProgressMonitor();

        IRuntime runtime = ServerCore.findRuntime( name );

        if( runtime == null )
        {
            final IRuntimeWorkingCopy runtimeWC =
                ServerCore.findRuntimeType( getRuntimeId() ).createRuntime( name, npm );

            runtimeWC.setName( name );
            runtimeWC.setLocation( newRuntimeLocation );

            runtime = runtimeWC.save( true, npm );
        }

        ServerCore.getRuntimes();
        assertNotNull( runtime );

        return runtime;
    }

    protected String getBundleId()
    {
        return BUNDLE_ID;
    }

    protected IPath getCustomLocationBase()
    {
        final IPath customLocationBase =
            org.eclipse.core.internal.utils.FileUtil.canonicalPath( new Path( System.getProperty( "java.io.tmpdir" ) ) ).append(
                "custom-project-location-tests" );

        return customLocationBase;
    }

    protected IPath getLiferayPluginsSDKZip()
    {
        return getLiferayBundlesPath().append( "liferay-ce-portal-tomcat-7.1.0-ga1-20180703012531655.zip" );
    }

    protected String getLiferayPluginsSdkZipFolder()
    {
        return "liferay-ce-portal-7.1.0-ga1/";
    }

    protected IProject getProject(String projectName ) throws Exception
    {
        IProject project = CoreUtil.getWorkspaceRoot().getProject( projectName );

        if( project != null && project.exists() )
        {
            return project;
        }

        return importProject( getBundleId(), projectName );
    }    
    
    protected File getProjectZip( String bundleId, String projectName ) throws IOException
    {
        final URL projectZipUrl =
            Platform.getBundle( bundleId ).getEntry( "projects/" + projectName + ".zip" );

        final File projectZipFile = new File( FileLocator.toFileURL( projectZipUrl ).getFile() );
        return projectZipFile;
    }

    private void ensureDefaultVMInstallExists() throws CoreException
    {
        IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();

        if( vmInstall != null )
        {
            return;
        }

        final IVMInstallType vmInstallType = JavaRuntime.getVMInstallType( StandardVMType.ID_STANDARD_VM_TYPE );
        String id = null;

        do
        {
            id = String.valueOf( System .currentTimeMillis() );
        }
        while( vmInstallType.findVMInstall( id ) != null );

        VMStandin newVm = new VMStandin( vmInstallType, id );
        newVm.setName( "Default-VM" );
        String jrePath = System.getProperty( "java.home" );
        File installLocation = new File( jrePath );
        newVm.setInstallLocation( installLocation );
        IVMInstall realVm = newVm.convertToRealVM();
        JavaRuntime.setDefaultVMInstall( realVm, new NullProgressMonitor() );
    }

    protected void removeAllRuntimes() throws Exception
    {
        for( IRuntime r : ServerCore.getRuntimes() )
        {
            r.delete();
        }
    }

    @Override
    public void setupRuntime() throws Exception
    {
        if( shouldSkipBundleTests() ) return;

        super.setupRuntime();
    }

    public void setupPluginsSDKAndRuntime() throws Exception
    {
        if( shouldSkipBundleTests() ) return;

        ensureDefaultVMInstallExists();
        setupRuntime();
    }

}