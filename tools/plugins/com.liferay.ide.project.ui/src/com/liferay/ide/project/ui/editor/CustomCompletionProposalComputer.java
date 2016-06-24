
package com.liferay.ide.project.ui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.internal.ui.text.java.JavaAllCompletionProposalComputer;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.SourceViewer;

import com.google.common.collect.Sets;

@SuppressWarnings( { "unchecked", "rawtypes" } )
//public class CustomCompletionProposalComputer implements IJavaCompletionProposalComputer
public class CustomCompletionProposalComputer extends JavaAllCompletionProposalComputer
implements ICompletionListener, ICompletionListenerExtension2 
{
	
    public static final String JDT_ALL_CATEGORY = "org.eclipse.jdt.ui.javaAllProposalCategory"; //$NON-NLS-1$
    public static final String JDT_NON_TYPE_CATEGORY = "org.eclipse.jdt.ui.javaNoTypeProposalCategory"; //$NON-NLS-1$
    public static final String JDT_TYPE_CATEGORY = "org.eclipse.jdt.ui.javaTypeProposalCategory"; //$NON-NLS-1$

    
	public ContentAssistantFacade contentAssist;
	
    private void registerCompletionListener() {
        ITextViewer v = jdtContext.getViewer();
        if (!(v instanceof SourceViewer)) {
            return;
        }
        SourceViewer sv = (SourceViewer) v;
        contentAssist = sv.getContentAssistantFacade();
    }

	
    private void unregisterCompletionListener() {
        if (contentAssist != null) {
//            contentAssist.removeCompletionListener(this);
        }
    }
    
    @Override
    public void applied(ICompletionProposal proposal) {
        unregisterCompletionListener();
    }
    
	public JavaContentAssistInvocationContext jdtContext;
	
    @Override
    public void sessionStarted()
    {
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(final Object object) {
        return (T) object;
    }
    
    private void storeContext(ContentAssistInvocationContext context) {
        jdtContext = cast(context);
        //crContext = new RecommendersCompletionContext(jdtContext, astProvider, functions);
    }
    
    private boolean isJdtJavaProposalsEnabled(Set<String> excludedCategories) {
        return !excludedCategories.contains(JDT_ALL_CATEGORY) || !excludedCategories.contains(JDT_TYPE_CATEGORY)
                || !excludedCategories.contains(JDT_NON_TYPE_CATEGORY);
    }

    
    protected boolean isContentAssistConfigurationOkay() {
        Set<String> excludedCategories = Sets.newHashSet(PreferenceConstants.getExcludedCompletionProposalCategories());

        if (isJdtJavaProposalsEnabled(excludedCategories)) {
            return false;
        }
        return true;
    }	
    
    public final ICompilationUnit getCompilationUnit(final ContentAssistInvocationContext context)
    {
        ICompilationUnit cunit = null;
        if (context instanceof JavaContentAssistInvocationContext)
        {
            JavaContentAssistInvocationContext jcontext = (JavaContentAssistInvocationContext) context;

            if (jcontext.getCoreContext() != null)
            {
                cunit = jcontext.getCompilationUnit();
            }
        }
        

        return cunit;
    }
    
    public static ITypeBinding getBindingType(final ASTNode node)
    {
        ITypeBinding type = null;
    
        if (node instanceof AnonymousClassDeclaration)
        {
            type = ((AnonymousClassDeclaration) node).resolveBinding();
        }
        else if( node instanceof ArrayInitializer )
        {
            ASTNode parent = node.getParent();
            
            ASTNode parent2 = parent.getParent();
            List expressions = ((ArrayInitializer) node).expressions();
            
            System.out.println( "aaa" );
        }
        else if( node instanceof AnnotationTypeDeclaration )
        {
            type = ((AnnotationTypeDeclaration) node).resolveBinding();
        }
        
        return type;
    }
    
    @Override
    public List computeCompletionProposals( ContentAssistInvocationContext context, IProgressMonitor monitor )
    {
    	storeContext(context);
        if (!(context instanceof JavaContentAssistInvocationContext)) {
            return Collections.emptyList();
        }
        
        int invocationOffset = jdtContext.getInvocationOffset();
        ICompilationUnit jCmuit = jdtContext.getCompilationUnit();
        CompletionProposalCollector collector= new CompletionProposalCollector(jCmuit);
        try {
			jCmuit.codeComplete(invocationOffset, collector);
		} catch (JavaModelException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        IJavaCompletionProposal[] proposals2= collector.getJavaCompletionProposals();
        
        ArrayList<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
        CompilationUnit astRoot = null;
        ITypeBinding bindingType = null, paramType = null;
        try
        {
            IDocument document = context.getDocument();
            System.out.println( document.getChar( context.getInvocationOffset() ) );
        }
        catch( BadLocationException e1 )
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        ICompilationUnit cunit = getCompilationUnit(context);
        
        if (cunit != null)
        {
            
            astRoot = ASTUtil.getAstOrParse(cunit, monitor);
            
            if( astRoot != null )
            {
                try
                {
                    IRegion lineInformationOfOffset = context.getDocument().getLineInformationOfOffset( context.getInvocationOffset() );
                    
                    String contentType = context.getDocument().getContentType( context.getInvocationOffset());
                    
                    ASTNode node = NodeFinder.perform(astRoot, context.getInvocationOffset(), 0);
                    node.accept( new ASTVisitor(){

                        @Override
                        public boolean visit( Assignment node )
                        {
                            System.out.println( "Assignment" );
                            return super.visit( node );
                        }

                        @Override
                        public boolean visit( StringLiteral node )
                        {
                            System.out.println( "StringLiteral" );
                            return super.visit( node );
                        }

                        @Override
                        public boolean visit( ArrayInitializer node )
                        {
                            System.out.println( "ArrayInitializer" );
                            return super.visit( node );
                        }

                        @Override
                        public boolean visit( ExpressionStatement node )
                        {
                            System.out.println( "ExpressionStatement" );
                            return super.visit( node );
                        }

                        @Override
                        public boolean visit( MemberValuePair node )
                        {
                            System.out.println( "MemberValuePair" );
                            return super.visit( node );
                        }

                        @Override
                        public boolean visit( NormalAnnotation node )
                        {
                            System.out.println( "NormalAnnotation" );
                            return super.visit( node );
                        }

                        @Override
                        public boolean visit( TypeLiteral node )
                        {
                            System.out.println( "TypeLiteral" );
                            return super.visit( node );
                        }
                    });
                    List structuralPropertiesForType = node.structuralPropertiesForType();
                    StructuralPropertyDescriptor location = node.getLocationInParent();
                    ASTNode parent = node.getParent();
                    if ((location != null) && location.isChildProperty())
                       assert parent.getStructuralProperty(location) == node;
                    if ((location != null) && location.isChildListProperty())
                       assert ((List) parent.getStructuralProperty(location)).contains(node);
                    int nodeType = node.getNodeType();
                    if ( nodeType == ASTNode.NORMAL_ANNOTATION )
                    {
                        
                    }
  

                    
//                  StructuralPropertyDescriptor location = node.getLocationInParent();
//                  if ((location != null) && location.isChildProperty())
//                     assert parent.getStructuralProperty(location) == node;
//                  if ((location != null) && location.isChildListProperty())
//                     assert ((List) parent.getStructuralProperty(location)).contains(node);
                  
                  int nodeType2 = parent.getNodeType();
                  bindingType = getBindingType(node);
                  //paramType = findMockedTypeFromNode(node);                    
                }
                catch( BadLocationException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }        
        proposals.add(
            new CompletionProposal(
                "codeandme.blogspot.com", context.getInvocationOffset(), 0, "codeandme.blogspot.com".length() ) );
        proposals.add(
            new CompletionProposal(
                "<your proposal here>", context.getInvocationOffset(), 0, "<your proposal here>".length() ) );
        String[] positionCategories = context.getDocument().getPositionCategories();
            System.out.println( positionCategories.length );
        
        
        return proposals;
    }

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void sessionEnded() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void assistSessionEnded(ContentAssistEvent event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		// TODO Auto-generated method stub
		
	}
}
