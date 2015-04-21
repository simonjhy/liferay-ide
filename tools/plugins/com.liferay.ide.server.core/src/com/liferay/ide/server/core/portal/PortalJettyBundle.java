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

import com.liferay.ide.server.core.LiferayServerCore;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

/**
 * @author Simon Jiang
 */
public class PortalJettyBundle extends AbstractPortalBundle  implements PortalBundle
{

    public static final int DEFAULT_JMX_PORT = 2099;

    public PortalJettyBundle( IPath path )
    {
       super(path);
    }

    protected int getDefaultJMXRemotePort()
    {
        return DEFAULT_JMX_PORT;
    }

    public String getMainClass()
    {
        return "org.eclipse.jetty.start.Main";
    }

    protected IPath getPortalDir( IPath appServerDir )
    {
        IPath retval = null;

        if( appServerDir != null )
        {
            retval = appServerDir.append( "/webapps/root" );
        }

        return retval;
    }

    public IPath[] getRuntimeClasspath()
    {
        final List<IPath> paths = new ArrayList<IPath>();

        if( this.bundlePath.toFile().exists() )
        {
            try
            {
                addLibs(this.bundlePath,paths);
                addLibs(this.bundlePath.append( "lib" ),paths);
                addLibs(this.bundlePath.append( "lib/jsp" ),paths);
            }
            catch( MalformedURLException e )
            {
            }
        }

        return paths.toArray( new IPath[0] );
    }

    @Override
    public String[] getRuntimeStartProgArgs()
    {
        final List<String> args = new ArrayList<String>();
        return args.toArray( new String[0] );

    }

    @Override
    public String[] getRuntimeStopProgArgs()
    {
        final List<String> args = new ArrayList<String>();
        args.add( "--stop" );
        return args.toArray( new String[0] );
    }

    @Override
    public String[] getRuntimeStartVMArgs()
    {
        final List<String> args = new ArrayList<String>();

        args.add( "-Dcom.sun.management.jmxremote" );
        args.add( "-Dcom.sun.management.jmxremote.authenticate=false" );
        args.add( "-Dcom.sun.management.jmxremote.port=" + jmxRemotePort );
        args.add( "-Dcom.sun.management.jmxremote.ssl=false" );
        //args.add( "-Djetty.home=" + LiferayServerCore.getDefault().getStateLocation() + "/tmp1" );
        //args.add( "-DSTART=" + LiferayServerCore.getDefault().getStateLocation() + "/tmp1/start.config" );
        args.add( "-Dinstall.jetty.home=" +  this.bundlePath );
        args.add( "-DVERBOSE");
        args.add( "-Djetty.port=8080");
        args.add( "-DSTOP.PORT=8082");
        args.add( "-DSTOP.KEY=secret");
        return args.toArray( new String[0] );
    }

    @Override
    public String[] getRuntimeStopVMArgs()
    {
        final List<String> args = new ArrayList<String>();
        return args.toArray( new String[0] );
    }

    public String getType()
    {
        return "jetty";
    }
}