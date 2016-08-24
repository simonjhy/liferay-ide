package com.liferay.ide.project.ui.upgrade.animated;

import org.eclipse.swt.widgets.Composite;

public class DescriptionUpgradePage extends UpgradePage
{
    
//    protected DescriptionUpgradePage( Composite parent, int style )
//    {
//        super( parent, style );
//        actions = new Action[] { new SkipAction(true), new DescriptionUpgradeFinishAction(false)};
//    }


    public DescriptionUpgradePage( Composite parent, int style )
    {
        super( parent, style );
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean backPage()
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    protected boolean nextPage()
    {
        // TODO Auto-generated method stub
        return true;
    }
    
    protected class DescriptionUpgradeFinishAction extends FinishAction
    {
        public DescriptionUpgradeFinishAction( boolean canFinish )
        {
            super( canFinish );
        }

        @Override
        protected boolean done()
        {
            //check description file upgrade finished or not
            return false;
        }

        
    }
}
