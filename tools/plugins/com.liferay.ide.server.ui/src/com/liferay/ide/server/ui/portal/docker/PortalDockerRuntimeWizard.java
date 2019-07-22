package com.liferay.ide.server.ui.portal.docker;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.liferay.ide.server.core.LiferayServerCore;

public class PortalDockerRuntimeWizard extends WizardFragment {

	public static final String LIST_REMOTE_PORTAL_TAGS_PAGE = "listRemoteLiferayPortaTags";
	public static final String[] WIZARD_PAGES = {
			LIST_REMOTE_PORTAL_TAGS_PAGE
		};

	public PortalDockerRuntimeWizard() {
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		_composite = new PortalDockerRuntimeComposite(parent, handle);

		return _composite;
	}

	
	@Override
	public List getChildFragments() {
		return super.getChildFragments();
	}
	
	@Override
	public void enter() {
		if (_composite != null) {
			IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy)getTaskModel().getObject(TaskModel.TASK_RUNTIME);

			_composite.setRuntime(runtime);
		}
	}

	public void exit() {
		IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy)getTaskModel().getObject(TaskModel.TASK_RUNTIME);

		IPath path = runtime.getLocation();
		IStatus status = runtime.validate(null);

		if (status.getSeverity() != IStatus.ERROR) {
			IRuntimeType runtimeType = runtime.getRuntimeType();

			LiferayServerCore.setPreference("location." + runtimeType.getId(), path.toPortableString());
		}
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public boolean isComplete() {
		boolean retval = false;

		IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy)getTaskModel().getObject(TaskModel.TASK_RUNTIME);

		if (runtime != null) {
			IStatus status = runtime.validate(null);

			retval = (status == null) || (status.getSeverity() != IStatus.ERROR);
		}

		return retval;
	}

	private PortalDockerRuntimeComposite _composite;

}
