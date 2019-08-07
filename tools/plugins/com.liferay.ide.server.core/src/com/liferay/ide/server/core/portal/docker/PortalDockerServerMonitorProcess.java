/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.server.core.portal.docker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.wst.server.core.IServer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.util.LiferayDockerClient;
import com.liferay.ide.server.util.SocketUtil;

/**
 * @author Simon Jiang
 */
public class PortalDockerServerMonitorProcess implements IProcess {

	public PortalDockerServerMonitorProcess(IServer server, final PortalDockerServerBehavior serverBehavior,
			ILaunch launch, boolean debug, IPortalDockerStreamsProxy proxy, ILaunchConfiguration config,
			PortalDockerServerLaunchConfigDelegate delegate, IProgressMonitor monitor) {

		_server = server;
		_portalServer = (PortalDockerServer) server.loadAdapter(PortalDockerServer.class, null);
		_streamsProxy = proxy;
		_launch = launch;
		_config = config;
		_delegate = delegate;
		_debug = debug;
		_dockerClient = LiferayDockerClient.getDockerClient();
		startContainer(monitor);
	}

	public boolean canTerminate() {
		return !_streamsProxy.isTerminated();
	}

	/**
	 * Fires a terminate event.
	 */
	public void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	public <T> T getAdapter(Class<T> adapterType) {
		return (T) null;
	}

	public String getAttribute(String key) {
		return (String) this._attributerMap.get(key);
	}

	public int getExitValue() throws DebugException {
		return 0;
	}

	public String getLabel() {
		if (_label == null) {
			String host = null;
			String port = null;

			if (_server != null) {
				host = _server.getHost();
			}

			PortalDockerServer portalServer = (PortalDockerServer) _server.loadAdapter(PortalDockerServer.class, null);

			if (portalServer != null) {
				port = "8080";
			}

			_label = (host != null ? host : "") + ":" + (port != null ? port : "");
		}

		return _label;
	}

	public ILaunch getLaunch() {
		return _launch;
	}

	public IStreamsProxy getStreamsProxy() {
		return _streamsProxy;
	}

	public boolean isTerminated() {

		InspectContainerCmd inspectContainerCmd = _dockerClient.inspectContainerCmd(_portalServer.getContainerId());
		InspectContainerResponse response = inspectContainerCmd.exec();
		return /* _streamsProxy.isTerminated() && */ !response.getState().getRunning();
	}

	public void setAttribute(String key, String value) {
		_attributerMap.put(key, value);
	}

	public void setStreamsProxy(IPortalDockerStreamsProxy streamsProxy) {
		_streamsProxy = streamsProxy;
	}

	public void terminate() throws DebugException {
		try {
			((IPortalDockerStreamsProxy) getStreamsProxy()).terminate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void fireCreateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager = DebugPlugin.getDefault();

		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[] { event });
		}
	}

//	@SuppressWarnings("restriction")
//	private static GradleRunConfigurationAttributes _getRunConfigurationAttributes(
//			IProject project, List<String> tasks, List<String> arguments) {
//
//			IPath projecLocation = project.getLocation();
//
//			File rootDir = projecLocation.toFile();
//
//			ConfigurationManager configurationManager = CorePlugin.configurationManager();
//
//			BuildConfiguration buildConfig = configurationManager.loadBuildConfiguration(rootDir);
//
//			String projectDirectoryExpression = null;
//
//			WorkspaceOperations workspaceOperations = CorePlugin.workspaceOperations();
//
//			IProject gradleProject = workspaceOperations.findProjectByLocation(rootDir).orNull();
//
//			if (gradleProject != null) {
//				projectDirectoryExpression = ExpressionUtils.encodeWorkspaceLocation(gradleProject);
//			}
//			else {
//				projectDirectoryExpression = rootDir.getAbsolutePath();
//			}
//
//			File gradleHome = buildConfig.getGradleUserHome();
//
//			String gradleUserHome = gradleHome == null ? "" : gradleHome.getAbsolutePath();
//
//			GradleDistribution gradleDistribution = buildConfig.getGradleDistribution();
//
//			String serializeString = gradleDistribution.toString();
//
//			return new GradleRunConfigurationAttributes(
//				tasks, projectDirectoryExpression, serializeString, gradleUserHome, null, Collections.<String>emptyList(),
//				arguments, true, true, buildConfig.isOverrideWorkspaceSettings(), buildConfig.isOfflineMode(),
//				buildConfig.isBuildScansEnabled());
//		}
// 	
//	@SuppressWarnings("restriction")
//	public static ILaunch runGradleTask(
//			IProject project, String[] tasks, String[] arguments, String launchName, boolean saveConfig,
//			IProgressMonitor monitor)
//		throws CoreException {
//
//		GradleRunConfigurationAttributes runAttributes = _getRunConfigurationAttributes(
//			project, Arrays.asList(tasks), Arrays.asList(arguments));
//
//		final ILaunchConfigurationWorkingCopy launchConfigurationWC;
//
//		if (launchName == null) {
//			GradleLaunchConfigurationManager launchConfigManager = CorePlugin.gradleLaunchConfigurationManager();
//
//			ILaunchConfiguration launchConfiguration = launchConfigManager.getOrCreateRunConfiguration(runAttributes);
//
//			launchConfigurationWC = launchConfiguration.getWorkingCopy();
//		}
//		else {
//			DebugPlugin debugPlugin = DebugPlugin.getDefault();
//
//			ILaunchManager launchManager = debugPlugin.getLaunchManager();
//
//			ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(
//				GradleRunConfigurationDelegate.ID);
//
//			launchConfigurationWC = launchConfigurationType.newInstance(null, launchName);
//
//			runAttributes.apply(launchConfigurationWC);
//
//			if (saveConfig) {
//				launchConfigurationWC.doSave();
//			}
//		}
//
//		launchConfigurationWC.setAttribute("org.eclipse.debug.ui.ATTR_CAPTURE_IN_CONSOLE", Boolean.FALSE);
//		launchConfigurationWC.setAttribute("org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND", Boolean.TRUE);
//		launchConfigurationWC.setAttribute("org.eclipse.debug.ui.ATTR_PRIVATE", Boolean.TRUE);
//		ILaunch dockerLaunch = launchConfigurationWC.launch(ILaunchManager.RUN_MODE, monitor);
//		return dockerLaunch;
//	}

	protected void startContainer(IProgressMonitor monitor) {
		if (_portalServer != null) {
//			_createBladeServerJob(
//				new PortalDockerServerRunnable(getLabel()) {
//
// 					@Override
//					public void doit(IProgressMonitor mon) throws Exception {
//						mon.beginTask("Start server", 10);
//
// 						try {
// 							DockerClient dockerClient = LiferayDockerClient.getDockerClient();
// 							StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(_portalServer.getContainerId());
// 							startContainerCmd.exec();
// 							fireCreateEvent();
//						}
//						catch (Exception e) {
//							fireTerminateEvent();
//							LiferayServerCore.logError(e);
//						}
//					
//						finally {
//							mon.done();
//						}
//					}
// 				});

//			GradleWorkspace workspace = GradleCore.getWorkspace();
//
//			Optional<GradleBuild> gradleBuildOpt = workspace.getBuild(getWorkspaceProject());
//
//			GradleBuild gradleBuild = gradleBuildOpt.get();
//
//			try {
//				gradleBuild.withConnection(
//					connection -> {
//						connection.newBuild(
//						).addArguments(
//							new String[0]
//						).forTasks(
//							"startDockerContainer"
//						).withCancellationToken(
//							GradleConnector.newCancellationTokenSource().token()
//						).run();
//
//						return null;
//					},
//					monitor);
//			}
//			catch (Exception e) {
//				LiferayServerCore.logError(e);
//			}	
//			try {
//				runGradleTask(getWorkspaceProject(),new String[] {"startDockerContainer"},new String[0],"startContainer",false,monitor);
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//			}
			try {
				DockerClient dockerClient = LiferayDockerClient.getDockerClient();
				StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(_portalServer.getContainerId());
				startContainerCmd.exec();
				fireCreateEvent();
			} catch (Exception e) {
				fireTerminateEvent();
				LiferayServerCore.logError(e);
			}

			if (_debug) {
				Thread checkDebugThread = new Thread("Liferay Portal Docker Server Debug Checking Thread") {
					public void run() {
						try {
							boolean debugPortStarted = false;
							String host = _config.getAttribute("hostname", _server.getHost());
							String port = _config.getAttribute("port", "8888");
							do {

								IStatus canConnect = SocketUtil.canConnect(host, port);
								try {
									if (canConnect.isOK()) {
										_delegate.startDebugLaunch(_server, _config, _launch, monitor);

										IDebugTarget[] debugTargets = _launch.getDebugTargets();

										if (ListUtil.isNotEmpty(debugTargets)) {
											debugPortStarted = true;
										}
									}
									sleep(500);
								} catch (Exception e) {
									e.printStackTrace();
								}
							} while (!debugPortStarted);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				checkDebugThread.setPriority(1);
				checkDebugThread.setDaemon(false);
				checkDebugThread.start();

				try {
					checkDebugThread.join(Integer.MAX_VALUE);
					if (checkDebugThread.isAlive()) {
						checkDebugThread.interrupt();
						throw new TimeoutException();
					}
				} catch (TimeoutException e) {
				} catch (InterruptedException e) {
				}
			}
		}
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
	private static final Pattern _workspacePluginPattern = Pattern
			.compile(".*apply.*plugin.*:.*[\'\"]com\\.liferay\\.workspace[\'\"].*", Pattern.MULTILINE | Pattern.DOTALL);

	public static boolean isValidGradleWorkspaceLocation(String location) {
		File workspaceDir = new File(location);

		File buildGradle = new File(workspaceDir, _BUILD_GRADLE_FILE_NAME);
		File settingsGradle = new File(workspaceDir, _SETTINGS_GRADLE_FILE_NAME);
		File gradleProperties = new File(workspaceDir, _GRADLE_PROPERTIES_FILE_NAME);

		if (FileUtil.notExists(buildGradle) || FileUtil.notExists(settingsGradle)
				|| FileUtil.notExists(gradleProperties)) {

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

	private DockerClient _dockerClient;
	private PortalDockerServerLaunchConfigDelegate _delegate;
	private ILaunchConfiguration _config;
	private Map<String, String> _attributerMap = new HashMap<>();
	private String _label;
	private ILaunch _launch;
	private PortalDockerServer _portalServer;
	private IServer _server;
	private boolean _debug;
	private IPortalDockerStreamsProxy _streamsProxy;

}