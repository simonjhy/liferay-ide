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

import com.liferay.ide.project.core.upgrade.UpgradeAssistantSettingsUtil;
import com.liferay.ide.project.core.upgrade.UpgradeProblems;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author Gregory Amerson
 * @author Terry Jia
 * @author Lovett Li
 */
public class MigrationContentProvider implements ITreeContentProvider
{

    List<ProblemsContainer> _problems;

    @Override
    public void dispose()
    {
    }

    @Override
    public Object[] getChildren( Object parentElement )
    {
        if( parentElement instanceof ProblemsContainer )
        {
            ProblemsContainer problemsContainer = (ProblemsContainer) parentElement;

            return problemsContainer.getProblemsArray();
        }
        else if( parentElement instanceof UpgradeProblems )
        {
            return ( (UpgradeProblems) parentElement ).getProblems();
        }

        return null;
    }

    @Override
    public Object[] getElements( Object inputElement )
    {
        return _problems.toArray();
    }

    @Override
    public Object getParent( Object element )
    {
        return null;
    }

    @Override
    public boolean hasChildren( Object element )
    {
        if( element instanceof ProblemsContainer )
        {
            ProblemsContainer problemsContainer = (ProblemsContainer) element;
            UpgradeProblems[] upgradeProblemsArray = problemsContainer.getProblemsArray();

            if( upgradeProblemsArray.length == 1 )
            {
                return problemsContainer.getProblemsArray()[0].getProblems().length > 0;
            }
            else
            {
                return true;
            }
        }
        else if( element instanceof UpgradeProblems )
        {
            return ( (UpgradeProblems) element ).getProblems().length > 0;
        }

        return false;
    }

    @Override
    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {
        if( newInput instanceof IWorkspaceRoot )
        {
            _problems = new ArrayList<ProblemsContainer>();

            try
            {
                MigrationProblemsContainer container =
                    UpgradeAssistantSettingsUtil.getObjectFromStore( MigrationProblemsContainer.class );

                if( container != null )
                {
                    _problems.add( container );
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    public void setProblems( List<ProblemsContainer> problems )
    {
        this._problems = problems;
    }

    public int getProblemsCount()
    {
        return this._problems != null ? this._problems.size() : 0;
    }

}
