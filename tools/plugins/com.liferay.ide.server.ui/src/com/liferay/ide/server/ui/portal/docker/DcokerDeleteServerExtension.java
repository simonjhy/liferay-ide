package com.liferay.ide.server.ui.portal.docker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.DeleteServerDialogExtension;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.liferay.ide.server.core.portal.docker.PortalDockerRuntime;
import com.liferay.ide.server.core.portal.docker.PortalDockerServer;
import com.liferay.ide.server.util.LiferayDockerClient;

public class DcokerDeleteServerExtension extends DeleteServerDialogExtension {

	public DcokerDeleteServerExtension() {
		deleteContainerIds = new ArrayList<String>();
	}

	@Override
	public void createControl(Composite parent) {
	}

	@Override
	public boolean isEnabled() {
		for(IServer server:servers) {
			IRuntime runtime = server.getRuntime();
			
			PortalDockerRuntime dockerRuntime = (PortalDockerRuntime)runtime.loadAdapter(PortalDockerRuntime.class, null);
			
			if ( dockerRuntime != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void performPreDeleteAction(IProgressMonitor monitor) {
		for(IServer server:servers) {
			PortalDockerServer dockerServer = (PortalDockerServer)server.loadAdapter(PortalDockerServer.class, null);
			
			if ( dockerServer != null) {
				deleteContainerIds.add(dockerServer.getContainerId());	
			}
		}
	}

	@Override
	public void performPostDeleteAction(IProgressMonitor monitor) {
		DockerClient dockerClient = LiferayDockerClient.getDockerClient();
		for(String containerId : deleteContainerIds ) {
			try {
				RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId);
				removeContainerCmd.exec();
			}
			catch(NotFoundException nf) {
			}	
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private List<String> deleteContainerIds;
}
