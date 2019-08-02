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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.wst.server.core.IServer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.util.LiferayDockerClient;

 /**
 * @author Simon Jiang
 */
public class PortalDockerServerMonitorProcess implements IProcess {

 	public PortalDockerServerMonitorProcess(
		IServer server, final PortalDockerServerBehavior serverBehavior, ILaunch launch, boolean debug,
		IPortalDockerStreamsProxy proxy, ILaunchConfiguration config, IProgressMonitor monitor) {

 		_server = server;
		_portalServer = (PortalDockerServer)server.loadAdapter(PortalDockerServer.class, null);
		_serverBehavior = serverBehavior;
		_streamsProxy = proxy;
		_launch = launch;

 		run(monitor);
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
		return (T)null;
	}

 	public String getAttribute(String key) {
		return (String)this._attributerMap.get(key);
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

 			PortalDockerServer portalServer = (PortalDockerServer)_server.loadAdapter(PortalDockerServer.class, null);

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
		return _streamsProxy.isTerminated();
	}

 	public void setAttribute(String key, String value) {
		_attributerMap.put(key, value);
	}

 	public void setStreamsProxy(IPortalDockerStreamsProxy streamsProxy) {
		_streamsProxy = streamsProxy;
	}

 	public void terminate() throws DebugException {
		if (_server != null) {
			_serverBehavior.stop(false);
		}
	}

 	protected void fireCreateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

 	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager = DebugPlugin.getDefault();

 		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[] {event});
		}
	}

 	protected void run(IProgressMonitor monitor) {
		if ((_portalServer != null) && (_job == null)) {
			_createBladeServerJob(
				new PortalDockerServerRunnable(getLabel()) {

 					@Override
					public void doit(IProgressMonitor mon) throws Exception {
						mon.beginTask("Start server", 10);

 						try {
 							DockerClient dockerClient = LiferayDockerClient.getDockerClient();
 							StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(_portalServer.getContainerId());
 							startContainerCmd.exec();
 							fireCreateEvent();
						}
						catch (Exception e) {
							fireTerminateEvent();
							LiferayServerCore.logError(e);
						}
						finally {
							mon.done();
						}
					}
 				});
		}
	}

 	private Job _createBladeServerJob(PortalDockerServerRunnable runable) {
		Job job = runable.asJob();

 		job.setPriority(Job.LONG);
		job.setRule(null);
		job.schedule();

 		return job;
	}

 	private Map<String, String> _attributerMap = new HashMap<>();
	private Job _job;
	private String _label;
	private ILaunch _launch;
	private PortalDockerServer _portalServer;
	private IServer _server;
	private PortalDockerServerBehavior _serverBehavior;
	private IPortalDockerStreamsProxy _streamsProxy;

 } 