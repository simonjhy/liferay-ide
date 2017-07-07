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

package com.liferay.ide.maven.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.wst.server.core.IServer;

import com.liferay.ide.maven.core.util.GenerateServiceAndDependency;
import com.liferay.ide.server.ui.LiferayServerUI;
import com.liferay.ide.server.ui.action.AbstractServerRunningAction;

/**
 * @author Simon Jiang
 */
public class TargetPlatformServiceUPdateAction extends AbstractServerRunningAction
{

    public TargetPlatformServiceUPdateAction()
    {
        super();
    }

    @Override
    protected int getRequiredServerState()
    {
        return IServer.STATE_STARTED;
    }

    public void run( IAction action )
    {
        if( selectedServer != null )
        {
            try
            {
            	GenerateServiceAndDependency serviceCommand = new GenerateServiceAndDependency( selectedServer );
            	serviceCommand.execute();
            }
            catch( Exception e )
            {
                LiferayServerUI.logError( "Error opening portal home folder.", e );
            }
        }
    }
}