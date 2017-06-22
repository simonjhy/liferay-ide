package com.liferay.ide.project.core.modules.templates.resoucebundle;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class ResourceBundleOperation extends AbstractComponentOperation
{

    protected String getTemplateFolder()
    {
        return "resoucebundle";
    }
    
    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.service.component.annotations", "1.3.0"} );
        return componentDependency;
    }    
    
}
