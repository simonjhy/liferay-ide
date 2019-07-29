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

package com.liferay.ide.server.ui.portal.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.stream.Stream;

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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem.ProgressDetail;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.collect.Lists;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.server.core.portal.docker.PortalDockerRuntime;
import com.liferay.ide.server.ui.LiferayServerUI;
import com.liferay.ide.server.util.LiferayDockerClient;
import com.liferay.ide.ui.util.UIUtil;

/**
 * @author Simon Jiang
 */
@SuppressWarnings("restriction")
public class DockerRuntimeBrowseComposite extends Composite {
	protected static final ISelection LiferayDockerImage = null;
	
	private static int _sizeMB = (1000*1000);

	public DockerRuntimeBrowseComposite(Composite parent, IWizardHandle wizard) {
		super(parent, SWT.NONE);

		_wizard = wizard;

		wizard.setTitle("Browse Liferay Portal Docker");
		wizard.setDescription("Browse Liferay Portal Docker");
		wizard.setImageDescriptor(LiferayServerUI.getImageDescriptor(LiferayServerUI.IMG_WIZ_RUNTIME));

		initInstalledImages();
		createControl(this);
	}

	private DockerClient _dockerClient = null;

	@Override
	public void dispose() {
		try {
			if (_defaultAsyncClient != null) {
				_defaultAsyncClient.close();
			}

			if (_dockerClient != null) {
				_dockerClient.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.dispose();
	}

	private Set<com.github.dockerjava.api.model.Image> installedImages = null;

	private Job initInstalledImages() {
		Job initInstalledImages = new Job("") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					_dockerClient = LiferayDockerClient.getDockerClient();

					ListImagesCmd listImagesCmd = _dockerClient.listImagesCmd();
					listImagesCmd.withShowAll(true);
					installedImages = new CopyOnWriteArraySet<>(listImagesCmd.exec());

					if (!_defaultAsyncClient.isRunning()) {
						_tableViewer.refresh();
					}
				} catch (Exception e) {
				}
				return Status.OK_STATUS;
			}

		};
		initInstalledImages.schedule();

		return initInstalledImages;
	}

	public void setRuntime(IRuntimeWorkingCopy newRuntime) {
		if (newRuntime == null) {
			_runtimeWC = null;
		} else {
			_runtimeWC = newRuntime;
		}
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
						dockerImage.setDownloadStatus("0MB/" + imageSize / _sizeMB + "MB");
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

					UIUtil.async(() -> {
						if (_tableViewer == null) {
							return;
						}

						Table upgradePlanTable = _tableViewer.getTable();

						if (upgradePlanTable.isDisposed()) {
							return;
						}

						Object oldInput = _tableViewer.getInput();

						ArrayList<LiferayDockerImage> oldList = null;
						if (oldInput != null && oldInput instanceof LiferayDockerImage[]) {
							oldList = Lists.newArrayList((LiferayDockerImage[]) oldInput);
						}

						ArrayList<LiferayDockerImage> newList = Lists.newArrayList((LiferayDockerImage[]) images);

						if (oldList != null) {
							newList.addAll(oldList);
						}

						_tableViewer.setInput(newList.toArray(new LiferayDockerImage[newList.size()]));

						Stream.of(upgradePlanTable.getColumns()).forEach(obj -> obj.pack());
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Exception arg0) {
		}

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
					Job reInitInstalledImagesJob = initInstalledImages();
					reInitInstalledImagesJob.join();
					_tableViewer.refresh();
				}
			}
		} catch (Exception e) {
			LiferayServerUI.logError("Failed to get latest JSF archtype version", e);
		}

		return dockerImages.toArray(new LiferayDockerImage[dockerImages.size()]);
	}

	private CloseableHttpAsyncClient _defaultAsyncClient;

	public void processDockerRequest(String request) {
		try {
			HttpGet httpGet = new HttpGet(request);
			_defaultAsyncClient.execute(httpGet, new DockerRemoteResponseCallback());
		} catch (Exception e) {
			LiferayServerUI.logError("Failed to http response from maven central", e);
		}
	}

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

	private void _createFields(Composite parent) {

		createLabel("Liferay Portal Docker Remote Repository");
		_liferayDockerRepositoryComb = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);

		_liferayDockerRepositoryComb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		_liferayDockerRepositoryComb.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (_defaultAsyncClient == null) {
					_defaultAsyncClient = createDefaultAsyncClient();
				}
				_defaultAsyncClient.start();
				processDockerRequest(_liferayDockerRepositoryComb.getText());
			}

		});

		_liferayDockerRepositoryComb
				.setItems(new String[] { "https://registry.hub.docker.com/v2/repositories/liferay/portal/tags/" });
		_liferayDockerRepositoryComb.select(0);
		_liferayDockerRepositoryComb.setEnabled(false);

	}

	private TableViewer _tableViewer;

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

	private class LiferayDockerRuntimeContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof LiferayDockerImage[]) {
				return (LiferayDockerImage[]) inputElement;
			}

			return new Object[] { inputElement };
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private static void _createTableColumn(TableViewer tableViewer, String name, int width,
			Function<Object, Image> imageProvider, Function<Object, String> textProvider) {

		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);

		TableColumn tableColumn = tableViewerColumn.getColumn();

		tableColumn.setText(name);

		if (width > -1) {
			tableColumn.setWidth(width);
		}

		tableColumn.pack();

		tableViewerColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public Image getImage(Object element) {
				if (imageProvider == null) {
					return null;
				}

				return imageProvider.apply(element);
			}

			@Override
			public String getText(Object element) {
				if (textProvider == null) {
					return null;
				}

				return textProvider.apply(element);
			}

			public void update(ViewerCell cell) {
				super.update(cell);
				LiferayDockerImage dockerImage = (LiferayDockerImage)cell.getElement();
				TableItem item = (TableItem)cell.getItem();

				Control control = tableViewer.getControl();

				if (dockerImage.isDownloading()) {
					item.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));	
				}
				else {
					item.setBackground(tableViewer.getControl().getBackground());	
				}
				
				if (dockerImage.isInstalled()) {
					item.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
				}
			}			
		});
	}

	private class NewRowAction extends Action {
		public NewRowAction() {
			super("Install this docker image");
		}

		public void run() {
			ISelection selection = _tableViewer.getSelection();
			PullImageCmd pullImageCmd = _dockerClient.pullImageCmd("liferay/portal");
			pullImageCmd.withAuthConfig(_dockerClient.authConfig());

			if (selection instanceof StructuredSelection) {
				StructuredSelection structuredSelection = (StructuredSelection) selection;

				LiferayDockerImage dockerImage = (LiferayDockerImage) structuredSelection.getFirstElement();

				pullImageCmd.withTag(dockerImage.getName());

				PullImageResultCallback res = new LiferayPullImageCallback(dockerImage, element ->  {
					LiferayDockerImage _image = (LiferayDockerImage) element;

					UIUtil.async(() -> {
						if (_tableViewer != null && !_tableViewer.getTable().isDisposed()) {
							_tableViewer.refresh(_image);
						}
					});
					
					return null;
				});

				pullImageCmd.exec(res);
				
				dockerImage.setDownloading(true);
				
				_tableViewer.refresh(dockerImage);
				
			}
		}
	}

	private class LiferayPullImageCallback extends PullImageResultCallback {

		private LiferayDockerImage _image = null;

		private Function<LiferayDockerImage, TableViewer> _response;
		
		private Map<String, Long[]> responseMap;
		
		public LiferayPullImageCallback(LiferayDockerImage dockerImage, Function<LiferayDockerImage, TableViewer> response) {
			_image = dockerImage;
			_response = response;
			responseMap = new HashMap<String, Long[]>();
		}

		@Override
		public void onNext(PullResponseItem item) {
			super.onNext(item);

			if (item.getStatus().equals("Downloading")) {

				ProgressDetail progressDetail = item.getProgressDetail();

				if (progressDetail != null) {
					responseMap.put(item.getId(),
							new Long[] { progressDetail.getCurrent(), progressDetail.getTotal() });
				}
				
				Collection<Long[]> values = responseMap.values();
				Long finishedSize = 0L;
				Long tottalSize = 0L;
				for (Long[] value : values) {
					finishedSize = finishedSize + value[0];
					tottalSize = tottalSize + value[1];
				}

				_image.setDownloadStatus(
						new String(finishedSize / _sizeMB + "MB/" + _image.getSize() / _sizeMB + "MB"));

				_image.setDownloading(true);
				
				_response.apply(_image);				
			}
		}

		@Override
		public void onComplete() {
			try {
				Job initInstalledImages = initInstalledImages();
				initInstalledImages.join();

				_image.setDownloadStatus(
						new String(_image.getSize() / _sizeMB + "MB/" + _image.getSize() / _sizeMB + "MB"));
				
				_image.setDownloading(false);
				
				_response.apply(_image);
				
				responseMap.clear();

			} catch (Exception e) {
				e.printStackTrace();
			}

			super.onComplete();
		}
	}

	private void _createDockerTable(Composite composite) {
//		final Composite composite = SWTUtil.createComposite(this, 2, 2, GridData.FILL_BOTH);

		final Table table = new Table(composite, SWT.FULL_SELECTION);

		final GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);

		tableData.grabExcessVerticalSpace = true;
		tableData.grabExcessHorizontalSpace = true;
		tableData.horizontalAlignment = SWT.FILL;

		tableData.heightHint = 225;
		tableData.widthHint = 550;

		table.setLayoutData(tableData);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		MenuManager popManager = new MenuManager();
		IAction menuAction = new NewRowAction();
		popManager.add(menuAction);
		Menu menu = popManager.createContextMenu(table);
		table.setMenu(menu);

		_tableViewer = new TableViewer(table);

		_tableViewer.setContentProvider(new LiferayDockerRuntimeContentProvider());

		_tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent selectionEvent) {
				IStructuredSelection structuredSelection = selectionEvent.getStructuredSelection();

				Object dockerImage = structuredSelection.getFirstElement();

				LiferayDockerImage targetImage = (LiferayDockerImage) dockerImage;

				IWizardPage nextPage = ((IWizardPage) _wizard).getNextPage();

				TaskWizard setingWizrd = (TaskWizard) (nextPage.getWizard());

				setingWizrd.getTaskModel().putObject("Docker-Image", targetImage);
			}

		});

		_createTableColumn(_tableViewer, "Installed", -1, element -> {
			LiferayDockerImage dockerImage = (LiferayDockerImage) element;

			if (installedImages != null) {
				for (com.github.dockerjava.api.model.Image iterImage : installedImages) {
					String imageName = Arrays.toString(iterImage.getRepoTags());
					if (imageName.contains(dockerImage.getName())) {
						dockerImage.setInstalled(true);
						return LiferayServerUI.getImage(LiferayServerUI.CHECKED_IMAGE_ID);
					}
				}
			}

			return LiferayServerUI.getImage(LiferayServerUI.UNCHECKED_IMAGE_ID);
		}, element -> null);

		_createTableColumn(_tableViewer, "Name", 50, null, element -> {
			LiferayDockerImage image = (LiferayDockerImage) element;

			return image.getName();
		});

		_createTableColumn(_tableViewer, "Image Size", 50, null, element -> {
			LiferayDockerImage image = (LiferayDockerImage) element;

			return String.valueOf(image.getSize() / _sizeMB) + "MB";
		});

		_createTableColumn(_tableViewer, "Architecture", 50, null, element -> {
			LiferayDockerImage image = (LiferayDockerImage) element;

			return image.getArchitecture();
		});

		_createTableColumn(_tableViewer, "Os", 50, null, element -> {
			LiferayDockerImage image = (LiferayDockerImage) element;

			return image.getOs();
		});

		_createTableColumn(_tableViewer, "Last Update Date", 50, null, element -> {
			LiferayDockerImage image = (LiferayDockerImage) element;

			return image.getLastUpdated();
		});

		_createTableColumn(_tableViewer, "Download Status", 50, null, element -> {
			LiferayDockerImage dockerImage = (LiferayDockerImage) element;

			if (installedImages != null) {
				for (com.github.dockerjava.api.model.Image iterImage : installedImages) {
					String imageName = Arrays.toString(iterImage.getRepoTags());
					if (imageName.contains(dockerImage.getName())) {
						return iterImage.getSize() / _sizeMB + "MB/" + iterImage.getSize() / _sizeMB + "MB";
					}
				}
			}

			return dockerImage.getDownloadStatus();
		});
	}

	private Combo _liferayDockerRepositoryComb;

	protected void createControl(final Composite parent) {
		setLayout(createLayout());
		setLayoutData(_createLayoutData());
		setBackground(parent.getBackground());
		_createFields(parent);
		_createDockerTable(parent);
		Dialog.applyDialogFont(this);
	}

	protected Label createLabel(String text) {
		Label label = new Label(this, SWT.NONE);

		label.setText(text);

		GridDataFactory.generate(label, 1, 1);

		return label;
	}

	protected Layout createLayout() {
		return new GridLayout(1, false);
	}

	protected PortalDockerRuntime getPortalDockerRuntime() {
		return (PortalDockerRuntime) getRuntime().loadAdapter(PortalDockerRuntime.class, null);
	}

	protected IRuntimeWorkingCopy getRuntime() {
		return _runtimeWC;
	}

	protected void validate() {
		
		if ( ListUtil.isEmpty(installedImages)) {
			_wizard.setMessage("No any installed docker image.", IMessageProvider.ERROR);
		}
		IStatus status = _runtimeWC.validate(null);

		if ((status == null) || status.isOK()) {
			_wizard.setMessage(null, IMessageProvider.NONE);
		} else if (status.getSeverity() == IStatus.WARNING) {
			_wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
		} else {
			_wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
		}

		_wizard.update();
	}

	private GridData _createLayoutData() {
		return new GridData(GridData.FILL_BOTH);
	}

	private IRuntimeWorkingCopy _runtimeWC;
	private final IWizardHandle _wizard;

}