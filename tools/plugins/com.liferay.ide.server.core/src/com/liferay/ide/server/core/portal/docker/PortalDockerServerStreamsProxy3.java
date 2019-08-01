package com.liferay.ide.server.core.portal.docker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.debug.core.model.IStreamMonitor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.liferay.ide.server.util.LiferayDockerClient;

public class PortalDockerServerStreamsProxy3 implements IPortalDockerStreamsProxy {

	public PortalDockerServerStreamsProxy3() {
		dockerClient = LiferayDockerClient.getDockerClient();
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
		if (_bufferSysOut != null) {
			try {
				_bufferSysOut.close();
			}
			catch (Exception e) {
			}
		}

 		_done = true;
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
 		}
 		
 		private ByteArrayInputStream inputStream;
		@Override
		public void onNext(Frame frame) {
			try {
				inputStream = new ByteArrayInputStream(frame.getPayload());
				_bufferSysOut = new BufferedReader(new InputStreamReader(inputStream));

				String s = _bufferSysOut.readLine();

				if (s != null) {
					_out.append(s +"\n");
				}				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		
		public ByteArrayInputStream getResultInputStream() {
			return inputStream;
		}
 	}
 	
 	protected void startMonitoring(String containerId) {
		if (_streamThread != null) {
			return;
		}
		
//		
//		LogContainerCmd initLogContainerCmd = dockerClient.logContainerCmd(containerId);
//		initLogContainerCmd.withStdOut(true);
//		initLogContainerCmd.withFollowStream(true);
//		initLogContainerCmd.withTail(5);
//		LiferayLogResultCallback liferayLogResultCallback = new LiferayLogResultCallback(sysOut);
//		initLogContainerCmd.exec(liferayLogResultCallback);
//		try {
//			liferayLogResultCallback.awaitCompletion();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
		
		_streamThread = new Thread("Liferay Portal Docker Server IO Monitor Stream") {

			public void run() {
				boolean sysOutInitialized = false;
				boolean sysOutEmpty = false;
//				long originalDockerLogSize = -1; 

				while (!_done) {
					try {
						sleep(500L);
					}
					catch (Exception localException2) {
					}

					try {
						String s = "";

//						while (s!= null && !_done) {
							LogContainerCmd initLogContainerCmd = dockerClient.logContainerCmd(containerId);
							initLogContainerCmd.withStdOut(true);
							initLogContainerCmd.withTail(1);
							
				 			LiferayLogResultCallback dockerLogResponse = new LiferayLogResultCallback(sysOut);

				 			initLogContainerCmd.exec(dockerLogResponse);
				 			dockerLogResponse.awaitCompletion();
				 			initLogContainerCmd.close();
//				 			
//							long newFpSysOutSize = dockerLogResponse.getResultInputStream().available();;
//
//							if (originalDockerLogSize != newFpSysOutSize) {
//								if (_bufferSysOut != null) {
//									_bufferSysOut.close();
//								}
//
//								_bufferSysOut = new BufferedReader(new InputStreamReader(dockerLogResponse.getResultInputStream()));
//							}
//
//							originalDockerLogSize = newFpSysOutSize;
//
//							s = _bufferSysOut.readLine();
//
//							if (s != null) {
//								sysOut.append(s +"\n");
//							}
//						}
					}
					catch (Exception e) {
					}
				}

				_streamThread = null;
			}

		};

		_streamThread.setPriority(1);
		_streamThread.setDaemon(true);
		_streamThread.start();		
	}

	protected PortalDockerServerOutputStreamMonitor sysErr;
	protected PortalDockerServerOutputStreamMonitor sysOut;

	private BufferedReader _bufferSysOut = null;
	private boolean _done = false;
	private boolean _monitorStopping = false;
	private Thread _streamThread;
	
	
	protected DockerClient dockerClient;

 } 
