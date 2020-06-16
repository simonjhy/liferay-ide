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

package com.liferay.ide.core;

import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.core.workspace.ProjectChangeListener;
import com.liferay.portal.tools.bundle.support.commands.DownloadCommand;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Gregory Amerson
 */
public class LiferayCore extends Plugin {

	public static final IPath GLOBAL_SETTINGS_PATH = new Path(System.getProperty("user.home", "") + "/.liferay-ide");

	public static final IPath GLOBAL_USER_DIR = GLOBAL_SETTINGS_PATH.append("bundles");

	public static final String LIFERAY_JOB_FAMILY = "com.liferay.ide.jobs";

	public static final String PLUGIN_ID = "com.liferay.ide.core";

	public static synchronized <T extends ILiferayProject> T create(Class<T> type, Object adaptable) {
		if (type == null) {
			throw new IllegalArgumentException("type can not be null");
		}

		if (adaptable == null) {
			throw new IllegalArgumentException("adaptable can not be null");
		}

		T retval = null;

		ILiferayProject liferayProject = _checkProjectCache(type, adaptable);

		if (liferayProject == null) {
			liferayProject = _createInternal(type, adaptable);
		}

		if (liferayProject != null) {
			if (liferayProject instanceof EventListener) {
				ListenerRegistry listenerRegistry = listenerRegistry();

				listenerRegistry.addEventListener((EventListener)liferayProject);

				_putProjectCache(type, adaptable, liferayProject);
			}

			retval = type.cast(liferayProject);
		}

		return retval;
	}

	public static IStatus createErrorStatus(Exception e) {
		return createErrorStatus(PLUGIN_ID, e);
	}

	public static IStatus createErrorStatus(String msg) {
		return createErrorStatus(PLUGIN_ID, msg);
	}

	public static IStatus createErrorStatus(String pluginId, String msg) {
		return new Status(IStatus.ERROR, pluginId, msg);
	}

	public static IStatus createErrorStatus(String pluginId, String msg, Throwable e) {
		return new Status(IStatus.ERROR, pluginId, msg, e);
	}

	public static IStatus createErrorStatus(String pluginId, Throwable t) {
		return new Status(IStatus.ERROR, pluginId, t.getMessage(), t);
	}

	public static IStatus createInfoStatus(String msg) {
		return createInfoStatus(PLUGIN_ID, msg);
	}

	public static IStatus createInfoStatus(String pluginId, String msg) {
		return new Status(IStatus.INFO, pluginId, msg);
	}

	public static IStatus createWarningStatus(String message) {
		return new Status(IStatus.WARNING, PLUGIN_ID, message);
	}

	public static IStatus createWarningStatus(String message, String id) {
		return new Status(IStatus.WARNING, id, message);
	}

	public static IStatus createWarningStatus(String message, String id, Exception e) {
		return new Status(IStatus.WARNING, id, message, e);
	}

	public static LiferayCore getDefault() {
		return _plugin;
	}

	public static IPath getDefaultStateLocation() {
		return getDefault().getStateLocation();
	}

	public static synchronized ILiferayProjectImporter getImporter(String buildType) {
		if (_importerReader == null) {
			_importerReader = new LiferayProjectImporterReader();
		}

		return _importerReader.getImporter(buildType);
	}

	public static synchronized ILiferayProjectImporter[] getImporters() {
		if (_importerReader == null) {
			_importerReader = new LiferayProjectImporterReader();
		}

		return _importerReader.getImporters();
	}

	public static synchronized ILiferayProjectAdapter[] getProjectAdapters() {
		List<ILiferayProjectAdapter> extensions = _adapterReader.getExtensions();

		return extensions.toArray(new ILiferayProjectAdapter[0]);
	}

	public static synchronized ILiferayProjectProvider getProvider(String shortName) {
		for (ILiferayProjectProvider provider : getProviders()) {
			String name = provider.getShortName();

			if (name.equals(shortName)) {
				return provider;
			}
		}

		return null;
	}

	public static synchronized ILiferayProjectProvider[] getProviders() {
		if (_providerReader == null) {
			_providerReader = new LiferayProjectProviderReader();
		}

		return _providerReader.getProviders();
	}

	public static synchronized ILiferayProjectProvider[] getProviders(Class<?> type) {
		if (_providerReader == null) {
			_providerReader = new LiferayProjectProviderReader();
		}

		return _providerReader.getProviders(type);
	}

	public static synchronized ILiferayProjectProvider[] getProviders(String projectType) {
		if (_providerReader == null) {
			_providerReader = new LiferayProjectProviderReader();
		}

		return _providerReader.getProviders(projectType);
	}

	public static IProxyService getProxyService() {
		Bundle bundle = getDefault().getBundle();

		ServiceTracker<Object, Object> proxyTracker = new ServiceTracker<>(
			bundle.getBundleContext(), IProxyService.class.getName(), null);

		proxyTracker.open();

		IProxyService proxyService = (IProxyService)proxyTracker.getService();

		proxyTracker.close();

		return proxyService;
	}

	public static ListenerRegistry listenerRegistry() {
		return getDefault()._listenerRegistryServiceTracker.getService();
	}

	public static void logError(IStatus status) {
		ILog log = getDefault().getLog();

		log.log(status);
	}

	public static void logError(String msg) {
		logError(createErrorStatus(msg));
	}

	public static void logError(String msg, Throwable t) {
		ILog log = getDefault().getLog();

		log.log(createErrorStatus(PLUGIN_ID, msg, t));
	}

	public static void logError(Throwable t) {
		ILog log = getDefault().getLog();

		log.log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
	}

	public static void logInfo(String msg) {
		logError(createInfoStatus(msg));
	}

	public static void logWarning(Throwable t) {
		ILog log = getDefault().getLog();

		log.log(new Status(IStatus.WARNING, PLUGIN_ID, t.getMessage(), t));
	}

	public LiferayCore() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		_plugin = this;

		Dictionary<String, Object> preferences = new Hashtable<>();

		preferences.put(Constants.SERVICE_RANKING, 1);

		_listenerRegistryServiceTracker = _createServiceTracker(context, ListenerRegistry.class);
		_listenerRegistryService = context.registerService(
			ListenerRegistry.class.getName(), new DefaultListenerRegistry(), preferences);
		_projectChangeListener = ProjectChangeListener.createAndRegister();
		
		initLiferayWorkspaceProduct();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_plugin = null;

		_listenerRegistryService.unregister();

		_listenerRegistryServiceTracker.close();

		_projectChangeListener.close();

		super.stop(context);
	}

	private static final String _PRODUCT_INFO_URL = "https://releases-cdn.liferay.com/tools/workspace/.product_info.json";
	private static final String _DEFAULT_WORKSPACE_CACHE_DIR_NAME = ".liferay/workspace";
	private static File _workspaceCacheDir = new File(System.getProperty("user.home"),
			_DEFAULT_WORKSPACE_CACHE_DIR_NAME);

	

	private void initLiferayWorkspaceProduct() {
		Job initLiferayWorkspaceProductInfo = new Job("Init Liferay Workspace Product") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					DownloadCommand downloadCommand = new DownloadCommand();

					downloadCommand.setCacheDir(_workspaceCacheDir);
					downloadCommand.setPassword(null);
					downloadCommand.setToken(false);
					downloadCommand.setUrl(new URL(_PRODUCT_INFO_URL));
					downloadCommand.setUserName(null);
					downloadCommand.setQuiet(true);

					downloadCommand.execute();
				}
				catch (Exception exception) {
					LiferayCore.logError("Failed to init liferay workspace product", exception);
				}
				
				return Status.OK_STATUS;
			}
			
		};

		initLiferayWorkspaceProductInfo.setSystem(true);
		initLiferayWorkspaceProductInfo.setUser(false);
		initLiferayWorkspaceProductInfo.schedule();
	}
	
	
	private static <T extends ILiferayProject> ILiferayProject _checkProjectCache(Class<T> type, Object adaptable) {
		Map<ProjectCacheKey<?>, ILiferayProject> projectCache = _plugin._projectCache;

		ProjectCacheKey<T> projectCacheKey = new ProjectCacheKey<>(type, adaptable);

		ILiferayProject liferayProject = projectCache.get(projectCacheKey);

		if ((liferayProject != null) && liferayProject.isStale()) {

			// Stale project should be removed the first time

			_removeFromCache(projectCacheKey, liferayProject);

			liferayProject = null;
		}

		//Handle the situation that the adaptable object could have muti-type

		if (liferayProject == null) {
			Entry<ProjectCacheKey<?>, ILiferayProject> cachedEntry = null;

			List<Entry<ProjectCacheKey<?>, ILiferayProject>> staleEntries = new ArrayList<>();

			for (Entry<ProjectCacheKey<?>, ILiferayProject> entry : projectCache.entrySet()) {
				ILiferayProject cachedLiferayProject = entry.getValue();

				if (type.isInstance(cachedLiferayProject) && adaptable.equals(cachedLiferayProject.getProject()) &&
					!cachedLiferayProject.isStale()) {

					cachedEntry = entry;
				}
				else if (cachedLiferayProject.isStale()) {
					staleEntries.add(entry);
				}
			}

			if (cachedEntry != null) {
				liferayProject = cachedEntry.getValue();
			}

			for (Entry<ProjectCacheKey<?>, ILiferayProject> staleEntry : staleEntries) {
				_removeFromCache(staleEntry.getKey(), staleEntry.getValue());
			}
		}

		return liferayProject;
	}

	private static ILiferayProject _createInternal(Class<?> type, Object adaptable) {
		ILiferayProjectProvider[] liferayProjectProviders = getProviders(adaptable.getClass());

		if (ListUtil.isEmpty(liferayProjectProviders)) {
			return null;
		}

		ILiferayProjectProvider currentLiferayProjectProvider = null;
		ILiferayProject liferayProject = null;

		for (ILiferayProjectProvider liferayProjectProvider : liferayProjectProviders) {
			if ((currentLiferayProjectProvider == null) ||
				(liferayProjectProvider.getPriority() > currentLiferayProjectProvider.getPriority())) {

				ILiferayProject providedLiferayProject = liferayProjectProvider.provide(type, adaptable);

				if (providedLiferayProject != null) {
					currentLiferayProjectProvider = liferayProjectProvider;

					liferayProject = providedLiferayProject;
				}
			}
		}

		return liferayProject;
	}

	private static <T extends ILiferayProject> void _putProjectCache(
		Class<T> type, Object adaptable, ILiferayProject liferayProject) {

		Map<ProjectCacheKey<?>, ILiferayProject> projectCache = _plugin._projectCache;

		projectCache.put(new ProjectCacheKey<>(type, adaptable), liferayProject);
	}

	private static void _removeFromCache(ProjectCacheKey<?> projectCacheKey, ILiferayProject liferayProject) {
		Map<ProjectCacheKey<?>, ILiferayProject> projectCache = _plugin._projectCache;

		if (liferayProject instanceof EventListener) {
			ListenerRegistry listenerRegistry = listenerRegistry();

			listenerRegistry.removeEventListener((EventListener)liferayProject);
		}

		projectCache.remove(projectCacheKey);
	}

	private <T> ServiceTracker<T, T> _createServiceTracker(BundleContext context, Class<T> clazz) {
		ServiceTracker<T, T> serviceTracker = new ServiceTracker<>(context, clazz.getName(), null);

		serviceTracker.open();

		return serviceTracker;
	}

	private static final LiferayProjectAdapterReader _adapterReader = new LiferayProjectAdapterReader();
	private static LiferayProjectImporterReader _importerReader;
	private static LiferayCore _plugin;
	private static LiferayProjectProviderReader _providerReader;

	private ServiceRegistration<?> _listenerRegistryService;
	private ServiceTracker<ListenerRegistry, ListenerRegistry> _listenerRegistryServiceTracker;
	private final Map<ProjectCacheKey<?>, ILiferayProject> _projectCache = new HashMap<>();
	private ProjectChangeListener _projectChangeListener;

	private static class ProjectCacheKey<T extends ILiferayProject> {

		public ProjectCacheKey(Class<T> type, Object adaptable) {
			_type = type;
			_adaptable = adaptable;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj == null) {
				return false;
			}

			if (getClass() != obj.getClass()) {
				return false;
			}

			ProjectCacheKey<?> other = (ProjectCacheKey<?>)obj;

			if (Objects.equals(_adaptable, other._adaptable) && Objects.equals(_type, other._type)) {
				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(_adaptable, _type);
		}

		@Override
		public String toString() {
			return "ProjectCacheKey [adaptable=" + _adaptable + ", type=" + _type + "]";
		}

		private Object _adaptable;
		private Class<T> _type;

	}

}