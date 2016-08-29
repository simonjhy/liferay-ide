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

import com.liferay.ide.ui.util.SWTUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * @author Andy
 * @author Simon Jiang
 */
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

    // public static int selection = 0;

    private static Composite pageControler = null;

    private static Page[] pages = null;

    public void setSelectPage( int i )
    {
        StackLayout stackLayout = (StackLayout) pageControler.getLayout();

        stackLayout.topControl = pages[i];

        pageControler.layout();
    }

    public static Page getPage( int i )
    {
        return pages[i];
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

        gear.setGearsNumber( 3 );

        final StackLayout stackLayout = new StackLayout();

        pageControler = new Composite( composite, SWT.BORDER );

        pageControler.setLayout( stackLayout );

        GridData containerData = new GridData( GridData.FILL_HORIZONTAL );
        containerData.grabExcessHorizontalSpace = true;
        containerData.widthHint = 400;
        containerData.heightHint = 500;
        pageControler.setLayoutData( containerData );

        Page page1 = new DescriptionUpgradePage( pageControler, SWT.BORDER, dataModel );
        page1.setIndex( 0 );
        page1.setTitle( "this is first page" );

        Page page2 = new InitCofigurePrjectPage( pageControler, SWT.BORDER, dataModel );
        page2.setIndex( 1 );
        page2.setTitle( "this is second page" );

        Page page3 = new DescriptionUpgradePage3( pageControler, SWT.BORDER, dataModel );
        page3.setIndex( 2 );
        page3.setTitle( "this is third page" );

        pages = new Page[3];

        pages[0] = page1;
        pages[1] = page2;
        pages[2] = page3;

        final NavigatorControl navigator = new NavigatorControl( composite, SWT.NONE, pages );

        navigator.addPageNavigateListener( gear );
        navigator.addPageActionListener( gear );

        gear.addSelectionChangedListener( navigator );
        gear.addSelectionChangedListener( this );

        
        page2.addPageValidationListener( gear );
        
        
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

    @Override
    public void onSelectionChanged( int targetSelection )
    {
        StackLayout stackLayout = (StackLayout) pageControler.getLayout();

        stackLayout.topControl = pages[targetSelection];

        pageControler.layout();

    }

}
