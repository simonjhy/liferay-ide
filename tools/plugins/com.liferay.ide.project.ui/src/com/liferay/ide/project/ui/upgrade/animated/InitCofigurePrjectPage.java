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

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.ui.util.SWTUtil;
import com.liferay.ide.ui.util.UIUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.sapphire.Event;
import org.eclipse.sapphire.Listener;
import org.eclipse.sapphire.Property;
import org.eclipse.sapphire.ValuePropertyContentEvent;
import org.eclipse.sapphire.modeling.Status;
import org.eclipse.sapphire.services.ValidationService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.ServerUIUtil;

/**
 * @author Simon Jiang
 */

@SuppressWarnings( "unused" )
public class InitCofigurePrjectPage extends Page implements IServerLifecycleListener
{
    PageAction[] actions = { new PageFinishAction(), new PageSkipAction() };
    private Text dirField;
    private Text projectNameField;
    private Combo layoutComb;
    private Label layoutLabel;
    private String[] layoutNames = {"Upgrade to Liferay SDK 7", "Use Plugin SDK In Liferay Workspace"};
    private Label serverLabel;
    private Combo serverComb;
    private Button serverButton;
    protected CLabel errorMessageLabel;
    private static Color GRAY;
    
    private class LiferayUpgradeValidationListener extends Listener
    {
        @Override
        public void handle( Event event )
        {
            System.out.print( event.toString() );
            if (event instanceof ValuePropertyContentEvent )
            {
                ValuePropertyContentEvent propertyEvetn  = (ValuePropertyContentEvent)event;
                Property property = propertyEvetn.property();
                Status validation = Status.createOkStatus();
                
                if (property.name().equals( "SdkLocation" ))
                {
                    SdkLocationValidationService sdkValidate = property.service( SdkLocationValidationService.class );
                    validation =  sdkValidate.compute();
                }
                else if (property.name().equals( "ProjectName" ))
                {
                    ProjectNameValidationService projectNameValidate = property.service( ProjectNameValidationService.class );
                    validation =  projectNameValidate.compute();                    
                }
                
                if ( !validation.ok() )
                {
                    errorMessageLabel.setVisible( true );
                    errorMessageLabel.setText( validation.message() );
                }
                else
                {
                    errorMessageLabel.setVisible( false );
                    errorMessageLabel.setText( "" );
                }                         
            }
        }
    }
    
    public InitCofigurePrjectPage( Composite parent, int style, LiferayUpgradeDataModel dataModel )
    {
        super( parent, style, dataModel );

        
        GridLayout layout = new GridLayout(2, false);

        setLayout(layout);
        setLayoutData(new GridData( GridData.FILL_BOTH ));
        setBackground( GRAY );
        
        errorMessageLabel = new CLabel( this, SWT.LEFT_TO_RIGHT );
        errorMessageLabel.setLayoutData( new GridData( SWT.FILL, SWT.BEGINNING, true, false, 2, 1 ) );
        errorMessageLabel.setImage( PlatformUI.getWorkbench().getSharedImages().getImage(
            ISharedImages.IMG_OBJS_ERROR_TSK ) );
        errorMessageLabel.setVisible( false );
        


        //dataModel.getProjectName().attach( new LiferayUpgradeValidationListener());
        
        this.dirField = createTextField( "Liferay SDK folder:" );
        dirField.addModifyListener
        (
            new ModifyListener()
            {
                public void modifyText( ModifyEvent e )
                {
                    if ( e.getSource().equals( dirField ) )
                    {
                        dataModel.setSdkLocation( dirField.getText() );
                    }
                }
            }
        );   
        dirField.setText( "" );
        
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
        });
        this.projectNameField = createTextField( "Project Name:" );
        projectNameField.addModifyListener
        (
            new ModifyListener()
            {
                public void modifyText( ModifyEvent e )
                {
                    if ( e.getSource().equals( projectNameField ) )
                    {
                        dataModel.setProjectName( projectNameField.getText() );
                    }
                    
                }
            }
        ); 
        
        layoutLabel = createLabel( "Select Migrate Layout:" );
        layoutComb = new Combo( this, SWT.DROP_DOWN | SWT.READ_ONLY );
        layoutComb.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        layoutComb.setItems( layoutNames );
        layoutComb.select( 0 );
        layoutComb.addModifyListener( new ModifyListener()
        {
            @Override
            public void modifyText( ModifyEvent e )
            {
                if ( e.getSource().equals( layoutComb ) )
                {
                    int sel = layoutComb.getSelectionIndex();

                    if ( sel > 0 )
                    {
                        serverLabel.setVisible( true );
                        serverButton.setVisible( true );
                        serverComb.setVisible( true );
                    }
                    else
                    {
                        serverLabel.setVisible( false );
                        serverButton.setVisible( false );
                        serverComb.setVisible( false );                       
                    }
                    

                    dataModel.setLayout( layoutComb.getText() );
                }
            }
        } );
        
        serverLabel = createLabel( "Liferay Server Name:" );
        serverComb = new Combo( this, SWT.DROP_DOWN | SWT.READ_ONLY );
        serverComb.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        
        serverComb.addModifyListener
        (
            new ModifyListener()
            {
                public void modifyText( ModifyEvent e )
                {
                    if ( e.getSource().equals( serverComb ) )
                    {
                        dataModel.setLiferayServerName( serverComb.getText() );
                    }
                    
                }
            }
        ); 
        
        serverButton = SWTUtil.createButton( this, "Add Server..." );
        serverButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                ServerUIUtil.showNewServerWizard( parent.getShell(), "liferay.bundle", null,
                                "com.liferay." );
            }
        });  

        
        
        if ( layoutComb.getSelectionIndex() == 0 )
        {
            serverLabel.setVisible( false );
            serverButton.setVisible( false );
            serverComb.setVisible( false );
        }
        
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
        setActions( actions );
        
        dataModel.getSdkLocation().attach( new LiferayUpgradeValidationListener());
        dataModel.getProjectName().attach( new LiferayUpgradeValidationListener());
    }

    @Override
    protected boolean showBackPage()
    {
        return true;
    }

    @Override
    protected boolean showNextPage()
    {
        return true;
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
                    List<String> serverList = new ArrayList<>(Arrays.asList(serverNames));
                    
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
                        serverComb.select( serverList.size()-1);
                    }
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
                if ( serverComb != null && !serverComb.isDisposed())
                {
                    String[] serverNames = serverComb.getItems();
                    List<String> serverList = new ArrayList<>(Arrays.asList(serverNames));
                    
                    Iterator<String> serverNameiterator = serverList.iterator();
                    while(serverNameiterator.hasNext())
                    {
                        String serverName = serverNameiterator.next(); 
                        if ( server.getName().equals( serverName ) )
                        {
                            serverNameiterator.remove();
                        }
                    }
                    serverComb.setItems( serverList.toArray( new String[serverList.size()] ) );
                    serverComb.select( 0 );
                }
            }
        });
    }
}
