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

import org.eclipse.swt.graphics.Color;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Joye Luo
 */
public class GradientHelper
{

    public static Image createGradientImageFor( Composite composite, Color color1, Color color2, boolean vertical )
    {

        Rectangle rect = composite.getClientArea();
        Image image = null;

        if( vertical )
        {
            image = new Image( composite.getDisplay(), 1, Math.max( 1, rect.height ) );
        }
        else
        {
            image = new Image( composite.getDisplay(), Math.max( 1, rect.width ), 1 );
        }

        GC gc = new GC( image );
        gc.setForeground( color1 );
        gc.setBackground( color2 );

        if( vertical )
        {
            gc.fillGradientRectangle( 0, 0, 1, rect.height, true );
        }
        else
        {
            gc.fillGradientRectangle( 0, 0, rect.width, 1, false );
        }

        gc.dispose();

        return image;
    }

}
