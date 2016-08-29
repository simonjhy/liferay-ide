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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.liferay.ide.project.ui.upgrade.animated.UpgradeView.PageValidationListener;

/**
 * @author Simon Jiang
 */
public abstract class Page extends Composite
{
    protected LiferayUpgradeDataModel dataModel;
    
    public Page( Composite parent, int style, LiferayUpgradeDataModel dataModel )
    {
        super( parent, style );
        this.dataModel = dataModel;
    }
    
    public static final int NONE = -1;

    private int pageId;
    
    private int index;

    private String title = "title";

    private int choice = NONE;
    
    protected PageAction[] actions;
    
    private PageAction selectedAction;

    public final int getIndex()
    {
      return index;
    }
    
    public void setIndex( int index )
    {
        this.index = index;
    }
    
    public void setTitle( String title )
    {
        this.title = title;
    }
    
    public String getTitle()
    {
        return this.title;
    }

    public PageAction[] getActions()
    {
        return this.actions;
    }

    public final void setActions(PageAction[] actions)
    {
      this.actions = actions;
    }

    protected  boolean showBackPage()
    {
        return false;
    }
    protected  boolean showNextPage()
    {
        return false;
    }

    @Override
    public boolean equals( Object obj )
    {
        Page comp = (Page)obj;
        return this.pageId == comp.pageId;
    }
    
    public PageAction getSelectedAction()
    {
        return selectedAction;
    }

    
    public void setSelectedAction( PageAction selectedAction )
    {
        this.selectedAction = selectedAction;
    }
    
    
    protected Text createTextField( String labelText, int style )
    {
        createLabel( labelText );

        Text text = new Text( this, SWT.BORDER | style );
        text.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        return text;
    }
    
    protected Label createLabel( String text )
    {
        Label label = new Label( this, SWT.NONE );
        label.setText( text );

        GridDataFactory.generate( label, 2, 1 );

        return label;
    }
    
    protected Text createTextField( String labelText )
    {
        return createTextField( labelText, SWT.NONE );
    }
    
    
    protected final List<PageValidationListener> pageValidationListeners =
                    Collections.synchronizedList( new ArrayList<PageValidationListener>() );

    public void addPageValidationListener( PageValidationListener listener )
    {
        this.pageValidationListeners.add( listener );
    }
}
