package com.liferay.ide.server.core.portal.docker;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerDelegate;

public class PortalDockerServerDelegate extends ServerDelegate {

	public PortalDockerServerDelegate() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

}
