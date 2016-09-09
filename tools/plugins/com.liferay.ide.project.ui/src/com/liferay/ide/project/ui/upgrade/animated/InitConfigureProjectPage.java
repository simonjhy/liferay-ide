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

package com.liferay.ide.project.ui.upgrade.animated;

import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.ILiferayProjectImporter;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.IOUtil;
import com.liferay.ide.core.util.ZipUtil;
import com.liferay.ide.project.core.IProjectBuilder;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.BladeCLI;
import com.liferay.ide.project.core.modules.BladeCLIException;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.util.ProjectImportUtil;
import com.liferay.ide.project.core.util.ProjectUtil;
import com.liferay.ide.project.core.util.SearchFilesVisitor;
import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.project.ui.upgrade.animated.UpgradeView.PageNavigatorListener;
import com.liferay.ide.project.ui.upgrade.animated.UpgradeView.PageValidationListener;
import com.liferay.ide.sdk.core.ISDKConstants;
import com.liferay.ide.sdk.core.SDK;
import com.liferay.ide.sdk.core.SDKUtil;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.core.portal.PortalRuntime;
import com.liferay.ide.server.core.portal.PortalServer;
import com.liferay.ide.server.util.ServerUtil;
import com.liferay.ide.ui.util.SWTUtil;
import com.liferay.ide.ui.util.UIUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.sapphire.Event;
import org.eclipse.sapphire.Listener;
import org.eclipse.sapphire.Property;
import org.eclipse.sapphire.ValuePropertyContentEvent;
import org.eclipse.sapphire.modeling.Status;
import org.eclipse.sapphire.platform.PathBridge;
import org.eclipse.sapphire.platform.StatusBridge;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.ServerUIUtil;
import org.osgi.framework.Version;

/**
 * @author Simon Jiang
 * @author Terry Jia
 */
@SuppressWarnings( "unused" )
public class InitConfigureProjectPage extends Page implements IServerLifecycleListener
{

    private class LiferayUpgradeValidationListener extends Listener
    {

        @Override
        public void handle( Event event )
        {
            if( event instanceof ValuePropertyContentEvent )
            {
                ValuePropertyContentEvent propertyEvetn = (ValuePropertyContentEvent) event;
                final Property property = propertyEvetn.property();

                if( property.name().equals( "SdkLocation" ) )
                {
                    org.eclipse.sapphire.modeling.Path sdkPath = dataModel.getSdkLocation().content();

                    if( sdkPath != null )
                    {
                        Status sdkStatus = sdkValidation.compute();

                        if( sdkStatus.ok() )
                        {
                            SDK sdk = SDKUtil.createSDKFromLocation( PathBridge.create( sdkPath ) );
                            String version = sdk.getVersion();

                            if( version != null )
                            {
                                Version sdkVersion = new Version( version );
                                int result = sdkVersion.compareTo( new Version( "7.0.0" ) );

                                if( result >= 0 )
                                {
                                    UIUtil.async( new Runnable()
                                    {

                                        @Override
                                        public void run()
                                        {
                                            if ( layoutComb.getSelectionIndex() != 1 )
                                            {
                                                layoutComb.select( 1 );
                                                layoutComb.setEnabled( false );
                                                dataModel.setLayout( layoutComb.getText() );
                                                createBundleControl();                                                
                                            }
                                            else
                                            {
                                                layoutComb.setEnabled( false );
                                                dataModel.setLayout( layoutComb.getText() );
                                            }
                                        }
                                    } );
                                }
                                else
                                {
                                    layoutComb.setEnabled( true );
                                }
                            }
                            else
                            {
                                layoutComb.setEnabled( true );
                            }
                        }
                        else
                        {
                            layoutComb.setEnabled( true );
                        }
                    }
                    else
                    {
                        layoutComb.setEnabled( true );
                    }
                }
            }

            startCheckThread();
        }
    }

    public static final String defaultBundleUrl =
        "https://sourceforge.net/projects/lportal/files/Liferay%20Portal/7.0.1%20GA2/liferay-ce-portal-tomcat-7.0-ga2-20160610113014153.zip";
    private static Color GRAY;
    private Label dirLabel;
    private Text dirField;
    // private Label newProjectLabel;
    // private Text newProjectField;
    private Combo layoutComb;
    private Label layoutLabel;
    private String[] layoutNames = { "Upgrade to Liferay SDK 7", "Use Plugin SDK In Liferay Workspace" };
    private Label serverLabel;
    private Combo serverComb;
    private Button serverButton;
    protected Label blankLabel;
    protected Label blankLabel2;
    private Button importButton;
    private Button showAllPagesButton;
    private Label bundleNameLabel;
    private Label bundleUrlLabel;
    private Text bundleNameField;

    private Text bundleUrlField;

    private Composite composite;

    private Control createHorizontalSpacer;
    private Control createSeparator;
    private SdkLocationValidationService sdkValidation =
        dataModel.getSdkLocation().service( SdkLocationValidationService.class );
    private ProjectNameValidationService projectNameValidation =
        dataModel.getProjectName().service( ProjectNameValidationService.class );
    private BundleNameValidationService bundleNameValidation =
        dataModel.getBundleName().service( BundleNameValidationService.class );

    private BundleUrlValidationService bundleUrlValidation =
        dataModel.getBundleUrl().service( BundleUrlValidationService.class );

    private SdkLocationDefaultValueService sdkLocationDefaultService =
        dataModel.getSdkLocation().service( SdkLocationDefaultValueService.class );

    public InitConfigureProjectPage( final Composite parent, int style, LiferayUpgradeDataModel dataModel )
    {
        super( parent, style, dataModel, IMPORT_PAGE_ID, false );

        dataModel.getSdkLocation().attach( new LiferayUpgradeValidationListener() );
        dataModel.getBundleName().attach( new LiferayUpgradeValidationListener() );
        dataModel.getBundleUrl().attach( new LiferayUpgradeValidationListener() );

        composite = this;

        createSeparator = createSeparator( this, 3 );

        dirLabel = createLabel( composite, "Liferay SDK Location:" );
        dirField = createTextField( composite, SWT.NONE );
        dirField.addModifyListener( new ModifyListener()
        {

            public void modifyText( ModifyEvent e )
            {
                dataModel.setSdkLocation( dirField.getText() );
                SDK sdk = SDKUtil.createSDKFromLocation( new Path( dirField.getText() ) );

                try
                {
                    if( sdk != null )
                    {
                        final String liferay62ServerLocation = (String) ( sdk.getBuildProperties( true ).get(
                            ISDKConstants.PROPERTY_APP_SERVER_PARENT_DIR ) );
                        dataModel.setLiferay62ServerLocation( liferay62ServerLocation );
                    }
                }
                catch( Exception xe )
                {
                    ProjectUI.logError( xe );
                }
            }
        } );

        dirField.setText( getSDKDefaultValue() );
        dataModel.setSdkLocation( getSDKDefaultValue() );

        SWTUtil.createButton( this, "Browse..." ).addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                final DirectoryDialog dd = new DirectoryDialog( getShell() );
                dd.setMessage( "Select source SDK folder" );

                final String selectedDir = dd.open();

                if( selectedDir != null )
                {
                    dirField.setText( selectedDir );
                }
            }
        } );

        layoutLabel = createLabel( composite, "Select Migrate Layout:" );
        layoutComb = new Combo( this, SWT.DROP_DOWN | SWT.READ_ONLY );
        layoutComb.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        layoutComb.setItems( layoutNames );
        layoutComb.select( 0 );
        layoutComb.addSelectionListener( new SelectionListener()
        {

            @Override
            public void widgetDefaultSelected( SelectionEvent e )
            {
            }

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                int sel = layoutComb.getSelectionIndex();

                if( sel == 0 )
                {
                    disposeBundleElement();

                    disposeLayoutElement();

                    disposeImportElement();

                    createServerElement();

                    createImportElement();

                    createShowAllPagesElement();
                }
                else
                {
                    disposeServerEelment();

                    disposeImportElement();

                    disposeBundleElement();

                    disposeLayoutElement();

                    createBundleElement();

                    createImportElement();

                    createShowAllPagesElement();
                }

                composite.layout();
                dataModel.setLayout( layoutComb.getText() );

                startCheckThread();
            }

        } );
        dataModel.setLayout( layoutComb.getText() );

        createServerElement();

        createImportElement();

        createShowAllPagesElement();

        startCheckThread();
    }

    public void addPortalRuntimeAndServer( String serverRuntimeName, String location, IProgressMonitor monitor )
        throws CoreException
    {
        final IRuntimeWorkingCopy runtimeWC =
            ServerCore.findRuntimeType( PortalRuntime.ID ).createRuntime( serverRuntimeName, monitor );

        IPath runTimePath = new Path( location );

        runtimeWC.setName( serverRuntimeName );
        runtimeWC.setLocation( runTimePath.append( LiferayWorkspaceUtil.loadConfiguredHomeDir( location ) ) );

        runtimeWC.save( true, monitor );

        final IServerWorkingCopy serverWC =
            ServerCore.findServerType( PortalServer.ID ).createServer( serverRuntimeName, null, runtimeWC, monitor );

        serverWC.setName( serverRuntimeName );
        serverWC.save( true, monitor );

    }

    private void backupSDK( IProgressMonitor monitor )
    {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );
        try
        {
            progress.setTaskName( "Backup sdk folder into Eclipse workspace...");
            org.eclipse.sapphire.modeling.Path originalSDKPath = dataModel.getSdkLocation().content();

            if( originalSDKPath != null )
            {
                IPath backupLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
                progress.worked( 30 );
                ZipUtil.zip( originalSDKPath.toFile(), backupLocation.append( "backup.zip" ).toFile() );
                progress.setWorkRemaining( 70 );
            }
        }
        catch( IOException e )
        {
            ProjectUI.logError( "Error to backup original sdk folder.", e );
        }
        finally
        {
            progress.done();
        }
    }

    private void checkProjectType( IProject project )
    {
        if( ProjectUtil.isPortletProject( project ) )
        {
            dataModel.setHasPortlet( true );

            List<IFile> searchFiles = new SearchFilesVisitor().searchFiles( project, "service.xml" );

            if( searchFiles.size() > 0 )
            {
                dataModel.setHasServiceBuilder( true );
            }
        }
        else if( ProjectUtil.isHookProject( project ) )
        {
            dataModel.setHasHook( true );
        }
        else if( ProjectUtil.isLayoutTplProject( project ) )
        {
            dataModel.setHasLayout( true );
        }
        else if( ProjectUtil.isThemeProject( project ) )
        {
            dataModel.setHasTheme( true );
        }
        else if( ProjectUtil.isExtProject( project ) )
        {
            dataModel.setHasExt( true );
        }
        else if( ProjectUtil.isWebProject( project ) )
        {
            dataModel.setHasWeb( true );
        }
    }

    private void clearWorkspaceSDKAndProjects( IPath targetSDKLocation, IProgressMonitor monitor ) throws CoreException
    {
        IProject sdkProject = SDKUtil.getWorkspaceSDKProject();

        if( sdkProject != null && sdkProject.getLocation().equals( targetSDKLocation ) )
        {
            IProject[] projects = ProjectUtil.getAllPluginsSDKProjects();

            for( IProject project : projects )
            {
                project.delete( false, true, monitor );
            }

            sdkProject.delete( false, true, monitor );
        }

    }

    private void copyNewSDK( IPath targetSDKLocation, IProgressMonitor monitor ) throws CoreException
    {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );
        try
        {
            progress.beginTask( "Copy new SDK to override target SDK.", 100 );
            final URL sdkZipUrl =
                Platform.getBundle( "com.liferay.ide.project.ui" ).getEntry( "resources/sdk70ga2.zip" );

            final File sdkZipFile = new File( FileLocator.toFileURL( sdkZipUrl ).getFile() );

            final IPath stateLocation = ProjectCore.getDefault().getStateLocation();

            File stateDir = stateLocation.toFile();

            progress.worked( 30 );

            ZipUtil.unzip( sdkZipFile, stateDir );

            progress.worked( 60 );

            IOUtil.copyDirToDir(
                new File( stateDir, "com.liferay.portal.plugins.sdk-7.0" ), targetSDKLocation.toFile() );

            progress.worked( 100 );
        }
        catch( Exception e )
        {
            ProjectUI.logError( e );
            throw new CoreException( StatusBridge.create( Status.createErrorStatus( "Failed copy new SDK..", e ) ) );
        }
        finally
        {
            progress.done();
        }

    }

    private void createBundleControl()
    {
        disposeServerEelment();

        disposeImportElement();

        disposeBundleElement();

        disposeLayoutElement();

        createBundleElement();

        createImportElement();

        createShowAllPagesElement();

        composite.layout();
    }

    private void createBundleElement()
    {
        bundleNameLabel = createLabel( composite, "Bundle Name:" );
        bundleNameField = createTextField( composite, SWT.NONE );
        bundleNameField.addModifyListener( new ModifyListener()
        {

            public void modifyText( ModifyEvent e )
            {
                dataModel.setBundleName( bundleNameField.getText() );
            }
        } );
        dataModel.setBundleName( bundleNameField.getText() );

        bundleUrlLabel = createLabel( composite, "Bundle URL:" );
        bundleUrlField = createTextField( composite, SWT.NONE );
        bundleUrlField.setForeground( composite.getDisplay().getSystemColor( SWT.COLOR_DARK_GRAY ) );
        bundleUrlField.setText( defaultBundleUrl );
        bundleUrlField.addModifyListener( new ModifyListener()
        {

            public void modifyText( ModifyEvent e )
            {
                dataModel.setBundleUrl( bundleUrlField.getText() );
            }
        } );
        bundleUrlField.addFocusListener( new FocusListener()
        {

            @Override
            public void focusGained( FocusEvent e )
            {
                String input = ( (Text) e.getSource() ).getText();

                if( input.equals( defaultBundleUrl ) )
                {
                    bundleUrlField.setText( "" );
                }
                bundleUrlField.setForeground( composite.getDisplay().getSystemColor( SWT.COLOR_BLACK ) );
            }

            @Override
            public void focusLost( FocusEvent e )
            {
                String input = ( (Text) e.getSource() ).getText();

                if( CoreUtil.isNullOrEmpty( input ) )
                {
                    bundleUrlField.setForeground( composite.getDisplay().getSystemColor( SWT.COLOR_DARK_GRAY ) );
                    bundleUrlField.setText( defaultBundleUrl );
                }
            }
        } );
        dataModel.setBundleUrl( bundleUrlField.getText() );
    }

    private void createDefaultControl()
    {
        UIUtil.async( new Runnable()
        {

            @Override
            public void run()
            {
                layoutComb.select( 0 );
                layoutComb.setEnabled( true );
                dataModel.setLayout( layoutComb.getText() );

                disposeBundleElement();

                disposeLayoutElement();

                disposeImportElement();

                createServerElement();

                createImportElement();

                createShowAllPagesElement();

                composite.layout();
            }
        } );
    }

    private void createImportElement()
    {
        createHorizontalSpacer = createHorizontalSpacer( this, 3 );
        createSeparator = createSeparator( this, 3 );

        blankLabel = new Label( this, SWT.LEFT_TO_RIGHT );

        importButton = SWTUtil.createButton( this, "Import Project..." );
        importButton.addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                try
                {
                    importButton.setEnabled( false );

                    importProject();

                    resetPages();

                    PageNavigateEvent event = new PageNavigateEvent();

                    event.setTargetPage( 2 );

                    for( PageNavigatorListener listener : naviListeners )
                    {
                        listener.onPageNavigate( event );
                    }

                    setNextPage( true );
                    importButton.setEnabled( true );
                }
                catch( CoreException ex )
                {
                    ProjectUI.logError( ex );

                    triggerValidationEvent( ex.getMessage() );
                }
            }
        } );

        importButton.setEnabled( false );
    }

    private void createShowAllPagesElement()
    {
        blankLabel2 = new Label( this, SWT.LEFT_TO_RIGHT );

        showAllPagesButton = SWTUtil.createButton( this, "Show All Pages..." );
        showAllPagesButton.addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                Boolean openNewLiferayProjectWizard = MessageDialog.openQuestion(
                    UIUtil.getActiveShell(), "Show All Pages",
                    "If you fail to import projects, you can click this button to finish the upgrade prossess, "+
                    "as shown in the following steps:\n" +
                    "   1.upgrade SDK 6.2 to SDK 7.0 manually\n" +
                    "   or use blade cli to create a Liferay workspace for your SDK\n" +
                    "   2.import projects you want to upgrade into Eclipse workspace\n" +
                    "   3.choose \"yes\" to finish the following steps");

                if( openNewLiferayProjectWizard )
                {
                    UpgradeView.resumePages();

                    UpgradeView.resetPages();

                    PageNavigateEvent event = new PageNavigateEvent();

                    event.setTargetPage( 2 );

                    for( PageNavigatorListener listener : naviListeners )
                    {
                        listener.onPageNavigate( event );
                    }

                    setNextPage( true );
                }
            }
        } );
    }

    private void createInitBundle( IProgressMonitor monitor ) throws CoreException
    {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );
        try
        {
            progress.beginTask( "Execute Blade CLI Command...", 100 );
            String layout = dataModel.getLayout().content();

            if( layout.equals( layoutNames[1] ) )
            {
                IPath sdkLocation = PathBridge.create( dataModel.getSdkLocation().content() );

                IProject project = CoreUtil.getProject( sdkLocation.lastSegment() );

                final String bundleUrl = dataModel.getBundleUrl().content();

                final String bundleName = dataModel.getBundleName().content();

                IProjectBuilder projectBuilder = getProjectBuilder( project );

                progress.worked( 30 );

                if( bundleUrl != null )
                {
                    projectBuilder.creatInitBundle( project, "initBundle", bundleUrl, monitor );
                }

                progress.worked( 60 );

                addPortalRuntimeAndServer( bundleName, sdkLocation.toPortableString(), monitor );

                IServer bundleServer = ServerCore.findServer( dataModel.getBundleName().content() );

                if( bundleServer != null )
                {
                    org.eclipse.sapphire.modeling.Path newPath = dataModel.getSdkLocation().content();
                    SDK sdk = SDKUtil.createSDKFromLocation( PathBridge.create( newPath ).append( "plugins-sdk" ) );

                    IPath bundleLocation = bundleServer.getRuntime().getLocation();

                    sdk.addOrUpdateServerProperties( bundleLocation );
                }

                project.refreshLocal( IResource.DEPTH_INFINITE, monitor );

                progress.worked( 100 );
            }
        }
        catch( Exception e )
        {
            ProjectUI.logError( e );
            throw new CoreException(
                StatusBridge.create( Status.createErrorStatus( "Faild execute create init bundle command.", e ) ) );
        }
        finally
        {
            progress.done();
        }
    }

    private void createLiferayWorkspace( IPath targetSDKLocation, IProgressMonitor monitor ) throws CoreException
    {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );
        try
        {

            progress.beginTask( "Execute Blade CLI Command...", 100 );

            StringBuilder sb = new StringBuilder();

            sb.append( "-b " );
            sb.append( "\"" + targetSDKLocation.toFile().getAbsolutePath() + "\" " );
            sb.append( "init -u" );
            progress.worked( 30 );
            BladeCLI.execute( sb.toString() );
            progress.worked( 100 );
        }
        catch( BladeCLIException e )
        {
            ProjectUI.logError( e );
            throw new CoreException(
                StatusBridge.create( Status.createErrorStatus( "Faild execute Balde CLI Command", e ) ) );
        }
        finally
        {
            progress.done();
        }
    }

    private void createServerElement()
    {
        serverLabel = createLabel( composite, "Liferay Server Name:" );
        serverComb = new Combo( composite, SWT.DROP_DOWN | SWT.READ_ONLY );
        serverComb.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        serverComb.addSelectionListener( new SelectionListener()
        {

            @Override
            public void widgetDefaultSelected( SelectionEvent e )
            {
            }

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                if( e.getSource().equals( serverComb ) )
                {
                    dataModel.setLiferayServerName( serverComb.getText() );
                }

            }
        } );

        serverButton = SWTUtil.createButton( composite, "Add Server..." );
        serverButton.addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                ServerUIUtil.showNewServerWizard( composite.getShell(), "liferay.bundle", null, "com.liferay." );
            }
        } );

        ServerCore.addServerLifecycleListener( this );

        IServer[] servers = ServerCore.getServers();
        List<String> serverNames = new ArrayList<String>();

        if( !CoreUtil.isNullOrEmpty( servers ) )
        {
            for( IServer server : servers )
            {
                if( LiferayServerCore.newPortalBundle( server.getRuntime().getLocation() ) != null )
                {
                    serverNames.add( server.getName() );
                }
            }
        }

        serverComb.setItems( serverNames.toArray( new String[serverNames.size()] ) );
        serverComb.select( 0 );
        dataModel.setLiferayServerName( serverComb.getText() );
    }

    private void deleteEclipseConfigFiles( File project )
    {
        for( File file : project.listFiles() )
        {
            if( file.getName().contentEquals( ".classpath" ) || file.getName().contentEquals( ".settings" ) ||
                file.getName().contentEquals( ".project" ) )
            {
                if( file.isDirectory() )
                {
                    FileUtil.deleteDir( file, true );
                }
                file.delete();
            }
        }
    }

    private void disposeBundleElement()
    {
        if( bundleNameField != null && bundleUrlField != null )
        {
            bundleNameField.dispose();
            bundleUrlField.dispose();
            bundleNameLabel.dispose();
            bundleUrlLabel.dispose();
        }
    }

    private void disposeImportElement()
    {
        blankLabel.dispose();
        blankLabel2.dispose();
        importButton.dispose();
        showAllPagesButton.dispose();
        createSeparator.dispose();
        createHorizontalSpacer.dispose();
    }

    private void disposeLayoutElement()
    {
        if( blankLabel != null && importButton != null && createSeparator != null && createHorizontalSpacer != null &&
            serverLabel != null && serverComb != null && serverButton != null )
        {
            disposeImportElement();
            serverLabel.dispose();
            serverComb.dispose();
            serverButton.dispose();
        }
    }

    private void disposeServerEelment()
    {
        if( serverLabel != null && serverComb != null && serverButton != null )
        {
            serverLabel.dispose();
            serverComb.dispose();
            serverButton.dispose();
        }
    }

    @Override
    public int getGridLayoutCount()
    {
        return 2;
    }

    @Override
    public boolean getGridLayoutEqualWidth()
    {
        return false;
    }

    private void getLiferayBudnle( IPath targetSDKLocation, IProgressMonitor monitor ) throws BladeCLIException
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "-b " );
        sb.append( "\"" + targetSDKLocation.toFile().getAbsolutePath() + "\" " );
        sb.append( "init" );

        BladeCLI.execute( sb.toString() );
    }

    @Override
    public String getPageTitle()
    {
        return "Configure Project";
    }

    private IProjectBuilder getProjectBuilder( IProject project ) throws CoreException
    {
        final ILiferayProject liferayProject = LiferayCore.create( project );

        if( liferayProject == null )
        {
            throw new CoreException( ProjectUI.createErrorStatus( "Can't find lifeay workspace project." ) );
        }

        final IProjectBuilder builder = liferayProject.adapt( IProjectBuilder.class );

        if( builder == null )
        {
            throw new CoreException( ProjectUI.createErrorStatus( "Can't find lifeay gradel project builder." ) );
        }

        return builder;
    }

    protected String getSDKDefaultValue()
    {
        String retVal = "";

        try
        {
            IProject sdk = SDKUtil.getWorkspaceSDKProject();

            if( sdk != null )
            {
                retVal = sdk.getLocation().toString();
            }
        }
        catch( CoreException e )
        {
            ProjectUI.logError( "Get workspace default sdk value failed.", e );
        }

        return retVal;
    }

    @Override
    public void getSpecialDescriptor( Composite parent, int style )
    {
        final String descriptor =
            "The first step will help you convert Liferay Plugins SDK 6.2 to Liferay Plugins SDK 7.0 or to  Liferay Workspace. \n" +
                "We will backup your project to a zip file in your eclipse workspace directory.\n" +
                "Click the \"import\" button to import your project into Eclipse workspace" + 
                "(this process maybe need 5-10mins for bundle init).\n" + "Note:\n" +
                "       In order to save time, downloading  7.0 ivy cache  locally could be a good choice to upgrade to liferay plugin sdk 7. \n" +
                "       Theme and ext projects will be ignored for that we do not support to upgrade them  at this tool currently. \n" +
                "       For more details, please see <a>dev.liferay.com</a>.\n";

        String url = "https://dev.liferay.com/develop/tutorials";

        SWTUtil.createHyperLink( this, style, descriptor, 1, url );
    }

    protected void importProject() throws CoreException
    {
        String layout = dataModel.getLayout().content();

        IPath location = PathBridge.create( dataModel.getSdkLocation().content() );

        deleteEclipseConfigFiles( location.toFile() );

        try
        {

            PlatformUI.getWorkbench().getProgressService().run( true, true, new IRunnableWithProgress()
            {

                public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException
                {
                    try
                    {
                        String newPath = "";

                        backupSDK( monitor );

                        clearWorkspaceSDKAndProjects( location, monitor );

                        if( layout.equals( "Use Plugin SDK In Liferay Workspace" ) )
                        {
                            createLiferayWorkspace( location, monitor );

                            newPath = renameProjectFolder( location, monitor );

                            ILiferayProjectImporter importer = LiferayCore.getImporter( "gradle" );

                            importer.importProject( newPath, monitor );

                            createInitBundle( monitor );

                            importSDKProject( new Path( newPath ).append( "plugins-sdk" ), monitor );

                            dataModel.setConvertLiferayWorkspace( true );
                        }
                        else
                        {
                            copyNewSDK( location, monitor );

                            String serverName = dataModel.getLiferayServerName().content();

                            IServer server = ServerUtil.getServer( serverName );

                            IPath serverPath = server.getRuntime().getLocation();

                            newPath = renameProjectFolder( location, monitor );

                            SDK sdk = SDKUtil.createSDKFromLocation( new Path( newPath ) );

                            sdk.addOrUpdateServerProperties( serverPath );

                            SDKUtil.openAsProject( sdk, monitor );

                            importSDKProject( sdk.getLocation(), monitor );

                        }

                    }
                    catch( Exception e )
                    {
                        ProjectUI.logError( e );
                        throw new InvocationTargetException( e, e.getMessage() );
                    }
                }
            } );
        }
        catch( Exception e )
        {
            ProjectUI.logError( e );
            throw new CoreException( StatusBridge.create( Status.createErrorStatus( e.getMessage(), e ) ) );
        }
    }

    private void importSDKProject( IPath targetSDKLocation, IProgressMonitor monitor )
    {
        Collection<File> eclipseProjectFiles = new ArrayList<File>();

        Collection<File> liferayProjectDirs = new ArrayList<File>();

        if( ProjectUtil.collectSDKProjectsFromDirectory(
            eclipseProjectFiles, liferayProjectDirs, targetSDKLocation.toFile(), null, true, monitor ) )
        {
            for( File project : liferayProjectDirs )
            {
                try
                {
                    deleteEclipseConfigFiles( project );

                    IProject importProject =
                        ProjectImportUtil.importProject( new Path( project.getPath() ), monitor, null );

                    if( ProjectUtil.isExtProject( importProject ) || ProjectUtil.isThemeProject( importProject ) ||
                        importProject.getName().startsWith( "resources-importer-web" ) )
                    {
                        importProject.delete( false, true, monitor );
                    }

                    if( importProject != null && importProject.isAccessible() && importProject.isOpen() )
                    {
                        checkProjectType( importProject );
                    }
                }
                catch( CoreException e )
                {
                }
            }

            for( File project : eclipseProjectFiles )
            {
                try
                {

                    deleteEclipseConfigFiles( project.getParentFile() );

                    IProject importProject =
                        ProjectImportUtil.importProject( new Path( project.getParent() ), monitor, null );

                    if( ProjectUtil.isExtProject( importProject ) || ProjectUtil.isThemeProject( importProject ) ||
                        importProject.getName().startsWith( "resources-importer-web" ) )
                    {
                        importProject.delete( false, true, monitor );
                    }

                    if( importProject != null && importProject.isAccessible() && importProject.isOpen() )
                    {
                        checkProjectType( importProject );
                    }
                }
                catch( CoreException e )
                {
                }
            }
        }
    }

    private String renameProjectFolder( IPath targetSDKLocation, IProgressMonitor monitor ) throws CoreException
    {
        // if( newName == null || newName.equals( "" ) )
        // {
        return targetSDKLocation.toString();
        // }
        // java.nio.file.Path newTargetPath;
        // File newFolder = targetSDKLocation.removeLastSegments( 1 ).append( newName ).toFile();
        // boolean renameStatus = targetSDKLocation.toFile().renameTo( newFolder );
        // try
        // {
        // newTargetPath = Files.move( targetSDKLocation.toFile().toPath(), newFolder.toPath(),
        // StandardCopyOption.REPLACE_EXISTING );
        // }
        // catch ( Exception e)
        // {
        // ProjectUI.logError( e );
        // throw new CoreException( StatusBridge.create( Status.createErrorStatus( "Failed to reanme target SDK folder
        // name.", e ) ) );
        // }
        // return newTargetPath.toAbsolutePath().toString();

        // if ( renameStatus == false )
        // {
        // throw new CoreException( StatusBridge.create( Status.createErrorStatus( "Failed to reanme target SDK folder
        // name." ) ) );
        // }
        // else
        // {
        // return newFolder.toPath().toString();
        // }
        //

    }

    private void resetPages()
    {
        UpgradeView.resumePages();

        if( !dataModel.getHasServiceBuilder().content() )
        {
            UpgradeView.removePage( BUILDSERVICE_PAGE_ID );
        }

        if( !dataModel.getHasLayout().content() )
        {
            UpgradeView.removePage( LAYOUTTEMPLATE_PAGE_ID );
        }

        if( !dataModel.getHasHook().content() )
        {
            UpgradeView.removePage( CUSTOMJSP_PAGE_ID );
        }

        if( !dataModel.getHasExt().content() && !dataModel.getHasTheme().content() )
        {
            UpgradeView.removePage( EXTANDTHEME_PAGE_ID );
        }

        UpgradeView.resetPages();
    }

    @Override
    public void serverAdded( IServer server )
    {
        UIUtil.async( new Runnable()
        {

            @Override
            public void run()
            {
                boolean serverExisted = false;

                if( serverComb != null && !serverComb.isDisposed() )
                {
                    String[] serverNames = serverComb.getItems();
                    List<String> serverList = new ArrayList<>( Arrays.asList( serverNames ) );

                    for( String serverName : serverList )
                    {
                        if( server.getName().equals( serverName ) )
                        {
                            serverExisted = true;
                        }
                    }
                    if( serverExisted == false )
                    {
                        serverList.add( server.getName() );
                        serverComb.setItems( serverList.toArray( new String[serverList.size()] ) );
                        serverComb.select( serverList.size() - 1 );
                    }
                    startCheckThread();
                }
            }
        } );
    }

    @Override
    public void serverChanged( IServer server )
    {
    }

    @Override
    public void serverRemoved( IServer server )
    {
        UIUtil.async( new Runnable()
        {

            @Override
            public void run()
            {
                if( serverComb != null && !serverComb.isDisposed() )
                {
                    String[] serverNames = serverComb.getItems();
                    List<String> serverList = new ArrayList<>( Arrays.asList( serverNames ) );

                    Iterator<String> serverNameiterator = serverList.iterator();
                    while( serverNameiterator.hasNext() )
                    {
                        String serverName = serverNameiterator.next();
                        if( server.getName().equals( serverName ) )
                        {
                            serverNameiterator.remove();
                        }
                    }
                    serverComb.setItems( serverList.toArray( new String[serverList.size()] ) );
                    serverComb.select( 0 );
                    startCheckThread();
                }
            }
        } );
    }

    private void startCheckThread()
    {
        final Thread t = new Thread()
        {

            @Override
            public void run()
            {
                validate();
            }
        };

        t.start();
    }

    private void validate()
    {
        UIUtil.async( new Runnable()
        {

            @Override
            public void run()
            {
                boolean inputValidation = true;
                boolean layoutValidation = true;
                String bundUrl = dataModel.getBundleUrl().content();

                String message = "ok";

                if( !sdkValidation.compute().ok() )
                {
                    message = sdkValidation.compute().message();

                    inputValidation = false;
                }
                else
                {
                    inputValidation = true;
                }

                if( layoutComb.getSelectionIndex() == 0 )
                {
                    final int itemCount = serverComb.getItemCount();

                    if( itemCount < 1 )
                    {
                        message = "You shoulde add at least one Liferay 7 portal bundle.";

                        layoutValidation = false;
                    }
                }
                else if( layoutComb.getSelectionIndex() == 1 )
                {
                    boolean liferayWorksapceValidation = true;
                    String workspaceValidationMessage = "ok";

                    try
                    {
                        if( LiferayWorkspaceUtil.hasLiferayWorkspace() )
                        {
                            liferayWorksapceValidation = false;
                            workspaceValidationMessage =
                                "A Liferay Workspace project already exists in this Eclipse instance.. ";
                        }
                    }
                    catch( CoreException e )
                    {
                        liferayWorksapceValidation = false;
                        workspaceValidationMessage =
                            "More than one Liferay workspace build in current Eclipse workspace.. ";
                    }

                    if( !liferayWorksapceValidation && inputValidation )
                    {
                        message = workspaceValidationMessage;
                        layoutValidation = false;
                    }
                    else if( !bundleNameValidation.compute().ok() )
                    {
                        message = bundleNameValidation.compute().message();

                        layoutValidation = false;
                    }
                    else if( bundUrl != null && bundUrl.length() > 0 && !bundleUrlValidation.compute().ok() )
                    {
                        message = bundleUrlValidation.compute().message();

                        layoutValidation = false;
                    }
                    else
                    {
                        layoutValidation = true;
                    }
                }

                triggerValidationEvent( message );

                importButton.setEnabled( layoutValidation && inputValidation );
            }
        } );
    }

}
