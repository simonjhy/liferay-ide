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

package com.liferay.ide.gradle.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.liferay.ide.core.LiferayCore;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import org.apache.commons.io.FileUtils;
import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plugin life cycle
 *
 * @author Gregory Amerson
 * @author Terry Jia
 * @author Andy Wu
 */
@SuppressWarnings("restriction")
public class GradleCore extends Plugin {

	// The shared instance

	public static final String JOB_FAMILY_ID = "CheckingGradleConfiguration";

	// The plugin ID

	public static final String PLUGIN_ID = "com.liferay.ide.gradle.core";

	public static final File customModelCache = LiferayCore.GLOBAL_SETTINGS_PATH.toFile();



	public static IStatus createErrorStatus(Exception ex) {
		return new Status(IStatus.ERROR, PLUGIN_ID, ex.getMessage(), ex);
	}

	public static IStatus createErrorStatus(String msg) {
		return new Status(IStatus.ERROR, PLUGIN_ID, msg);
	}

	public static IStatus createErrorStatus(String msg, Exception e) {
		return new Status(IStatus.ERROR, PLUGIN_ID, msg, e);
	}

	public static IStatus createWarningStatus(String msg) {
		return new Status(IStatus.WARNING, PLUGIN_ID, msg);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static GradleCore getDefault() {
		return _plugin;
	}

	public static <T> T getToolingModel(Class<T> modelClass, File projectDir) {
		T retval = null;

		try {
			retval = GradleTooling.getModel(modelClass, customModelCache, projectDir);
		}
		catch (Exception e) {
			logError("Error getting tooling model", e);
		}

		return retval;
	}

	public static <T> T getToolingModel(Class<T> modelClass, IProject gradleProject) {
		return getToolingModel(modelClass, gradleProject.getLocation().toFile());
	}

	public static void logError(Exception ex) {
		ILog log = getDefault().getLog();

		log.log(createErrorStatus(ex));
	}

	public static void logError(String msg) {
		ILog log = getDefault().getLog();

		log.log(createErrorStatus(msg));
	}

	public static void logError(String msg, Exception e) {
		ILog log = getDefault().getLog();

		log.log(createErrorStatus(msg, e));
	}

	/**
	 * The constructor
	 */
	public GradleCore() {
		_gradleProjectCreatedListener = new GradleProjectCreatedListener();
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);

		_plugin = this;

		CorePlugin.listenerRegistry().addEventListener(_gradleProjectCreatedListener);
	}

	public void stop(BundleContext context) throws Exception {
		CorePlugin.listenerRegistry().removeEventListener(_gradleProjectCreatedListener);

		_plugin = null;

		super.stop(context);
	}

	private static GradleCore _plugin;

	private final GradleProjectCreatedListener _gradleProjectCreatedListener;

}