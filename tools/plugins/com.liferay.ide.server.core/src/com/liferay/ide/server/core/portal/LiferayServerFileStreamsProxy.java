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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.debug.core.model.IStreamMonitor;

/**
 * @author Simon Jiang
 */

public abstract class LiferayServerFileStreamsProxy implements ITerminateableStreamsProxy
{

    protected LiferayServerOutputStreamMonitor _sysOut;
    protected String _sysoutFile;
    protected Thread _streamThread;
    protected boolean _done = false;
    protected boolean _isPaused = false;
    protected boolean _isMonitorStopping = false;
    BufferedReader _bufferOut = null;
    protected File _fpOut = null;

    public IStreamMonitor getOutputStreamMonitor()
    {
        return _sysOut;
    }

    public boolean isMonitorStopping()
    {
        return _isMonitorStopping;
    }

    public boolean isPaused()
    {
        return _isPaused;
    }

    public boolean isTerminated()
    {
        return _done;
    }

    protected void readToNow( BufferedReader br ) throws IOException
    {
        String s = "";
        while( s != null )
        {
            s = br.readLine();
        }
    }

    protected void setIsMonitorStopping( boolean curIsMonitorStopping )
    {
        _isMonitorStopping = curIsMonitorStopping;
    }

    protected void setIsPaused( boolean curIsPaused )
    {
        _isPaused = curIsPaused;
    }

    protected final boolean shouldReloadFileReader( long originalFileSize, long newFileSize )
    {
        boolean reloadFileReader = true;

        if( originalFileSize <= newFileSize )
        {
            reloadFileReader = false;
        }
        return reloadFileReader;
    }

    protected void startMonitoring()
    {
        if( _streamThread != null )
        {
            return;
        }
        _streamThread = new Thread( "Liferay Server IO Stream")
        {

            public void run()
            {
                boolean isOutInitialized = false;
                boolean isOutFileEmpty = false;

                while( ( !( _done ) ) && ( ( ( !( isOutInitialized ) ) ) ) )
                {
                    try
                    {
                        _fpOut = _sysoutFile!= null? new File( _sysoutFile ):null;

                        if( !( isOutInitialized ) )
                        {
                            if( !( _fpOut.exists() ) )
                            {
                                isOutFileEmpty = true;
                            }
                            else
                            {
                                isOutInitialized = true;
                            }
                        }
                    }
                    catch( Exception e )
                    {
                    }

                    if( ( isOutInitialized ) )
                    {
                        continue;
                    }
                    try
                    {
                        sleep( 200L );
                    }
                    catch( Exception localException1 )
                    {
                    }
                }
                try
                {
                    if( isOutInitialized )
                    {
                        _bufferOut = new BufferedReader( new FileReader( _fpOut ) );

                        if( !( isOutFileEmpty ) )
                        {
                            readToNow( _bufferOut );
                        }
                    }
                }
                catch( Exception e )
                {
                }

                long originalFpOutSize = _fpOut.length();

                while( !( _done ) )
                {
                    try
                    {
                        sleep( 500L );
                    }
                    catch( Exception localException2 )
                    {
                    }

                    try
                    {
                        String s = "";

                        while( s != null )
                        {
                            long newFpOutSize = _fpOut.length();

                            if( shouldReloadFileReader( originalFpOutSize, newFpOutSize ) )
                            {

                                if( _bufferOut != null )
                                {
                                    _bufferOut.close();
                                }
                                _bufferOut = new BufferedReader( new FileReader( _fpOut ) );
                            }
                            originalFpOutSize = newFpOutSize;
                            s = _bufferOut.readLine();

                            if( s != null )
                            {
                                if( isPaused() )
                                {
                                    if( s.startsWith( "************ " ) )
                                    {
                                        _sysOut.append( s + "\n" );
                                        setIsPaused( false );
                                    }
                                }
                                else if( ( isMonitorStopping() ) && ( s.startsWith( "************ " ) ) )
                                {
                                    setIsPaused( true );
                                }
                                else
                                {
                                    _sysOut.append( s + "\n" );
                                }
                            }
                        }
                    }
                    catch( Exception e )
                    {
                    }

                }

                _streamThread = null;
            }
        };
        _streamThread.setPriority( 1 );
        _streamThread.setDaemon( true );
        _streamThread.start();
    }

    public void terminate()
    {
        if( _bufferOut != null )
        {
            try
            {
                _bufferOut.close();
            }
            catch( Exception e )
            {
            }
        }

        _done = true;
    }

    public void write( String input ) throws IOException
    {
    }
}
