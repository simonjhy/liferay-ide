package com.liferay.ide.project.core.modules.templates.resourcecommand;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class ResourceCommandOperation extends AbstractComponentOperation
{

    protected String getTemplateFolder()
    {
        return "resourcecommand";
    }
    
    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "com.liferay", "com.liferay.document.library.web", "1.0.0"} );
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "javax.portlet", "portlet-api", "2.0"} );
        componentDependency.add( new String[]{ "javax.servlet", "javax.servlet-api", "3.0.1"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.service.component.annotations", "1.3.0"} );
        return componentDependency;
    }    
    
}
