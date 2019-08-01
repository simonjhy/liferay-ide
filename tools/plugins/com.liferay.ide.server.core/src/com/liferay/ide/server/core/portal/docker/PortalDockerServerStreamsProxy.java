package com.liferay.ide.server.core.portal.docker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IStreamMonitor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.liferay.ide.server.util.LiferayDockerClient;

public class PortalDockerServerStreamsProxy implements IPortalDockerStreamsProxy {

	public PortalDockerServerStreamsProxy() {
		dockerClient = LiferayDockerClient.getDockerClient();
		dockerLogResponse = new LiferayLogResultCallback(sysOut);
	}
	
 	public IStreamMonitor getErrorStreamMonitor() {
		return sysErr;
	}

 	public IStreamMonitor getOutputStreamMonitor() {
		return sysOut;
	}

 	public boolean isMonitorStopping() {
		return _monitorStopping;
	}

 	public boolean isTerminated() {
		return _done;
	}

 	public void terminate() {
 		dockerLogResponse.kill();
 		initLogContainerCmd.close();
	}

 	public void write(String input) throws IOException {
	}

 	protected void readToNow(BufferedReader br) throws IOException {
		String s = "";

 		while (s != null) {
			s = br.readLine();
		}
	}

 	protected void setIsMonitorStopping(boolean curIsMonitorStopping) {
		_monitorStopping = curIsMonitorStopping;
	}

 	protected final boolean shouldReloadFileReader(long originalFileSize, long newFileSize) {
		boolean reloadFileReader = true;

 		if (originalFileSize <= newFileSize) {
			reloadFileReader = false;
		}

 		return reloadFileReader;
	}

 	protected class LiferayLogResultCallback extends ResultCallbackTemplate<LiferayLogResultCallback, Frame>{
 		
 		private PortalDockerServerOutputStreamMonitor _out;
 		
 		public LiferayLogResultCallback(PortalDockerServerOutputStreamMonitor out) {
 			_out = out;
 			fDone = new AtomicBoolean(false);
 		}
 		
 		private static final int BUFFER_SIZE= 8192;
 		private String fEncoding = "UTF-8";
 		private boolean fKilled= false;
 		
 		
		@Override
		public void onStart(Closeable stream) {
			super.onStart(stream);
			
		}

		private void read(InputStream inputStream) {
			try {
				while(!fKilled) {
					internalRead(inputStream);
				}
			} finally {
				fDone.set(true);
			}
		}
		
		protected void startMonitoring(InputStream inputStream) {
			if (_streamThread == null) {
				fDone.set(false);
				_streamThread = new Thread((Runnable) () -> read(inputStream), "Liferay Portal Docker Server IO Monitor Stream");
				_streamThread.setDaemon(true);
				_streamThread.setPriority(Thread.MIN_PRIORITY);
				_streamThread.start();
			}
		}
		private Thread _streamThread;
		private final AtomicBoolean fDone;
		private long lastSleep;
		
		private void internalRead(InputStream inputStream) {
			lastSleep = System.currentTimeMillis();
			long currentTime = lastSleep;
			char[] chars = new char[BUFFER_SIZE];
			int read = 0;
			try (InputStreamReader reader = (fEncoding == null ? new InputStreamReader(inputStream) : new InputStreamReader(inputStream, fEncoding))) {
				while (read >= 0) {
					try {
						if (fKilled) {
							break;
						}
						read = reader.read(chars);
						if (read > 0) {
							String text = new String(chars, 0, read);
							synchronized (this) {
								_out.append(text +"\n");
							}
						}
					} catch (IOException ioe) {
						if (!fKilled) {
							DebugPlugin.log(ioe);
						}
						return;
					} catch (NullPointerException e) {
						return;
					}

					currentTime = System.currentTimeMillis();
					if (currentTime - lastSleep > 1000) {
						lastSleep = currentTime;
						try {
							// just give up CPU to maintain UI responsiveness.
							Thread.sleep(1);
						} catch (InterruptedException e) {
						}
					}
				}
			} catch (IOException e) {
				DebugPlugin.log(e);
			}
		}
		
		public void kill() {
			fKilled= true;
		}

		@Override
		public void onNext(Frame frame) {
			System.out.println(new String(frame.getPayload() + "\n"));
			startMonitoring(new ByteArrayInputStream(frame.getPayload()));
		}
 	}
 	
 	protected void startMonitoring(String containerId) {
		if (_streamThread != null) {
			return;
		}
		
		
		initLogContainerCmd = dockerClient.logContainerCmd(containerId);
		initLogContainerCmd.withStdOut(true);
		initLogContainerCmd.withStdErr(true);
		initLogContainerCmd.withFollowStream(true);
		initLogContainerCmd.withTailAll();

		initLogContainerCmd.exec(dockerLogResponse);
		try {
			dockerLogResponse.awaitCompletion();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}		
	}

	protected PortalDockerServerOutputStreamMonitor sysErr;
	protected PortalDockerServerOutputStreamMonitor sysOut;

	private boolean _done = false;
	private boolean _monitorStopping = false;
	private Thread _streamThread;
	private LiferayLogResultCallback dockerLogResponse;
	private LogContainerCmd initLogContainerCmd;
	protected DockerClient dockerClient;

 } 
