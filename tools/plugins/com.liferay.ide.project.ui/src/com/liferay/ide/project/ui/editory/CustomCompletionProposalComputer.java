
package com.liferay.ide.project.ui.editory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.osgi.container.SystemModule;

@SuppressWarnings( { "unchecked", "rawtypes" } )
public class CustomCompletionProposalComputer implements IJavaCompletionProposalComputer
{

    @Override
    public void sessionStarted()
    {
    }

    @Override
    public List computeCompletionProposals( ContentAssistInvocationContext context, IProgressMonitor monitor )
    {

        ArrayList<CompletionProposal> proposals = new ArrayList<CompletionProposal>();

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
    public List computeContextInformation( ContentAssistInvocationContext context, IProgressMonitor monitor )
    {

        return null;
    }

    @Override
    public String getErrorMessage()
    {
        return null;
    }

    @Override
    public void sessionEnded()
    {
    }
}
