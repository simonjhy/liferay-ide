package com.liferay.ide.server.ui.portal.docker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
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
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
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

	public static boolean isValidWorkspace(IProject project) {
		if ((project != null) && (project.getLocation() != null) && isValidWorkspaceLocation(project.getLocation())) {
			return true;
		}

		return false;
	}

	public static boolean isValidWorkspaceLocation(IPath path) {
		if (FileUtil.notExists(path)) {
			return false;
		}

		return isValidWorkspaceLocation(path.toOSString());
	}

	public static boolean isValidWorkspaceLocation(String location) {
		if (isValidGradleWorkspaceLocation(location)) {
			return true;
		}

		return false;
	}
	
	private static final String _BUILD_GRADLE_FILE_NAME = "build.gradle";

	private static final String _GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";

	private static final String _SETTINGS_GRADLE_FILE_NAME = "settings.gradle";
	private static final Pattern _workspacePluginPattern = Pattern.compile(
			".*apply.*plugin.*:.*[\'\"]com\\.liferay\\.workspace[\'\"].*", Pattern.MULTILINE | Pattern.DOTALL);

	public static boolean isValidGradleWorkspaceLocation(String location) {
		File workspaceDir = new File(location);

		File buildGradle = new File(workspaceDir, _BUILD_GRADLE_FILE_NAME);
		File settingsGradle = new File(workspaceDir, _SETTINGS_GRADLE_FILE_NAME);
		File gradleProperties = new File(workspaceDir, _GRADLE_PROPERTIES_FILE_NAME);

		if (FileUtil.notExists(buildGradle) || FileUtil.notExists(settingsGradle) ||
			FileUtil.notExists(gradleProperties)) {

			return false;
		}

		String settingsContent = FileUtil.readContents(settingsGradle, true);

		if (settingsContent != null) {
			Matcher matcher = _workspacePluginPattern.matcher(settingsContent);

			if (matcher.matches()) {
				return true;
			}
		}

		return false;
	}
	
	public static IProject getWorkspaceProject() {
		IProject[] projects = CoreUtil.getAllProjects();

		for (IProject project : projects) {
			if (isValidWorkspace(project)) {
				return project;
			}
		}

		return null;
	}
	
	public static IWorkspaceProject getLiferayWorkspaceProject() {
		IProject workspaceProject = getWorkspaceProject();

		if (workspaceProject != null) {
			return LiferayCore.create(IWorkspaceProject.class, getWorkspaceProject());
		}

		return null;
	}
 	
	@Override
	public void exit() {
		try {
			IServerWorkingCopy server = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);
			IRuntime runtime = server.getRuntime();
			PortalDockerRuntime dockerRuntime = (PortalDockerRuntime)runtime.loadAdapter(PortalDockerRuntime.class, null);
			PortalDockerServer dockerServer = (PortalDockerServer)server.loadAdapter(PortalDockerServer.class, null);
			
			DockerClient dockerClient = LiferayDockerClient.getDockerClient();
			
//			IPath liferayServerStateLocation = LiferayServerCore.getDefault().getStateLocation();
			
			IProject workspaceProject = getWorkspaceProject();
			Bind bind = new Bind(workspaceProject.getLocation().append("build/docker").toOSString(), new Volume("/etc/liferay/mount"));
			
			HostConfig hostConfig = HostConfig.newHostConfig();
			hostConfig.withBinds(bind);

			CreateContainerCmd contanerCreateCmd = dockerClient.createContainerCmd(dockerRuntime.getImageRepo());
			List<PortBinding> portBindings = new ArrayList<PortBinding>();

			portBindings.add(new PortBinding(Binding.bindIpAndPort("0.0.0.0",8888), new ExposedPort(8000, InternetProtocol.TCP)));
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
						IRuntime runtime = removeServer.getRuntime();
						
						PortalDockerRuntime dockerRuntime = (PortalDockerRuntime)runtime.loadAdapter(PortalDockerRuntime.class, null);

						if (dockerRuntime != null) {
							PortalDockerServer dockerPortalServer = (PortalDockerServer)removeServer.loadAdapter(PortalDockerServer.class, null);
							RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(dockerPortalServer.getContainerId());
							removeContainerCmd.exec();
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
