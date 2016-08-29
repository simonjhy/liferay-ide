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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author Simon Jiang
 */
public abstract class PageAction
{
    protected Image[] images;
    
    private boolean selected = false;

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected( boolean selected )
    {
        this.selected = selected;
    }

    public PageAction()
    {
        images = new Image[5];
    }
    
    public Point getSize()
    {
      Rectangle bounds = images[2].getBounds();
      return new Point(bounds.width, bounds.height);
    }
    
    public Image[] getImages()
    {
        return this.images;
    }
    
    public Image getBageImage()
    {
        return this.images[4];
    }
    
}
