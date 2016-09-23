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

import com.liferay.ide.core.IBundleProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.project.core.util.ProjectUtil;
import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.project.ui.dialog.JavaProjectSelectionDialog;
import com.liferay.ide.ui.util.UIUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.sapphire.modeling.Status;
import org.eclipse.sapphire.platform.StatusBridge;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * @author Joye Luo
 */
public class BuildPage extends Page
{

    public BuildPage( Composite parent, int style, LiferayUpgradeDataModel dataModel )
    {
        super( parent, style, dataModel, BUILD_PAGE_ID, true );

        Composite container = new Composite( this, SWT.NONE );
        container.setLayout( new GridLayout( 2, false ) );
        container.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        tableViewer = new TableViewer( container );
        tableViewer.setContentProvider( new TableViewContentProvider() );
        tableViewer.addDoubleClickListener( new IDoubleClickListener()
        {

            @Override
            public void doubleClick( DoubleClickEvent event )
            {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final TableViewElement tableViewElement = (TableViewElement) selection.getFirstElement();
                final String projectName = tableViewElement.projectName;
                final IProject project = ProjectUtil.getProject( projectName );
                final IProject[] selectProjects = new IProject[] { project };

                Boolean openNewLiferayProjectWizard = MessageDialog.openQuestion(
                    UIUtil.getActiveShell(), "Build Project", "Do you want to build this project again?" );

                if( openNewLiferayProjectWizard )
                {
                    buildProjects( selectProjects );
                }
            }
        } );

        TableViewerColumn colFirstName = new TableViewerColumn( tableViewer, SWT.NONE );
        colFirstName.getColumn().setWidth( 400 );
        colFirstName.getColumn().setText( "projectName" );
        colFirstName.setLabelProvider( new ColumnLabelProvider()
        {

            @Override
            public Image getImage( Object element )
            {
                return imageProject;
            }

            @Override
            public String getText( Object element )
            {
                TableViewElement tableViewElement = (TableViewElement) element;
                return tableViewElement.projectName;
            }
        } );

        TableViewerColumn colSecondName = new TableViewerColumn( tableViewer, SWT.NONE );
        colSecondName.getColumn().setWidth( 200 );
        colSecondName.getColumn().setText( "Build Status" );
        colSecondName.setLabelProvider( new ColumnLabelProvider()
        {

            @Override
            public Image getImage( Object element )
            {
                TableViewElement tableViewElement = (TableViewElement) element;
                if( tableViewElement.buildStatus.equals( "build successful" ) )
                {
                    return imageSuccess;
                }
                else if ( tableViewElement.buildStatus.equals( "build failed" ) )
                {
                    return imageFail;
                }
                return imageFail;
            }

            @Override
            public String getText( Object element )
            {
                TableViewElement tableViewElement = (TableViewElement) element;
                return tableViewElement.buildStatus;
            }
        } );

        final Table table = tableViewer.getTable();
        final GridData tableData = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 );
        table.setLayoutData( tableData );
        table.setLinesVisible( false );

        Button buildButton = new Button( container, SWT.PUSH );
        buildButton.setText( "Build..." );
        buildButton.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false, 1, 1 ) );

        buildButton.addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                final List<IProject> selectProjectList = getSelectedProjects();
                final IProject[] selectProjects = selectProjectList.toArray( new IProject[selectProjectList.size()] );
                buildProjects( selectProjects );
            }
        } );
    }

    private TableViewer tableViewer;
    private Image imageProject;
    private Image imageSuccess;
    private Image imageFail;
    private TableViewElement[] tableViewElements;

    class TableViewElement
    {
        public String projectName;
        public String buildStatus;

        public TableViewElement( String projectName, String buildStatus )
        {
            this.projectName = projectName;
            this.buildStatus = buildStatus;
        }
    }

    class TableViewContentProvider implements IStructuredContentProvider
    {

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }

        @Override
        public Object[] getElements( Object inputElement )
        {
            if( inputElement instanceof TableViewElement[] )
            {
                return (TableViewElement[]) inputElement;
            }

            return new Object[] { inputElement };
        }
    }

    private void buildProjects( IProject[] selectProjects )
    {
        createImages();

        try
        {
            final WorkspaceJob workspaceJob = new WorkspaceJob( "Build Project......" )
            {

                @Override
                public IStatus runInWorkspace( IProgressMonitor monitor ) throws CoreException
                {
                    final List<TableViewElement> tableViewElementList = new ArrayList<TableViewElement>();
                    int count = selectProjects.length;

                    if( count <= 0 )
                    {
                        return StatusBridge.create( Status.createOkStatus() );
                    }

                    int unit = 100 / count;

                    monitor.beginTask( "Start to build project......", 100 );

                    for( int i = 0; i < count; i++ )
                    {
                        final IProject project = selectProjects[i];
                        final String projectName = project.getName();
                        TableViewElement tableViewElement = new TableViewElement( projectName, "not build" );
                        tableViewElementList.add( tableViewElement );
                    }
                    
                    UIUtil.async( new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            tableViewElements =
                                tableViewElementList.toArray( new TableViewElement[tableViewElementList.size()] );
                            tableViewer.setInput( tableViewElements );
                            tableViewer.refresh();
                        }
                    } );  
                    
                    for( int i = 0; i < count; i++ )
                    {
                        monitor.worked( i + 1 * unit );

                        if( monitor.isCanceled() )
                        {
                            break;
                        }
                        if( i == count - 1 )
                        {
                            monitor.worked( 100 );
                        }

                        TableViewElement viewElement = tableViewElementList.get( i );
                        final String projectName = viewElement.projectName;
                        
                        final IProject project = ProjectUtil.getProject( projectName );

                        monitor.setTaskName( "Build " + projectName + " Project..." );

                        IBundleProject bundleProject = LiferayCore.create( IBundleProject.class, project );
                        IPath outputBundlepath = null;

                        try
                        {
                            outputBundlepath = bundleProject.getOutputBundle( true, monitor );
                        }
                        catch( Exception e )
                        {
                        }

                        boolean buildStatus = false;

                        if( outputBundlepath != null && !outputBundlepath.isEmpty() )
                        {
                            buildStatus = true;
                        }

                        if ( buildStatus )
                        {
                            viewElement.buildStatus = "build successful";
                        }
                        else
                        {
                            viewElement.buildStatus = "build failed";
                        }
                        
                        
                        UIUtil.async( new Runnable()
                        {

                            @Override
                            public void run()
                            {
                                tableViewElements =
                                    tableViewElementList.toArray( new TableViewElement[tableViewElementList.size()] );
                                tableViewer.setInput( tableViewElements );
                                tableViewer.refresh();
                            }
                        } );
                    }



                    return StatusBridge.create( Status.createOkStatus() );
                }
            };

            workspaceJob.setUser( true );
            workspaceJob.schedule();
        }
        catch( Exception e )
        {
            ProjectUI.logError( e );
        }
    }

    private void createImages()
    {
        imageProject = PlatformUI.getWorkbench().getSharedImages().getImage( SharedImages.IMG_OBJ_PROJECT );

        if( imageProject.isDisposed() )
        {
            imageProject = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                SharedImages.IMG_OBJ_PROJECT ).createImage();
        }

        URL greenTickUrl = ProjectUI.getDefault().getBundle().getEntry( "/images/greentick.png" );
        imageSuccess = ImageDescriptor.createFromURL( greenTickUrl ).createImage();
        imageSuccess.getImageData().scaledTo( 16, 16 );

        imageFail = JFaceResources.getImage( Dialog.DLG_IMG_MESSAGE_ERROR );

    }

    private List<IProject> getSelectedProjects()
    {
        List<IProject> projects = new ArrayList<>();

        final JavaProjectSelectionDialog dialog =
            new JavaProjectSelectionDialog( Display.getCurrent().getActiveShell() );

        if( dialog.open() == Window.OK )
        {
            Object[] selectedProjects = dialog.getResult();

            if( selectedProjects != null )
            {
                for( Object project : selectedProjects )
                {
                    if( project instanceof IJavaProject )
                    {
                        IJavaProject p = (IJavaProject) project;
                        projects.add( p.getProject() );
                    }
                }
            }
        }

        return projects;
    }

    @Override
    public String getDescriptor()
    {
        return "This step will help you to build all your projects.\n" +
            "You can rebuild the project by double click it.";
    }

    @Override
    public String getPageTitle()
    {
        return "Build";
    }

}
