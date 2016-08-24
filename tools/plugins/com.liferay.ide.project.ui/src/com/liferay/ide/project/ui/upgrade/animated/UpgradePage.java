
package com.liferay.ide.project.ui.upgrade.animated;

import org.eclipse.swt.widgets.Composite;

public abstract class UpgradePage extends Composite
{
    public UpgradePage( Composite parent, int style )
    {
        super( parent, style );
    }
    public static final int NONE = -1;
    
    private int index;

    private String title;
    
    private int choice = NONE;
    
    protected Action[] actions;

    public final int getIndex()
    {
      return index;
    }
    
    public String getTitle()
    {
        return this.title;
    }

    public Action[] getActions()
    {
        return this.actions;
    }

    public final void Actions(Action[] actions)
    {
      this.actions = actions;
    }

    
    protected abstract class Action
    {
        protected abstract boolean done();
    }

    public class SkipAction extends Action
    {
        private boolean canSkip;
        
        protected SkipAction( final boolean canSkip )
        {
            this.canSkip = canSkip;
        }

        @Override
        protected boolean done()
        {
            return canSkip;
        }
    }
    
    public class FinishAction extends Action
    {
        private boolean canFinish;
        
        public FinishAction( final boolean canFinish )
        {
            this.canFinish = canFinish;
        }
        
        @Override
        protected boolean done()
        {
            return canFinish;
        }        
    }    
    
    protected abstract boolean backPage();
    protected abstract boolean nextPage();
    
}
