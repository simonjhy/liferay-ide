package com.liferay.ide.server.ui.portal.docker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.util.ServerLifecycleAdapter;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.core.portal.docker.PortalDockerRuntime;
import com.liferay.ide.server.core.portal.docker.PortalDockerServer;
import com.liferay.ide.server.util.LiferayDockerClient;

public class DockerServerWizard extends WizardFragment {

	public DockerServerWizard() {
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
		if ( _composite != null) {
			return _composite.isComplete();
		}

		return true;
	}

	@Override
	public void exit() {
		try {
			IServerWorkingCopy server = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);
			IRuntime runtime = server.getRuntime();
			PortalDockerRuntime dockerRuntime = (PortalDockerRuntime)runtime.loadAdapter(PortalDockerRuntime.class, null);
			PortalDockerServer dockerServer = (PortalDockerServer)server.loadAdapter(PortalDockerServer.class, null);
			
			DockerClient dockerClient = LiferayDockerClient.getDockerClient();
			
			IPath liferayServerStateLocation = LiferayServerCore.getDefault().getStateLocation();
			Bind bind = new Bind(liferayServerStateLocation.append("deploy").toOSString(), new Volume("/etc/liferay/mount"));
			
			HostConfig hostConfig = HostConfig.newHostConfig();
			hostConfig.withBinds(bind);

			CreateContainerCmd contanerCreateCmd = dockerClient.createContainerCmd(dockerRuntime.getImageRepo());
			List<PortBinding> portBindings = new ArrayList<PortBinding>();

			portBindings.add(new PortBinding(Binding.bindIpAndPort("0.0.0.0",8000), new ExposedPort(8000, InternetProtocol.TCP)));
			portBindings.add(new PortBinding(Binding.bindIpAndPort("0.0.0.0",8080), new ExposedPort(8080, InternetProtocol.TCP)));
			portBindings.add(new PortBinding(Binding.bindIpAndPort("0.0.0.0",11311), new ExposedPort(11311, InternetProtocol.TCP)));

			
			contanerCreateCmd.withName(dockerServer.getContainerName());
			contanerCreateCmd.withEnv("LIFERAY_JPDA_ENABLED=true", "JPDA_ADDRESS=8000", "JPDA_TRANSPORT=dt_socket");
			contanerCreateCmd.withImage(dockerRuntime.getImageRepo());
			contanerCreateCmd.withHostConfig(hostConfig.withPortBindings(portBindings)).withExposedPorts(new ExposedPort(8000, InternetProtocol.TCP));
			CreateContainerResponse createResponse = contanerCreateCmd.exec();

			dockerServer.settContainerId(createResponse.getId());

			ServerCore.addServerLifecycleListener(
				new ServerLifecycleAdapter() {
					public void serverRemoved(IServer removeServer) {
						if ( removeServer.getId().equals(server.getId())) {
							IRuntime runtime = removeServer.getRuntime();
							
							PortalDockerRuntime dockerRuntime = (PortalDockerRuntime)runtime.loadAdapter(PortalDockerRuntime.class, null);
							
							if (dockerRuntime != null) {
								PortalDockerServer dockerPortalServer = (PortalDockerServer)removeServer.loadAdapter(PortalDockerServer.class, null);
								RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(dockerPortalServer.getContainerId());
								removeContainerCmd.exec();
							}						
						}
					}
				});		
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void enter() {
		if (_composite != null) {
			IServerWorkingCopy server = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);

			_composite.setServer(server);			
		}
	}

	private DockerServerSettingComposite _composite;
}
