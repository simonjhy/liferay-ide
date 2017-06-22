package com.liferay.ide.project.core.modules.templates.documentaction;

import com.liferay.ide.project.core.modules.templates.AbstractComponentOperation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;


public class DocumentActionOperation extends AbstractComponentOperation
{

    protected String getTemplateFolder()
    {
        return "documentaction";
    }
    
    @Override
    protected List<String[]> getComponentDependency() throws CoreException
    {
        List<String[]> componentDependency = super.getComponentDependency();
        componentDependency.add( new String[]{ "com.liferay", "com.liferay.document.library.api", "3.0.0"} );
        componentDependency.add( new String[]{ "com.liferay", "com.liferay.dynamic.data.mapping.api", "3.0.0"} );
        componentDependency.add( new String[]{ "com.liferay.portal", "com.liferay.portal.kernel", "2.0.0"} );
        componentDependency.add( new String[]{ "javax.portlet", "portlet-api", "2.0"} );
        componentDependency.add( new String[]{ "javax.servlet", "servlet-api", "2.5"} );
        componentDependency.add( new String[]{ "org.osgi", "org.osgi.service.component.annotations", "1.3.0"} );
        return componentDependency;
    }    
    
}
