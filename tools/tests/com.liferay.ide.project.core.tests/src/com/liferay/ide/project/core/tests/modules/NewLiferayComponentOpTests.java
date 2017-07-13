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
package com.liferay.ide.project.core.tests.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.BladeCLI;
import com.liferay.ide.project.core.modules.NewLiferayComponentOp;
import com.liferay.ide.project.core.modules.NewLiferayComponentOpMethods;
import com.liferay.ide.project.core.modules.NewLiferayModuleProjectOp;
import com.liferay.ide.project.core.modules.NewLiferayModuleProjectOpMethods;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.sapphire.modeling.Status;
import org.eclipse.sapphire.platform.ProgressMonitorBridge;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Gregory Amerson
 * @author Simon Jiang
 */
public class NewLiferayComponentOpTests
{

    @Test
    public void testNewLiferayComponentDefaultValueServiceDashes() throws Exception
    {
        NewLiferayComponentOp op = NewLiferayComponentOp.TYPE.instantiate();

        op.setProjectName( "my-test-project" );

        op.setComponentClassTemplate( "PortletActionCommand" );

        assertEquals( "MyTestProjectPortletActionCommand", op.getComponentClassName().content( true ) );
    }

    @Test
    public void testNewLiferayComponentDefaultValueServiceUnderscores() throws Exception
    {
        NewLiferayComponentOp op = NewLiferayComponentOp.TYPE.instantiate();

        op.setProjectName( "my_test_project" );

        op.setComponentClassTemplate( "PortletActionCommand" );

        assertEquals( "MyTestProjectPortletActionCommand", op.getComponentClassName().content( true ) );
    }

    @Test
    public void testNewLiferayComponentDefaultValueServiceDots() throws Exception
    {
        NewLiferayComponentOp op = NewLiferayComponentOp.TYPE.instantiate();

        op.setProjectName( "my.test.project" );

        op.setComponentClassTemplate( "PortletActionCommand" );

        assertEquals( "MyTestProjectPortletActionCommand", op.getComponentClassName().content( true ) );
    }

    @Test
    public void testNewLiferayComponentDefaultValueServiceIsListeningToProjectName() throws Exception
    {
        NewLiferayComponentOp op = NewLiferayComponentOp.TYPE.instantiate();

        op.setProjectName( "my.test.project" );

        op.setComponentClassTemplate( "PortletActionCommand" );

        assertEquals( "MyTestProjectPortletActionCommand", op.getComponentClassName().content( true ) );

        op.setProjectName( "my_abc-test" );

        assertEquals( "MyAbcTestPortletActionCommand", op.getComponentClassName().content( true ) );
    }

    @Test
    public void testNewLiferayComponentDefaultValueServiceIsListeningToComponentClassTemplate() throws Exception
    {
        NewLiferayComponentOp op = NewLiferayComponentOp.TYPE.instantiate();

        op.setProjectName( "my.test.project" );

        op.setComponentClassTemplate( "PortletActionCommand" );

        assertEquals( "MyTestProjectPortletActionCommand", op.getComponentClassName().content( true ) );

        op.setComponentClassTemplate( "FriendlyUrlMapper" );

        assertEquals( "MyTestProjectFriendlyUrlMapper", op.getComponentClassName().content( true ) );
    }


    @BeforeClass
    public static void setupBladeCLIRepoUrl() throws Exception
    {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode( ProjectCore.PLUGIN_ID );

        prefs.put( BladeCLI.BLADE_CLI_REPO_URL, "https://liferay-test-01.ci.cloudbees.com/job/liferay-blade-cli/lastSuccessfulBuild/artifact/build/generated/p2/" );

        prefs.flush();
    }

    @Test
    public void testNewLiferayComponentBndAndGradleForPortleActionCommandAndRest() throws Exception
    {
        NewLiferayModuleProjectOp pop = NewLiferayModuleProjectOp.TYPE.instantiate();

        pop.setProjectName( "testGradleModuleComponentBnd" );
        pop.setProjectTemplateName( "portlet" );
        pop.setProjectProvider( "gradle-module" );

        Status modulePorjectStatus = NewLiferayModuleProjectOpMethods.execute( pop, ProgressMonitorBridge.create( new NullProgressMonitor() ) );
        assertTrue( modulePorjectStatus.ok() );

        IProject modPorject = CoreUtil.getProject( pop.getProjectName().content() );
        modPorject.open( new NullProgressMonitor() );

        NewLiferayComponentOp cop = NewLiferayComponentOp.TYPE.instantiate();
        cop.setProjectName( pop.getProjectName().content() );
        cop.setComponentClassTemplate( "PortletActionCommand" );

        NewLiferayComponentOpMethods.execute( cop, ProgressMonitorBridge.create( new NullProgressMonitor() ) );

        IFile bgd = modPorject.getFile( "bnd.bnd" );
        String bndcontent = FileUtil.readContents( bgd.getLocation().toFile(), true );

        String bndConfig = "-includeresource: \\" + System.getProperty( "line.separator" ) +
                        "\t" + "@com.liferay.util.bridges-2.0.0.jar!/com/liferay/util/bridges/freemarker/FreeMarkerPortlet.class,\\" + System.getProperty( "line.separator" ) +
                        "\t" + "@com.liferay.util.taglib-2.0.0.jar!/META-INF/*.tld" + System.getProperty( "line.separator" );

        assertTrue( bndcontent.contains( bndConfig ) );

        IFile buildgrade = modPorject.getFile( "build.gradle" );
        String buildgradeContent = FileUtil.readContents( buildgrade.getLocation().toFile(),true );
        assertTrue( buildgradeContent.contains( "compile group: \"com.liferay.portal\", name:\"com.liferay.util.bridges\", version:\"2.0.0\"" ) );
        assertTrue( buildgradeContent.contains( "compile group: \"org.osgi\", name:\"org.osgi.service.component.annotations\", version:\"1.3.0\"" ) );

        NewLiferayComponentOp copRest = NewLiferayComponentOp.TYPE.instantiate();
        copRest.setProjectName( pop.getProjectName().content() );
        copRest.setComponentClassTemplate( "RestService" );

        NewLiferayComponentOpMethods.execute( copRest, ProgressMonitorBridge.create( new NullProgressMonitor() ) );

        bgd = modPorject.getFile( "bnd.bnd" );
        bndcontent = FileUtil.readContents( bgd.getLocation().toFile(), true );
        assertTrue( bndcontent.contains( bndConfig ) );

        final String restConfig = "Require-Capability: osgi.contract; filter:=\"(&(osgi.contract=JavaJAXRS)(version=2))\"";
        assertTrue( bndcontent.contains( restConfig ) );

        buildgrade = modPorject.getFile( "build.gradle" );
        buildgradeContent = FileUtil.readContents( buildgrade.getLocation().toFile(),true );
        assertTrue( buildgradeContent.contains( "compile group: \"javax.ws.rs\", name:\"javax.ws.rs-api\", version:\"2.0.1\"" ) );

        NewLiferayComponentOp copAuth = NewLiferayComponentOp.TYPE.instantiate();
        copAuth.setProjectName( pop.getProjectName().content() );
        copAuth.setComponentClassTemplate( "Authenticator" );

        NewLiferayComponentOpMethods.execute( copAuth, ProgressMonitorBridge.create( new NullProgressMonitor() ) );

        bgd = modPorject.getFile( "bnd.bnd" );
        bndcontent = FileUtil.readContents( bgd.getLocation().toFile(), true );

        bndConfig = "-includeresource: \\" + System.getProperty( "line.separator" ) +
                        "\t" + "@com.liferay.util.bridges-2.0.0.jar!/com/liferay/util/bridges/freemarker/FreeMarkerPortlet.class,\\" + System.getProperty( "line.separator" ) +
                        "\t" + "@com.liferay.util.taglib-2.0.0.jar!/META-INF/*.tld,\\" + System.getProperty( "line.separator" ) +
                        "\t" + "@shiro-core-1.1.0.jar";

        assertTrue( bndcontent.contains( bndConfig ) );

        buildgrade = modPorject.getFile( "build.gradle" );
        buildgradeContent = FileUtil.readContents( buildgrade.getLocation().toFile() ,true);
        assertTrue( buildgradeContent.contains( "compile group: \"org.apache.shiro\", name:\"shiro-core\", version:\"1.1.0\"" ) );
    }
}
