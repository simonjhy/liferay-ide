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

import java.util.Vector;

import org.eclipse.debug.core.IStreamListener;

/**
 * @author Simon Jiang
 */

public class PortalListenerList
{

    private static final Vector<IStreamListener> EmptyVector = new Vector<IStreamListener>();

    private Vector<IStreamListener> _listeners = null;

    public PortalListenerList( int capacity )
    {
        if( capacity < 1 )
        {
            throw new IllegalArgumentException();
        }
        this._listeners = new Vector<IStreamListener>();
    }

    public void add( IStreamListener listener )
    {
        _listeners.add( listener );
    }

    public IStreamListener[] getListeners()
    {
        if( _listeners.size() == 0 )
        {
            return EmptyVector.toArray( new IStreamListener[EmptyVector.size()] );
        }

        return _listeners.toArray( new IStreamListener[_listeners.size() ] );
    }

    public void remove( IStreamListener listener )
    {
        if( listener == null )
        {
            throw new IllegalArgumentException();
        }

        _listeners.remove( listener );
    }

    public void removeAll()
    {
        _listeners.removeAllElements();
    }

    public int size()
    {
        return _listeners.size();
    }
}
