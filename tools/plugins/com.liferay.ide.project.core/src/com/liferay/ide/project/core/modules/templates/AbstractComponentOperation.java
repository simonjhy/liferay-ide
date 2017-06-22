
package com.liferay.ide.project.core.modules.templates;

import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.NodeUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.ComponentTemplateOperation;
import com.liferay.ide.project.core.modules.NewLiferayComponentOp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public abstract class AbstractComponentOperation extends AbstractLiferayComponentTemplate
{
    protected List<ComponentTemplateOperation> parserOperationConfiguration()
    {
        List<ComponentTemplateOperation> opertaionConfigurations = new ArrayList<ComponentTemplateOperation>();
        try
        {
            URL operationFileURL = getClass().getClassLoader().getResource(
                TEMPLATE_DIR + "/" + getTemplateFolder() + "/ComponentOperation.xml" );

            final DocumentBuilderFactory newInstance = DocumentBuilderFactory.newInstance();
            newInstance.setValidating( false );
            final Document doc = newInstance.newDocumentBuilder().parse( new File( FileLocator.toFileURL( operationFileURL ).getFile() ) );
            final Element root = doc.getDocumentElement();
            Element operationNodeElement = NodeUtil.findChildElement( root, "operations" );
            NodeList operationNodes = operationNodeElement.getChildNodes();
            
            for( int i=0; i< operationNodes.getLength(); i++ )
            {
                Node opertaionNode = operationNodes.item( i );

                if( opertaionNode instanceof Element && opertaionNode.getNodeName().equals( "operation" ) )
                {
                    ComponentTemplateOperation opertaionConfiguration = new ComponentTemplateOperation();
                    Element nameElement = NodeUtil.findChildElement( (Element) opertaionNode , "name" );
                    Element sourceElement = NodeUtil.findChildElement( (Element) opertaionNode , "source" );
                    Element typeElement = NodeUtil.findChildElement( (Element) opertaionNode , "type" );
                    Element locatElement = NodeUtil.findChildElement( (Element) opertaionNode , "location" );
                    Element isPrimaryElement = NodeUtil.findChildElement( (Element) opertaionNode , "isPrimary" );
                    Element isChildFolderElement = NodeUtil.findChildElement( (Element) opertaionNode , "isInChildFolder" );
                    opertaionConfiguration.setName( nameElement!=null?nameElement.getTextContent():null );
                    opertaionConfiguration.setSource( sourceElement!=null?sourceElement.getTextContent():null );
                    opertaionConfiguration.setType( typeElement!=null?typeElement.getTextContent():null );
                    opertaionConfiguration.setLocation( locatElement!=null?locatElement.getTextContent():null );
                    opertaionConfiguration.setPrimary( isPrimaryElement!=null?Boolean.parseBoolean( isPrimaryElement.getTextContent() ):false );
                    opertaionConfiguration.setIsInChildFolder( isChildFolderElement!=null?Boolean.getBoolean( isChildFolderElement.getTextContent()):true );
                    opertaionConfigurations.add( opertaionConfiguration );
                }
            }
        }
        catch ( Exception o )
        {
            ProjectCore.logError( o );
        }
        return opertaionConfigurations;
    }
    
    protected abstract String getTemplateFolder();

    protected void freemarkerOperation( IFile srcFile, ComponentTemplateOperation operation ) throws CoreException
    {
        try(OutputStream fos = new FileOutputStream( srcFile.getLocation().toFile() ))
        {
            Template temp = cfg.getTemplate( getTemplateFolder() + "/" + operation.getSource() );

            Map<String, Object> root = getTemplateMap();

            Writer out = new OutputStreamWriter( fos );
            temp.process( root, out );
            fos.flush();
        }
        catch( IOException | TemplateException e )
        {
            throw new CoreException( ProjectCore.createErrorStatus( e ) );
        }
    }

    protected void doMergeBndOperation( ComponentTemplateOperation operation ) throws CoreException
    {
        BndProperties bndProperty = new BndProperties();
        IFile iBndFile = project.getFile( "bnd.bnd" );

        if( iBndFile.exists() )
        {
            File bndFile = iBndFile.getLocation().toFile();

            initBndProperties( bndFile, bndProperty );

            try(OutputStream out = new FileOutputStream( bndFile ))
            {
                readBndTemplate( bndProperty, operation );
                bndProperty.store( out, null );
            }
            catch( Exception e )
            {
                ProjectCore.logError( e );
            }
        }
    }

    protected void readBndTemplate( BndProperties initProperty, ComponentTemplateOperation operation )
        throws IOException
    {
        BndProperties bndPropertyTemplate = new BndProperties();
        URL operationFileURL = getClass().getClassLoader().getResource(
            TEMPLATE_DIR + "/" + getTemplateFolder() + "/" + operation.getSource() );

        initBndProperties( new File( FileLocator.toFileURL( operationFileURL ).getFile() ), bndPropertyTemplate );
        Set<Object> bndKeys = bndPropertyTemplate.keySet();

        for( Object key : bndKeys )
        {
            BndPropertiesValue templateBndValue = (BndPropertiesValue) bndPropertyTemplate.get( key );
            initProperty.addValue( (String) key, templateBndValue );
        }
    }

    protected void doNewPropertiesOperation( ComponentTemplateOperation operation ) throws CoreException
    {
        try
        {
            IFolder resourceFolder = liferayProject.getSourceFolder( "resources" );
            IFile operationFile =
                resourceFolder.getFile( new Path( operation.getLocation() ).append( operation.getName() ) );

            if( operationFile.exists() )
            {
                String originContent = FileUtil.readContents( operationFile.getLocation().toFile(), true );

                URL operationFileURL = getClass().getClassLoader().getResource(
                    TEMPLATE_DIR + "/" + getTemplateFolder() + "/" + operation.getSource() );

                String addContent =
                    FileUtil.readContents( new File( FileLocator.toFileURL( operationFileURL ).getFile() ), true );

                Pattern operationFilePattern = Pattern.compile( "\\$\\{componentNameWithoutTemplateName\\}" );
                String newOperationFileContent =
                    operationFilePattern.matcher( addContent ).replaceAll( componentNameWithoutTemplateName );
                String totalContent = originContent + System.getProperty( "line.separator" ) + newOperationFileContent;

                FileUtil.writeFile( operationFile.getLocation().toFile(), totalContent.getBytes(), projectName );
            }
            else
            {
                if( operationFile.getParent() instanceof IFolder )
                {
                    CoreUtil.prepareFolder( (IFolder) operationFile.getParent() );
                }
                freemarkerOperation( operationFile, operation );
            }
        }
        catch( IOException e )
        {
            throw new CoreException( ProjectCore.createErrorStatus( e ) );
        }
    }

    protected void doMergeResourcesOperation( ComponentTemplateOperation operation ) throws CoreException
    {
        try
        {
            IFolder resourceFolder = liferayProject.getSourceFolder( "resources" );
            IPath templateFolder = new Path( operation.getLocation() );

            if( operation.getIsInChildFolder().booleanValue() )
            {
                templateFolder = templateFolder.append( componentClassName.toLowerCase() );
            }

            if( !templateFolder.toFile().exists() )
            {
                CoreUtil.prepareFolder( resourceFolder.getFolder( templateFolder ) );
            }

            IPath resourceFileFullPath = templateFolder.append( operation.getName() );
            IFile resourceFile = resourceFolder.getFile( resourceFileFullPath );

            if( !resourceFile.getLocation().toFile().exists() )
            {
                freemarkerOperation( resourceFile, operation );
            }
        }
        catch( Exception e )
        {
            throw new CoreException( ProjectCore.createErrorStatus( e ) );
        }
    }

    @Override
    public void doExecute( NewLiferayComponentOp op, IProgressMonitor monitor ) throws CoreException
    {
        try
        {
            initializeOperation( op );
            this.project = CoreUtil.getProject( projectName );

            if( project != null )
            {
                liferayProject = LiferayCore.create( project );

                if( liferayProject != null )
                {
                    initFreeMarker();

                    List<ComponentTemplateOperation> componentTemplateOperation = parserOperationConfiguration();

                    for( ComponentTemplateOperation operation : componentTemplateOperation )
                    {
                        String type = operation.getType();
                        switch( type )
                        {
                        case "java":
                            IFile srcFile = null;

                            if( operation.isPrimary() )
                            {
                                srcFile = prepareClassFile( componentClassName );
                            }
                            else
                            {
                                srcFile = prepareClassFile( componentNameWithoutTemplateName + operation.getName() );
                            }
                            freemarkerOperation( srcFile, operation );
                            break;
                        case "resource":
                            doMergeResourcesOperation( operation );
                            break;
                        case "properties":
                            doNewPropertiesOperation( operation );
                            break;
                        case "bnd":
                            doMergeBndOperation( operation );
                            break;
                        default:
                            break;
                        }
                    }
                    doMergeDependencyOperation();
                    project.refreshLocal( IResource.DEPTH_INFINITE, new NullProgressMonitor() );
                }
            }
        }
        catch( Exception e )
        {
            throw new CoreException( ProjectCore.createErrorStatus( e ) );
        }
    }

    @Override
    protected String getTemplateFile()
    {
        return null;
    }

}
