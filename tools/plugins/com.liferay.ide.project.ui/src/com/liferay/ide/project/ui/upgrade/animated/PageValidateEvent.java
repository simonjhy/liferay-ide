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

/**
 * @author Simon Jiang
 */
public class PageValidateEvent
{

    private String pageId;
    private String ValidationMessage;

    
    public String getValidationMessage()
    {
        return ValidationMessage;
    }

    
    public void setValidationMessage( String validationMessage )
    {
        ValidationMessage = validationMessage;
    }


    
    public String getPageId()
    {
        return pageId;
    }


    
    public void setPageId( String pageId )
    {
        this.pageId = pageId;
    }

}
