
package com.liferay.ide.project.ui.upgrade.animated;

import com.liferay.ide.ui.util.SWTUtil;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class UpgradeView extends ViewPart implements SelectionChangedListener
{
    protected LiferayUpgradeDataModel dataModel;

    private LiferayUpgradeDataModel createUpgradeModel()
    {
        return LiferayUpgradeDataModel.TYPE.instantiate();
    }

    public UpgradeView()
    {
        super();
        this.dataModel = createUpgradeModel();
    }

    private static Composite pagesSwitchControler = null;

    private static Page[] pages = null;

    public void setSelectPage( int i )
    {
        StackLayout stackLayout = (StackLayout) pagesSwitchControler.getLayout();

        stackLayout.topControl = pages[i];

        pagesSwitchControler.layout();
    }

    public static Page getPage( int i )
    {
        if(i < 0 || i > pages.length - 1 )
        {
            return null;
        }
        else
        {
            return pages[i];
        }
    }

    @Override
    public void createPartControl( Composite parent )
    {
        final Composite composite = SWTUtil.createComposite( parent, 1, 1, GridData.FILL_BOTH );

        composite.setLayout( new GridLayout( 1, true ) );

        GridData grData = new GridData( GridData.FILL_BOTH );
        grData.heightHint = 600;
        grData.widthHint = 300;
        composite.setLayoutData( grData );

        final GearControl gear = new GearControl( composite, SWT.NONE );

        GridData gridData = new GridData( GridData.FILL_HORIZONTAL );
        gridData.grabExcessHorizontalSpace = true;
        gridData.widthHint = 400;
        gridData.heightHint = 130;

        gear.setLayoutData( gridData );

        gear.setGearsNumber( 10 );

        StackLayout stackLayout = new StackLayout();
        
        pagesSwitchControler = new Composite( composite, SWT.BORDER );

        pagesSwitchControler.setLayout( stackLayout );

        GridData containerData = new GridData( GridData.FILL_HORIZONTAL );
        containerData.grabExcessHorizontalSpace = true;
        containerData.widthHint = 400;
        containerData.heightHint = 500;
        pagesSwitchControler.setLayoutData( containerData );

        Page welcomePage = new WelcomePage( pagesSwitchControler, SWT.NONE, dataModel );
        welcomePage.setIndex( 0 );
        welcomePage.setTitle( "Welcome" );

        Page initCofigurePrjectPage = new InitCofigurePrjectPage( pagesSwitchControler, SWT.NONE, dataModel );
        initCofigurePrjectPage.setIndex( 1 );
        initCofigurePrjectPage.setTitle( "Cofigure Projects" );
        
        Page descriptorsPage = new  DescriptorsPage( pagesSwitchControler, SWT.NONE, dataModel );
        descriptorsPage.setIndex( 2 );
        descriptorsPage.setTitle( "Update Descriptor Files" );
        

        Page findBreakingChangesPage = new  FindBreakingChangesPage( pagesSwitchControler, SWT.NONE, dataModel );
        findBreakingChangesPage.setIndex( 3 );
        findBreakingChangesPage.setTitle( "Find Breaking Changes" );
        
        Page buildServicePage = new  BuildServicePage( pagesSwitchControler, SWT.NONE, dataModel );
        buildServicePage.setIndex( 4 );
        buildServicePage.setTitle( "Build Service" );
        
        Page layoutTemplatePage = new  LayoutTemplatePage( pagesSwitchControler, SWT.NONE, dataModel );
        layoutTemplatePage.setIndex( 5 );
        layoutTemplatePage.setTitle( "Layout Template" );
        
        Page customJspPage = new  CustomJspPage( pagesSwitchControler, SWT.NONE, dataModel );
        customJspPage.setIndex( 6 );
        customJspPage.setTitle( "Custom Jsp" );
        
        Page extAndThemePage = new  ExtAndThemePage( pagesSwitchControler, SWT.NONE, dataModel );
        extAndThemePage.setIndex( 7 );
        extAndThemePage.setTitle( "Ext and Theme" );
        
        Page compilePage = new  CompilePage( pagesSwitchControler, SWT.NONE, dataModel );
        compilePage.setIndex( 8 );
        compilePage.setTitle( "Compile" );
        
        Page deployPage = new  DeployPage( pagesSwitchControler, SWT.NONE, dataModel );
        deployPage.setIndex( 9 );
        deployPage.setTitle( "Deploy" );

        List<Page> pageList = new ArrayList<Page>();
                
        pageList.add( welcomePage );
        pageList.add( initCofigurePrjectPage );
        pageList.add( descriptorsPage );
        pageList.add( findBreakingChangesPage );
        pageList.add( buildServicePage );
        pageList.add( layoutTemplatePage );
        pageList.add( customJspPage );
        pageList.add( extAndThemePage );
        pageList.add( compilePage );
        pageList.add( deployPage );
        
        pages = pageList.toArray(  new Page[0] );

        final NavigatorControl navigator = new NavigatorControl( composite, SWT.NONE );

        navigator.addPageNavigateListener( gear );
        navigator.addPageActionListener( gear );

        gear.addSelectionChangedListener( navigator );
        gear.addSelectionChangedListener( this );
        
        GridData navData = new GridData( GridData.FILL_HORIZONTAL );

        navData.grabExcessHorizontalSpace = true;
        navData.widthHint = 400;
        navData.heightHint = 55;

        navigator.setLayoutData( navData );

        setSelectPage( 0 );

    }

    @Override
    public void setFocus()
    {

    }

    public interface PageActionListener
    {

        public void onPageAction( PageActionEvent event );
    }

    public interface PageNavigatorListener
    {
        public void onPageNavigate( PageNavigateEvent event );
    }
    
    public interface PageValidationListener
    {
        public void onValidation( PageValidateEvent event );
    }

    private final List<PageActionListener> actionListeners = new ArrayList<PageActionListener>();
    private final List<PageNavigatorListener> navigatorListeners = new ArrayList<PageNavigatorListener>();

    public final void addActionListener( PageActionListener listener )
    {
        synchronized( actionListeners )
        {
            actionListeners.add( listener );
        }
    }

    public final PageActionListener[] getActionListeners()
    {
        synchronized( actionListeners )
        {
            return actionListeners.toArray( new PageActionListener[actionListeners.size()] );
        }
    }

    public final void removeActionListener( PageActionListener listener )
    {
        synchronized( actionListeners )
        {
            actionListeners.remove( listener );
        }
    }

    public final void addNavigatorListener( PageNavigatorListener listener )
    {
        synchronized( navigatorListeners )
        {
            navigatorListeners.add( listener );
        }
    }

    public PageNavigatorListener[] getPageNavigatorListener()
    {
        synchronized( navigatorListeners )
        {
            return navigatorListeners.toArray( new PageNavigatorListener[navigatorListeners.size()] );
        }
    }

    public final void removeNavigatorListener( PageNavigatorListener listener )
    {
        synchronized( navigatorListeners )
        {
            navigatorListeners.remove( listener );
        }
    }

    @Override
    public void onSelectionChanged( int targetSelection )
    {
        StackLayout stackLayout = (StackLayout) pagesSwitchControler.getLayout();

        stackLayout.topControl = pages[targetSelection];

        pagesSwitchControler.layout();

    }

}
