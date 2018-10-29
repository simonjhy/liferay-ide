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

package com.liferay.ide.server.core.portal;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;

/**
 * @author Simon Jiang
 */
public class BladeServerStateStopThread {

	/**
	 * Create a new PingThread.
	 *
	 * @param server
	 * @param url
	 * @param maxPings  the maximum number of times to try pinging, or -1 to
	 *                  continue forever
	 * @param behaviour
	 */
	public BladeServerStateStopThread(IServer server, PortalServerBehavior behaviour) {
		_server = server;
		_behaviour = behaviour;
		_mointorProcess = behaviour.getProcess();

		int serverStopTimeout = server.getStopTimeout();

		if (serverStopTimeout < (_defaultTimeout / 1000)) {
			_timeout = _defaultTimeout;
		}
		else {
			_timeout = serverStopTimeout * 1000;
		}

		Thread t = new Thread("Liferay Blade Server Stop Thread") {

			public void run() {
				_startedTime = System.currentTimeMillis();
				startMonitor();
			}

		};

		t.setDaemon(true);
		t.start();
	}

	public IProcess getMonitorProcess() {
		return _mointorProcess;
	}

	/**
	 * Tell the pinging to stop.
	 */
	public void stop() {
		_stop = true;
	}

	/**
	 * Ping the server until it is started. Then set the server state to
	 * STATE_STARTED.
	 */
	protected void startMonitor() {
		long currentTime = 0;

		try {
			Thread.sleep(_pingDelay);
		}
		catch (Exception e) {
		}
		while (!_stop) {
			try {
				currentTime = System.currentTimeMillis();

				if ((currentTime - _startedTime) > _timeout) {
					try {
						_server.stop(true);
						((IBladeServerStartStreamsProxy)_mointorProcess.getStreamsProxy()).terminate();
						_behaviour.triggerCleanupEvent(_mointorProcess);
						_stop = true;
					}
					catch (Exception e) {
					}

					break;
				}

				Thread.sleep(1000);

				if (_server.getServerState() == IServer.STATE_STOPPED) {
					Thread.sleep(200);
					((IBladeServerStartStreamsProxy)_mointorProcess.getStreamsProxy()).terminate();
					_behaviour.triggerCleanupEvent(_mointorProcess);
					_stop = true;
				}
			}
			catch (Exception e) {
				if (!_stop) {
					try {
						Thread.sleep(_pingInterval);
					}
					catch (InterruptedException ie) {
					}
				}
			}
		}
	}

	private static int _pingDelay = 2000;
	private static int _pingInterval = 250;

	private PortalServerBehavior _behaviour;
	private long _defaultTimeout = 2 * 60 * 1000;
	private IProcess _mointorProcess;
	private IServer _server;
	private long _startedTime;
	private boolean _stop = false;
	private long _timeout = 0;

}