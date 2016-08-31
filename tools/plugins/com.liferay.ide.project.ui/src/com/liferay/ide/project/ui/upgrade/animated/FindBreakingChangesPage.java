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

import com.liferay.ide.project.ui.migration.MigrationView;
import com.liferay.ide.project.ui.migration.RunMigrationToolAction;
import com.liferay.ide.ui.util.UIUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Andy Wu
 * @author Simon Jiang
 */
public class FindBreakingChangesPage extends Page
{
    PageAction[] actions = { new PageFinishAction(), new PageSkipAction() };

    public FindBreakingChangesPage( Composite parent, int style, LiferayUpgradeDataModel dataModel )
    {
        super( parent, style,dataModel );

        GridLayout layout = new GridLayout( 1, true );
        this.setLayout( layout );

        Label title = new Label( this, SWT.LEFT );
        title.setText( "Find Breaking Changes" );
        title.setFont( new Font( null, "Times New Roman", 16, SWT.NORMAL ) );

        Text content = new Text( this, SWT.MULTI );
        final String descriptor =
                        "This step will help you to find  breaking changes for type of java , jsp , xml " +
                        "and properties file.\n" +
                        "It  will not support to find the front-end codes( e.g., javascript, css).\n" +
                        "For service builder, you just need to modify the changes on xxxServiceImp.class, " +
                        "xxxFinder.class, xxxModel.class.\n" +
                        "Others will be solved at step ¡°Build Service¡±. ";

        content.setText( descriptor );

        Button findBCButton = new Button(this, SWT.PUSH);

        findBCButton.setText( "Find Breaking Changes" );

        findBCButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                MigrationView view = (MigrationView) UIUtil.showView( MigrationView.ID );

                new RunMigrationToolAction( "Run Migration Tool", view.getViewSite().getShell() ).run();
            }
        } );

        setActions( actions );
        this.setPageId( FINDBREACKINGCHANGES_PAGE_ID );
    }
}
