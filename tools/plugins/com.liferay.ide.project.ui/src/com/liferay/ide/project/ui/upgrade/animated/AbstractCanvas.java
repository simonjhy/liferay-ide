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

import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.project.ui.upgrade.animated.util.UIUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author Simon Jiang
 */
public class AbstractCanvas extends Canvas
{
    protected Font baseFont;
    protected Font bigFont;

    private final List<Resource> resources = new ArrayList<Resource>();
    
    public AbstractCanvas( Composite parent, int style )
    {
        super( parent, style );
    }
   
    protected void init()
    {
      Display display = getDisplay();
      
      Font initialFont = getFont();
      FontData[] fontData = initialFont.getFontData();
      for (int i = 0; i < fontData.length; i++)
      {
        fontData[i].setHeight(16);
        fontData[i].setStyle(SWT.BOLD);
      }

      baseFont = new Font(display, fontData);
    }
    
    protected final Font getBaseFont()
    {
      return baseFont;
    }
    
    protected final Font createFont(int pixelHeight, int pixelWidth, String... testStrings)
    {
      if (testStrings.length == 0)
      {
        pixelWidth = Integer.MAX_VALUE;
        testStrings = new String[] { "Ag" };
      }

      Display display = getDisplay();
      GC fontGC = new GC(display);

      try
      {
        FontData[] fontData = baseFont.getFontData();
        int fontSize = 40;
        while (fontSize > 0)
        {
          for (int i = 0; i < fontData.length; i++)
          {
            fontData[i].setHeight(fontSize);
            fontData[i].setStyle(SWT.BOLD);
          }

          Font font = new Font(display, fontData);
          fontGC.setFont(font);

          if (isFontSmallEnough(pixelHeight, pixelWidth, fontGC, testStrings))
          {
            resources.add(font);
            return font;
          }

          font.dispose();
          --fontSize;
        }

        throw new RuntimeException("Could not create font: " + pixelHeight);
      }
      finally
      {
        fontGC.dispose();
      }
    }
    
    private boolean isFontSmallEnough(int pixelHeight, int pixelWidth, GC fontGC, String[] testStrings)
    {
      for (String testString : testStrings)
      {
        Point extent = fontGC.stringExtent(testString);
        if (extent.y > pixelHeight || extent.x > pixelWidth)
        {
          return false;
        }
      }

      return true;
    }
    

    protected final Color createColor(int r, int g, int b)
    {
      Display display = getDisplay();
      Color color = new Color(display, r, g, b);
      resources.add(color);
      return color;
    }
    
    @Override
    public void dispose()
    {
      UIUtil.dispose(resources.toArray(new Resource[resources.size()]));
    }

    protected final Image loadImage( String name )
    {
        URL url = null;
        
        try
        {
            //TODO need to be changed to get image from bundle
            url  = ProjectUI.getDefault().getBundle().getEntry( "images/" + name );
        }
        catch( Exception e )
        {
        }

        ImageDescriptor imagedesc = ImageDescriptor.createFromURL( url );

         Image image = imagedesc.createImage();

        resources.add( image );

        return image;
    }
    
    public Rectangle drawImage(GC gc, Image image, int cX, int cY)
    {
      Rectangle bounds = image.getBounds();
      cX -= bounds.width / 2;
      cY -= bounds.height / 2;
      gc.drawImage(image, cX, cY);
      
      return new Rectangle(cX, cY, bounds.width, bounds.height);
    }
}
