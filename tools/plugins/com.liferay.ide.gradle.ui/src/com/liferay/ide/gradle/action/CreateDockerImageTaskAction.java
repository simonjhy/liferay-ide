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

package com.liferay.ide.gradle.action;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.ResponseItem.ProgressDetail;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.gradle.ui.LiferayGradleUI;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.server.util.LiferayDockerClient;

import java.io.Closeable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.Sets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Terry Jia
 * @author Charles Wu
 * @author Simon Jiang
 */
public class CreateDockerImageTaskAction extends GradleTaskAction {

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);

		action.setEnabled(LiferayWorkspaceUtil.isValidWorkspace(project));
	}

	private CloseableHttpAsyncClient _defaultAsyncClient;
	
	private CloseableHttpAsyncClient createDefaultAsyncClient() {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(50000).setSocketTimeout(50000)
				.setConnectionRequestTimeout(1000).build();

		// configure IO
		IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(Runtime.getRuntime().availableProcessors())
				.setSoKeepAlive(true).build();

		// configure thread pool
		ConnectingIOReactor ioReactor = null;
		try {
			ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
		} catch (IOReactorException e) {
			e.printStackTrace();
		}
		PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
		connManager.setMaxTotal(100);
		connManager.setDefaultMaxPerRoute(100);

		return HttpAsyncClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(requestConfig)
				.build();
	}
	public void processDockerRequest(String request) {
		try {
			HttpGet httpGet = new HttpGet(request);
			_defaultAsyncClient.execute(httpGet, new DockerRemoteResponseCallback());
		} catch (Exception e) {
			LiferayGradleUI.logError("Failed to http response from maven central", e);
		}
	}

	public class LiferayDockerImage {
		private String name;
		private int size;
		private String architecture;
		private String os;
		private String downloadStatus;
		private boolean downloading = false;
		private boolean installed = false;

		public boolean isInstalled() {
			return installed;
		}

		public void setInstalled(boolean installed) {
			this.installed = installed;
		}

		public boolean isDownloading() {
			return downloading;
		}

		public void setDownloading(boolean downloading) {
			this.downloading = downloading;
		}

		public String getDownloadStatus() {
			return downloadStatus;
		}

		public void setDownloadStatus(String downloadStatus) {
			this.downloadStatus = downloadStatus;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public String getArchitecture() {
			return architecture;
		}

		public void setArchitecture(String architecture) {
			this.architecture = architecture;
		}

		public String getOs() {
			return os;
		}

		public void setOs(String os) {
			this.os = os;
		}

		public String getLastUpdated() {
			return lastUpdated;
		}

		public void setLastUpdated(String lastUpdated) {
			this.lastUpdated = lastUpdated;
		}

		private String lastUpdated;
	}

	private Object _getJSONResponse(String response) {
		Object retval = null;

		try {
			retval = new JSONObject(response);
		} catch (JSONException e) {
			try {
				retval = new JSONArray(response);
			} catch (JSONException e1) {
			}
		}

		return retval;
	}
	private Set<LiferayDockerImage> parseDockerJsonResponse(Object resultsObject) {
		Set<LiferayDockerImage> dockerImages = new CopyOnWriteArraySet<>();
		try {
			if (resultsObject != null) {
				if ((resultsObject != null) && resultsObject instanceof JSONArray) {
					JSONArray results = (JSONArray) resultsObject;
					for (int i = 0; i < results.length(); i++) {
						LiferayDockerImage dockerImage = new LiferayDockerImage();

						JSONObject result = (JSONObject) results.get(i);
						String imageName = result.getString("name");

						if (!imageName.startsWith("7")) {
							continue;
						}
						int imageSize = Integer.parseInt(result.getString("full_size"));
						dockerImage.setName(result.getString("name"));
						dockerImage.setSize(imageSize);
						String udpateDate = result.getString("last_updated");
						if (udpateDate != null) {
							dockerImage.setLastUpdated(udpateDate.split("T")[0]);
						}

						Object imagesObject = result.get("images");

						if ((imagesObject != null) && imagesObject instanceof JSONArray) {
							JSONArray images = (JSONArray) imagesObject;

							for (int j = 0; j < images.length(); j++) {
								JSONObject image = (JSONObject) images.get(j);
								dockerImage.setOs(image.getString("os"));
								dockerImage.setArchitecture(image.getString("architecture"));
							}
						}
						dockerImages.add(dockerImage);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dockerImages;
	}
	
	private LiferayDockerImage[] _extractLiferayDockerResponse(String responseString) {
		Set<LiferayDockerImage> dockerImages = new CopyOnWriteArraySet<>();
		try {
			Object responseObject = _getJSONResponse(responseString);

			if (responseObject instanceof JSONObject) {
				JSONObject jsonResult = (JSONObject) responseObject;
				String nextPage = jsonResult.getString("next");

				dockerImages.addAll(parseDockerJsonResponse(jsonResult.get("results")));

				if (nextPage != null && CoreUtil.isNotNullOrEmpty(nextPage) && !nextPage.equals("null")) {
					processDockerRequest(nextPage);
				} else {
					_defaultAsyncClient.close();
				}
			}
		} catch (Exception e) {
			LiferayGradleUI.logError("Failed to get latest JSF archtype version", e);
		}

		return dockerImages.toArray(new LiferayDockerImage[dockerImages.size()]);
	}
	
	public class DockerRemoteResponseCallback implements FutureCallback<HttpResponse> {
		@Override
		public void cancelled() {
		}

		@Override
		public void completed(HttpResponse response) {
			StringBuilder retVal = new StringBuilder();
			try {
				StatusLine statusLine = response.getStatusLine();

				int statusCode = statusLine.getStatusCode();

				if (statusCode == HttpStatus.SC_OK) {
					HttpEntity entity = response.getEntity();

					String body = CoreUtil.readStreamToString(entity.getContent(), false);

					EntityUtils.consume(entity);

					retVal.append(body);

					LiferayDockerImage[] images = _extractLiferayDockerResponse(retVal.toString());
					String dockerRepoTag = LiferayWorkspaceUtil.getGradleProperty(project.getLocation().toOSString(), "liferay.workspace.docker.image.liferay", null);
					String[] tags = dockerRepoTag.split(":");
					
					for(LiferayDockerImage image : images) {
						if (image.getName().equals(tags[1])) {
							Job buildDockerJob = new Job("Build Docker Image Job") {

								private BuildImageResultCallback _buildImageResultCallback;
								private BuildImageCmd _buildImageCmd;
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									try {
										DockerClient dockerClient = LiferayDockerClient.getDockerClient();
										_buildImageCmd = dockerClient.buildImageCmd();
										_buildImageCmd.withDockerfile(project.getLocation().append("build/docker").append("Dockerfile").toFile());
										_buildImageCmd.withRemove(true);
										_buildImageCmd
												.withTag("liferay/portal:" + tags[1] /* + "-" + project.getName() */);
										_buildImageCmd.withNoCache(true);
										_buildImageCmd.withPull(true);
										_buildImageResultCallback = new LiferayBuildImageCallback(monitor, image);
										_buildImageCmd.exec(_buildImageResultCallback);
										_buildImageResultCallback.awaitCompletion();						
									}
									catch(Exception e) {
										e.printStackTrace();
									}
									return Status.OK_STATUS;
								}
								
								@Override
								protected void canceling() {
									try {
										_buildImageCmd.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}

								
							};
							buildDockerJob.setUser(true);
							buildDockerJob.schedule();		
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Exception arg0) {
		}

	}
	
	@Override
	protected void afterAction() {
	}
	
	
	@Override
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			final List<String> gradleTasks = getGradleTasks();

			if (ListUtil.isEmpty(gradleTasks)) {
				return;
			}
//			_defaultAsyncClient = createDefaultAsyncClient();
//			_defaultAsyncClient.start();
//			HttpGet httpGet = new HttpGet("https://registry.hub.docker.com/v2/repositories/liferay/portal/tags/");
//			_defaultAsyncClient.execute(httpGet, new DockerRemoteResponseCallback());

			
				
			Job dockerJob = _createBladeServerJob(new PortalDockerServerRunnable("Build Docker Image") {

				@Override
				public void doit(IProgressMonitor mon) throws Exception {
					try {
						DockerClient dockerClient = LiferayDockerClient.getDockerClient();
						BuildImageCmd buildImageCmd = dockerClient.buildImageCmd();
						buildImageCmd.withDockerfile(project.getLocation().append("build/docker").append("Dockerfile").toFile());
						buildImageCmd.withRemove(true);
						buildImageCmd.withForcerm(true);
						buildImageCmd.withTags(Sets.newHashSet("liferay/portal:7.2.0-ga1" + "-" + project.getName()));
						buildImageCmd.withNoCache(false);
						buildImageCmd.withPull(true);
						buildImageCmd.withQuiet(false);
						LiferayBuildImageCallbackTest buildImageResultCallback = new LiferayBuildImageCallbackTest(mon);
						buildImageCmd.exec(buildImageResultCallback);
						buildImageResultCallback.awaitCompletion();	
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			});
			dockerJob.schedule();
		}
	}
	
 	private Job _createBladeServerJob(PortalDockerServerRunnable runable) {
		Job job = runable.asJob();

 		job.setPriority(Job.LONG);
		job.setRule(null);
		job.schedule();

 		return job;
	}
	
	private class LiferayBuildImageCallbackTest extends BuildImageResultCallback{
		private IProgressMonitor _monitor;
		private final Logger LOGGER = LoggerFactory.getLogger(LiferayBuildImageCallbackTest.class);
		public LiferayBuildImageCallbackTest(IProgressMonitor monitor) {
			_monitor =   monitor;
		}
		
	    @Override
	    public void onStart(Closeable stream) {
	    	super.onStart(stream);
	    	
	    }
	    
	    private int total = 10;
	    public void onNext(BuildResponseItem item) {
	    	if (item.getStream()!=null && CoreUtil.isNotNullOrEmpty(item.getStream()) && !item.getStream().equals("null")) {
		    	SubMonitor convert = SubMonitor.convert(_monitor, item.getStream(), 100);
		    	convert.setTaskName(item.getStream());
		    	convert.worked(total);
		    	total = total +10;	
	    	}

	    }
		
	    @Override
	    public void onComplete() {
	       super.onComplete();
	       _monitor.done();
	    }
	}
 	
	private class LiferayBuildImageCallback extends BuildImageResultCallback{
		private ConcurrentMap<String, Long> responseMap;
		private NumberFormat _nf;
		private IProgressMonitor _monitor;
		private LiferayDockerImage _liferayImage;
		
		public LiferayBuildImageCallback(IProgressMonitor monitor, LiferayDockerImage liferayImage) {
			_monitor = monitor;
			responseMap = new ConcurrentHashMap<String, Long>();
			_nf = NumberFormat.getInstance();
			_nf.setMinimumFractionDigits(0);
			_liferayImage = liferayImage;
		}
		
	    @Override
	    public void onStart(Closeable stream) {
	    	super.onStart(stream);
	    	_monitor.beginTask("Start to build docker image", new Double(_liferayImage.getSize()/(1000*1000)).intValue());
	    }
		
	    private Long totalFinishedSize = 0L;
	    
	    @Override
	    public void onNext(BuildResponseItem item) {
	    	if ( item == null || item.getStatus() == null) {
	    		return;
	    	}
	    	if (item.getStatus().equals("Downloading")) {
	    		ProgressDetail progressDetail = item.getProgressDetail();
		    	if ( progressDetail != null ) {
		    		
		    		Long incremnetSize = 0L;
		    		
	    			Long lastSize = responseMap.get(item.getId());
	    			Long currentSize = progressDetail.getCurrent()/(1000*1000);
	    			
	    			if ( lastSize == null) {
	    				incremnetSize = currentSize;
	    			}
	    			else {
	    				incremnetSize = currentSize - lastSize;
	    			}
		    		
	    			totalFinishedSize = totalFinishedSize + incremnetSize;
		    		responseMap.put(item.getId(),currentSize);
					
	    			_monitor.worked(incremnetSize.intValue());

		    	}	    		
	    	}

	    }
	    
	    @Override
	    public void onComplete() {
	       super.onComplete();
	       Long remainWorked = new Long((_liferayImage.getSize()/(1000*1000)) - totalFinishedSize);
	       _monitor.worked(remainWorked.intValue());
	       _monitor.done();
	    }
	}

	protected List<String> getGradleTasks() {
		GradleProject gradleProject = getGradleProjectModel();

		if (gradleProject == null) {
			return Collections.emptyList();
		}

		List<GradleTask> gradleTasks = new ArrayList<>();

		fetchModelTasks(gradleProject, "pullDockerImage", gradleTasks);

		Stream<GradleTask> gradleTaskStream = gradleTasks.stream();

		return gradleTaskStream.map(
			task -> task.getPath()
		).collect(
			Collectors.toList()
		);
	}
	
	@Override
	protected void beforeAction() {
//		try {
//			GradleUtil.runGradleTask(project,"createDockerFile", null);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
	}

	@Override
	protected String getGradleTaskName() {
		return "initBundle";
	}

}