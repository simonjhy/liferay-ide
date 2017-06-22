package com.liferay.ide.project.core.modules.templates.configurationaction;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class ConfigurationActionOperation extends AbstractComponentOperation
{
    protected String getTemplateFolder()
    {
        return "configurationaction";
    }

    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "biz.aQute.bnd", "biz.aQute.bndlib", "3.1.0"} );
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "com.liferay", "com.liferay.portal.configuration.metatype", "2.0.0"} );
        componentDependency.add( new String[]{ "javax.portlet", "portlet-api", "2.0"} );
        componentDependency.add( new String[]{ "javax.servlet", "javax.servlet-api", "3.0.1"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.service.component.annotations", "1.3.0"} );
        return componentDependency;
    }    
}
