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


import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.ElementType;
import org.eclipse.sapphire.Type;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.ValueProperty;
import org.eclipse.sapphire.modeling.Path;
import org.eclipse.sapphire.modeling.annotations.Required;
import org.eclipse.sapphire.modeling.annotations.Service;

/**
 * @author Simon Jiang
 */

public interface LiferayUpgradeDataModel extends Element
{
    ElementType TYPE = new ElementType( LiferayUpgradeDataModel.class );
    
    
    @Type( base = Path.class )
    @Service( impl = SdkLocationValidationService.class )
    ValueProperty PROP_SDK_LOCATION = new ValueProperty( TYPE, "SdkLocation" );
    
    Value<Path> getSdkLocation();
    void setSdkLocation( String sdkLocation );
    void setSdkLocation( Path sdkLocation );
    
    @Service( impl = ProjectNameValidationService.class )
    ValueProperty PROP_PROJECT_NAME = new ValueProperty( TYPE, "ProjectName" );
    Value<String> getProjectName();
    void setProjectName( String ProjectName );
    
    @Required
    ValueProperty PROP_LAYOUT = new ValueProperty( TYPE, "Layout" );
    Value<String> getLayout();
    void setLayout( String Layout );

    @Required
    ValueProperty PROP_LIFERAY_SERVER_NAME = new ValueProperty( TYPE, "LiferayServerName" );
    Value<String> getLiferayServerName();
    void setLiferayServerName( String value );
}
