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

package com.liferay.ide.server.core.portal;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStreamMonitor;

/**
 * @author Simon Jiang
 */

public class LiferayServerLogFileStreamsProxy extends LiferayServerFileStreamsProxy
{
    private ILaunch launch;

    public LiferayServerLogFileStreamsProxy(
        PortalRuntime runtime, ILaunch curLaunch )
    {
        this( runtime, curLaunch, new LiferayServerOutputStreamMonitor(), new LiferayServerOutputStreamMonitor() );
    }

    public LiferayServerLogFileStreamsProxy(
        PortalRuntime runtime, 
        ILaunch curLaunch, LiferayServerOutputStreamMonitor systemOut, LiferayServerOutputStreamMonitor systemErr )
    {
        launch = null;

        if( ( runtime == null ) )
        {
            return;
        }

        PortalBundle portalBundle = runtime.getPortalBundle();

        launch = curLaunch;
        try
        {

            _sysoutFile = portalBundle.getLogPath().toOSString();

            if( systemOut != null )
            {
                _sysOut = systemOut;
            }
            else
            {
                _sysOut = new LiferayServerOutputStreamMonitor();
            }

            startMonitoring();
        }
        catch( Exception e )
        {
        }
    }

    public ILaunch getLaunch()
    {
        return launch;
    }

    @Override
    public IStreamMonitor getErrorStreamMonitor()
    {
        return null;
    }
}
