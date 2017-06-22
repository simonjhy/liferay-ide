package com.liferay.ide.project.core.modules.templates.controlpanel;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class ControlPanelOperation extends AbstractComponentOperation
{

    protected String getTemplateFolder()
    {
        return "controlpanel";
    }
    
    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "com.liferay","com.liferay.application.list.api","1.0.0"});
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.util.bridges", "2.0.0"} );
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.util.taglib", "2.0.0"} ); 
        componentDependency.add( new String[]{ "javax.portlet", "portlet-api", "2.0"} );
        componentDependency.add( new String[]{ "javax.servlet", "javax.servlet-api", "3.0.1"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.service.component.annotations", "1.3.0"} );
        return componentDependency;
    }    
       
}
