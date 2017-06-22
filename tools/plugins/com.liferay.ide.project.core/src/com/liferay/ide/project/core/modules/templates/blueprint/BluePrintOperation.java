package com.liferay.ide.project.core.modules.templates.blueprint;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class BluePrintOperation extends AbstractComponentOperation
{

    protected String getTemplateFolder()
    {
        return "blueprint";
    }
    
    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "javax.portlet", "portlet-api", "2.0"} );
        return componentDependency;
    }    
    
}
