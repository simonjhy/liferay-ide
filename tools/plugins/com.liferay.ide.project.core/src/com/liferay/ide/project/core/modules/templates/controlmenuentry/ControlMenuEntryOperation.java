package com.liferay.ide.project.core.modules.templates.controlmenuentry;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class ControlMenuEntryOperation extends AbstractComponentOperation
{

    protected String getTemplateFolder()
    {
        return "controlmenuentry";
    }
    
    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "com.liferay", "com.liferay.product.navigation.control.menu.api", "3.0.0"} );
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.core", "6.0.0"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.service.component.annotations", "1.3.0"} );
        componentDependency.add( new String[]{ "javax.servlet", "javax.servlet-api", "1.3.0"} );
        return componentDependency;
    }    
    
}
