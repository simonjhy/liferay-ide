package com.liferay.ide.server.ui.portal.docker;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

public class DockerServerWizard extends WizardFragment {

	public DockerServerWizard() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean hasComposite() {
		return true;
	}
	
	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		_composite = new DockerServerSettingComposite(parent, handle);

		return _composite;
	}
	
	@Override
	public boolean isComplete() {
		boolean retval = false;

		IServerWorkingCopy server = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);
//
//		if (server != null) {
//			IStatus status = server
//
//			retval = (status == null) || (status.getSeverity() != IStatus.ERROR);
//		}

		return true;
	}
	
	@Override
	public void enter() {

		IServerWorkingCopy server = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);

		_composite.setServer(server);
	}

	private DockerServerSettingComposite _composite;
}
