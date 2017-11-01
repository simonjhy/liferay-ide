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

package com.liferay.ide.project.ui.migration;

import com.liferay.blade.api.Migration;
import com.liferay.blade.api.MigrationConstants;
import com.liferay.blade.api.Problem;
import com.liferay.blade.api.ProgressMonitor;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.MarkerUtil;
import com.liferay.ide.project.core.upgrade.FileProblems;
import com.liferay.ide.project.core.upgrade.FileProblemsUtil;
import com.liferay.ide.project.core.upgrade.IgnoredProblemsContainer;
import com.liferay.ide.project.core.upgrade.MigrationProblems;
import com.liferay.ide.project.core.upgrade.UpgradeAssistantSettingsUtil;
import com.liferay.ide.project.core.upgrade.UpgradeProblems;
import com.liferay.ide.project.core.util.ValidationUtil;
import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.project.ui.upgrade.animated.FindBreakingChangesPage;
import com.liferay.ide.project.ui.upgrade.animated.Page;
import com.liferay.ide.project.ui.upgrade.animated.UpgradeView;
import com.liferay.ide.ui.util.UIUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * @author Gregory Amerson
 * @author Andy Wu
 * @author Lovett Li
 * @author Terry Jia
 */
@SuppressWarnings( "unchecked" )
public class MigrateProjectHandler extends AbstractHandler
{

    private String[] ignoreSuperClasses =
                    {   "com.liferay.portal.service.BaseLocalServiceImpl",
                        "com.liferay.portal.model.CacheModel",
                        "com.liferay.portal.model.impl.BaseModelImpl",
                        "com.liferay.portal.service.BaseServiceImpl",
                        "com.liferay.portal.service.persistence.impl.BasePersistenceImpl",
                        "com.liferay.portal.NoSuchModelException",
                        "com.liferay.portal.model.PersistedModel",
                        "com.liferay.portal.model.BaseModel",
                        "com.liferay.portal.model.StagedGroupedModel",
                        "com.liferay.portal.model.ModelWrapper",
                        "com.liferay.portal.service.BaseLocalService",
                        "com.liferay.portal.service.InvokableLocalService",
                        "com.liferay.portal.service.PersistedModelLocalService",
                        "com.liferay.portal.service.ServiceWrapper",
                        "com.liferay.portal.service.BaseService",
                        "com.liferay.portal.service.InvokableService",
                        "com.liferay.portal.kernel.messaging.BaseMessageListener",
                        "com.liferay.portal.kernel.dao.orm.BaseActionableDynamicQuery",
                        "com.liferay.portal.service.persistence.BasePersistence"
                    };

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException
    {
        ISelection selection = HandlerUtil.getCurrentSelection( event );

        if( selection instanceof IStructuredSelection )
        {
            Object element = null;

            if( ( (IStructuredSelection) selection ).size() > 1 )
            {
                element = ( (IStructuredSelection) selection ).toArray();
            }
            else
            {
                element = ( (IStructuredSelection) selection ).getFirstElement();
            }

            IProject project = null;
            IProject[] projects = null;

            if( element instanceof IProject )
            {
                project = (IProject) element;
            }
            else if( element instanceof IAdaptable )
            {
                IAdaptable adaptable = (IAdaptable) element;

                project = (IProject) adaptable.getAdapter( IProject.class );
            }
            else if( element instanceof Object[] )
            {
                projects = Arrays.copyOf( (Object[]) element, ( (Object[]) element ).length, IProject[].class );
            }

            if( project != null )
            {
                if( shouldShowMessageDialog( project ) )
                {
                    final boolean shouldContinue = showMessageDialog( project );

                    if( !shouldContinue )
                    {
                        return null;
                    }

                    MarkerUtil.clearMarkers( project, MigrationConstants.MARKER_TYPE, null );
                }

                setButtonState( false );
                final IPath location = project.getLocation();

                findMigrationProblems( new IPath[] { location }, new String[] { project.getName() } );
            }
            else if( projects != null )
            {
                final List<IPath> locations = new ArrayList<>();
                final List<String> projectNames = new ArrayList<>();
                boolean shouldContinue = false;

                for( IProject iProject : projects )
                {
                    if( shouldContinue == false && shouldShowMessageDialog( iProject ) )
                    {
                        shouldContinue = showMessageDialog( project );

                        if( !shouldContinue )
                        {
                            return null;
                        }

                    }
                    if( shouldContinue && shouldShowMessageDialog( iProject ) )
                    {
                        MarkerUtil.clearMarkers( iProject, MigrationConstants.MARKER_TYPE, null );
                    }

                    locations.add( iProject.getLocation() );
                    projectNames.add( iProject.getName() );
                }

                setButtonState( false );
                findMigrationProblems( locations.toArray( new IPath[0] ), projectNames.toArray( new String[0] ) );
            }
        }

        return null;
    }

    public void findMigrationProblems( final IPath[] locations )
    {
        findMigrationProblems( locations, new String[] { "" } );
    }

    public void findMigrationProblems( final IPath[] locations, final String[] projectName )
    {
        Job job = new WorkspaceJob( "Finding migration problems..." )
        {

            @Override
            public IStatus runInWorkspace( final IProgressMonitor monitor ) throws CoreException
            {
                IStatus retval = Status.OK_STATUS;

                final BundleContext context = FrameworkUtil.getBundle( this.getClass() ).getBundleContext();

                ProgressMonitor override = new ProgressMonitor()
                {

                    @Override
                    public void worked( int work )
                    {
                        monitor.worked( work );
                    }

                    @Override
                    public void setTaskName( String taskName )
                    {
                        monitor.setTaskName( taskName );
                    }

                    @Override
                    public boolean isCanceled()
                    {
                        return monitor.isCanceled();
                    }

                    @Override
                    public void done()
                    {
                        monitor.done();
                    }

                    @Override
                    public void beginTask( String taskName, int totalWork )
                    {
                        monitor.beginTask( taskName, totalWork );
                    }
                };

                try
                {
                    final ServiceReference<Migration> sr = context.getServiceReference( Migration.class );
                    final Migration m = context.getService( sr );
                    List<Problem> allProblems = null;

                    boolean isSingleFile =
                        locations.length == 1 && locations[0].toFile().exists() && locations[0].toFile().isFile();

                    for( int j = 0; j < locations.length; j++ )
                    {
                        allProblems = new ArrayList<>();

                        if( !override.isCanceled() && shouldSearch( locations[j].toFile() ) )
                        {
                            List<Problem> problems = null;

                            if( isSingleFile )
                            {
                                clearFileMarkers( locations[j].toFile() );

                                Set<File> files = new HashSet<>();

                                files.add( locations[j].toFile() );

                                problems = m.findProblems( files, override );
                            }
                            else
                            {
                                problems = m.findProblems( locations[j].toFile(), override );
                            }

                            for( Problem problem : problems )
                            {
                                if( shouldAdd( problem ) )
                                {
                                    allProblems.add( problem );
                                }
                            }
                        }

                        MigrationProblemsContainer container = null;

                        container = UpgradeAssistantSettingsUtil.getObjectFromStore( MigrationProblemsContainer.class );

                        MigrationProblems[] migrationProblemsArray = null;

                        List<MigrationProblems> migrationProblemsList = new ArrayList<MigrationProblems>();

                        if( container == null )
                        {
                            container = new MigrationProblemsContainer();
                        }

                        if( container.getProblemsArray() != null )
                        {
                            migrationProblemsArray = container.getProblemsArray();
                        }

                        if( migrationProblemsArray != null )
                        {
                            List<MigrationProblems> mpList = Arrays.asList( migrationProblemsArray );

                            for( MigrationProblems mp : mpList )
                            {
                                migrationProblemsList.add( mp );
                            }
                        }

                        if( allProblems.size() > 0 )
                        {

                            addMarkers(allProblems);

                            MigrationProblems migrationProblems = new MigrationProblems();

                            List<FileProblems> fileProblemsList =
                                FileProblemsUtil.newFileProblemsListFrom( allProblems.toArray( new Problem[0] ) );

                            migrationProblems.setProblems( fileProblemsList.toArray( new FileProblems[0] ) );

                            migrationProblems.setType( "Code Problems" );
                            migrationProblems.setSuffix( projectName[j] );

                            int index = isAlreadyExist( migrationProblemsList, projectName, j );

                            if( index != -1 )
                            {
                                if( isSingleFile )
                                {
                                    UpgradeProblems up = migrationProblemsList.get( index );

                                    FileProblems[] problems = up.getProblems();

                                    for( int n = 0; n < problems.length; n++ )
                                    {
                                        FileProblems fp = problems[n];

                                        if( fp.getFile().getPath().equals( locations[0].toFile().getPath() ) )
                                        {
                                            problems[n] = fileProblemsList.get( 0 );

                                            break;
                                        }
                                    }

                                    migrationProblems.setProblems( problems );
                                }

                                migrationProblemsList.set( index, migrationProblems );
                            }
                            else
                            {
                                migrationProblemsList.add( migrationProblems );
                            }

                        }
                        else
                        {
                            int index = isAlreadyExist( migrationProblemsList, projectName, j );

                            if( index != -1 )
                            {
                                if( isSingleFile )
                                {
                                    MigrationProblems mp = migrationProblemsList.get( index );
                                    FileProblems[] fps = mp.getProblems();
                                    List<FileProblems> fpList = Arrays.asList( fps );
                                    List<FileProblems> newFPList = new ArrayList<>();
                                    for( FileProblems fp : fpList )
                                    {
                                        if( !fp.getFile().getPath().equals( locations[0].toFile().getPath() ) )
                                        {
                                            newFPList.add( fp );
                                        }
                                    }

                                    mp.setProblems( newFPList.toArray( new FileProblems[0] ) );

                                    migrationProblemsList.set( index, mp );
                                }
                                else
                                {
                                    migrationProblemsList.remove( index );
                                }
                            }
                        }

                        if( migrationProblemsList.size() > 0 )
                        {
                            container.setProblemsArray( migrationProblemsList.toArray( new MigrationProblems[0] ) );
                            UpgradeAssistantSettingsUtil.setObjectToStore( MigrationProblemsContainer.class, container );
                        }
                        else
                        {
                            UpgradeAssistantSettingsUtil.setObjectToStore( MigrationProblemsContainer.class, null );
                        }

                    }

                    if( isSingleFile )
                    {
                       refreshViewer(allProblems);
                    }
                    else
                    {
                        allProblems.add( new Problem() );
                        m.reportProblems( allProblems, Migration.DETAIL_LONG, "ide" );
                    }
                }
                catch( Exception e )
                {
                    retval = ProjectUI.createErrorStatus( "Error in migrate command", e );
                }

                return retval;
            }
        };

        try
        {
            PlatformUI.getWorkbench().getProgressService().showInDialog( Display.getDefault().getActiveShell(), job );
        }
        catch( Exception e )
        {
        }
        job.schedule();
    }

    private boolean showMessageDialog( IProject project )
    {
        final Display display = Display.getDefault();
        final Shell shell = display.getActiveShell();

        return MessageDialog.openConfirm(
            shell, "Migrate Liferay Plugin",
            "This project already contains migration problem markers.  All existing markers will be deleted.  " +
                "Do you want to continue to run migration tool?" );
    }

    private boolean shouldShowMessageDialog( IProject project )
    {
        final IMarker[] markers = MarkerUtil.findMarkers( project, MigrationConstants.MARKER_TYPE, null );

        return markers != null && markers.length > 0;
    }

    private boolean shouldAdd( Problem problem )
    {
        IgnoredProblemsContainer ignoredProblemsContainer = MigrationUtil.getIgnoredProblemsContainer();

        if( ignoredProblemsContainer != null )
        {
            Set<String> ticketSet = ignoredProblemsContainer.getProblemMap().keySet();

            if( ticketSet != null && ticketSet.contains( problem.getTicket() ) )
            {
                final IResource resource = MigrationUtil.getIResourceFromProblem( problem );
                final IMarker marker = resource.getMarker( problem.getMarkerId() );

                if( marker.exists() )
                {
                    try
                    {
                        marker.delete();
                    }
                    catch( CoreException e )
                    {
                        ProjectUI.logError( e );
                    }
                }

                return false;
            }
        }

        return true;
    }

    @SuppressWarnings( "deprecation" )
    private boolean shouldSearch( File file )
    {
        String path = file.getAbsolutePath().replaceAll( "\\\\", "/" );

        if( path.contains( "WEB-INF/classes" ) || path.contains( "WEB-INF/service" ) ||
            ValidationUtil.isProjectTargetDirFile( file ) )
        {
            return false;
        }

        if( path.endsWith( "java" ) )
        {
            CompilationUnit ast = CUCache.getCU( file, FileUtil.readContents( file ).toCharArray() );

            Name superClass = ( (TypeDeclaration) ast.types().get( 0 ) ).getSuperclass();

            if( superClass != null )
            {
                return !checkClassIgnore( ast, superClass.toString() );
            }

            List<ASTNode> interfaces = ( (TypeDeclaration) ast.types().get( 0 ) ).superInterfaces();

            if( interfaces != null )
            {
                for( ASTNode n : interfaces )
                {
                    String name = n.toString();

                    if( checkClassIgnore( ast, name ) )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean checkClassIgnore( CompilationUnit ast, String className )
    {

        List<ImportDeclaration> importDeclarationList = ast.imports();

        if( importDeclarationList != null )
        {
            String fullClassName = "";

            for( ImportDeclaration importDeclaration : importDeclarationList )
            {
                String importName = importDeclaration.getName().toString();

                if( importName.endsWith( className ) )
                {
                    fullClassName = importName;

                    break;
                }
            }

            if( !fullClassName.equals( "" ) )
            {
                for( String ignoreSuperClass : ignoreSuperClasses )
                {
                    if( ignoreSuperClass.equals( fullClassName ) )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int isAlreadyExist(
        List<MigrationProblems> migrationProblemsList, String[] projectName, int projectIndex )
    {
        int index = -1;

        for( int i = 0; i < migrationProblemsList.size(); i++ )
        {
            UpgradeProblems upgradeProblems = migrationProblemsList.get( i );

            if( ( (MigrationProblems) upgradeProblems ).getSuffix().equals( projectName[projectIndex] ) )
            {
                index = i;

                break;
            }
        }

        return index;
    }

    private void clearFileMarkers( File file )
    {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IPath location = Path.fromOSString( file.getAbsolutePath() );
        IFile projectFile = ws.getRoot().getFileForLocation( location );

        if( projectFile.exists() && projectFile != null )
        {
			MarkerUtil.clearMarkers( projectFile, MigrationConstants.MARKER_TYPE, null );
        }
    }

    protected void refreshViewer(List<Problem> allProblems)
    {
        FindBreakingChangesPage page = UpgradeView.getPage(Page.FINDBREACKINGCHANGES_PAGE_ID,FindBreakingChangesPage.class);
        TableViewer problemsViewer = page.get_problemsViewer();
        TreeViewer treeViewer = page.getTreeViewer();

        UIUtil.async( new Runnable()
        {

            @Override
            public void run()
            {
                problemsViewer.setInput( allProblems );

                Object currentTreeNode = treeViewer.getStructuredSelection().getFirstElement();
                String currentPath = null;

                if( currentTreeNode instanceof FileProblems )
                {
                    FileProblems currentNode = (FileProblems) currentTreeNode;
                    currentPath = currentNode.getFile().getAbsolutePath().toString();
                }
                MigrationContentProvider contentProvider = (MigrationContentProvider) treeViewer.getContentProvider();

                MigrationProblemsContainer mc = (MigrationProblemsContainer) contentProvider._problems.get( 0 );

                for( MigrationProblems project : mc.getProblemsArray() )
                {
                    Iterator<FileProblems> fileProblemItertor = new LinkedList<FileProblems>(Arrays.asList( project.getProblems() )).iterator();

                    while( fileProblemItertor.hasNext())
                    {
                        FileProblems fileProblem = fileProblemItertor.next();

                        if( fileProblem.getFile().getAbsolutePath().toString().equals( currentPath ) )
                        {
                            fileProblem.getProblems().clear();
                            fileProblem.getProblems().addAll( allProblems );
                            break;
                        }
                    }
                }
            }
        } );
    }

    private void addMarkers(List<Problem> problems)
    {
        final IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();

        for( Problem problem : problems )
        {
            IResource workspaceResource = null;

            final File file = problem.file;

            final IResource[] containers = ws.findContainersForLocationURI( file.toURI() );

            if( containers != null && containers.length > 0 )
            {
                // prefer project containers
                for( IResource container : containers )
                {
                    if( container.exists() )
                    {
                        if( container.getType() == IResource.PROJECT )
                        {
                            workspaceResource = container;
                            break;
                        }
                        else
                        {
                            final IProject project = container.getProject();

                            if( CoreUtil.isLiferayProject( project ) )
                            {
                                workspaceResource = container;
                                break;
                            }
                        }
                    }
                }

                if( workspaceResource == null )
                {
                    final IFile[] files = ws.findFilesForLocationURI( file.toURI() );

                    for( IFile ifile : files )
                    {
                        if( ifile.exists() )
                        {
                            if( workspaceResource == null )
                            {
                                if( CoreUtil.isLiferayProject( ifile.getProject() ) )
                                {
                                    workspaceResource = ifile;
                                }
                            }
                            else
                            {
                                // prefer the path that is shortest (to avoid a nested version)
                                if( ifile.getFullPath().segmentCount() < workspaceResource.getFullPath().segmentCount() )
                                {
                                    workspaceResource = ifile;
                                }
                            }
                        }
                    }
                }

                if( workspaceResource == null )
                {
                    for( IResource container : containers )
                    {
                        if( workspaceResource == null )
                        {
                            workspaceResource = container;
                        }
                        else
                        {
                            // prefer the path that is shortest (to avoid a nested version)
                            if( container.getLocation().segmentCount() < workspaceResource.getLocation().segmentCount() )
                            {
                                workspaceResource = container;
                            }
                        }
                    }
                }
            }

            if( workspaceResource != null && workspaceResource.exists() )
            {
                try
                {
                    final IMarker marker =
                        workspaceResource.createMarker( MigrationConstants.MARKER_TYPE );

                    problem.setMarkerId( marker.getId() );
                    MigrationUtil.problemToMarker( problem, marker );
                }
                catch( CoreException e )
                {
                }
            }
        }
    }

    private void setButtonState( boolean enableState )
    {
        FindBreakingChangesPage page =
            UpgradeView.getPage( Page.FINDBREACKINGCHANGES_PAGE_ID, FindBreakingChangesPage.class );
        page.setButtonState( enableState );
    }

}
