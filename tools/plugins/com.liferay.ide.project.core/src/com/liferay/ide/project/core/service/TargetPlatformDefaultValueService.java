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

package com.liferay.ide.project.core.service;

import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.core.util.SapphireUtil;
import com.liferay.ide.core.workspace.WorkspaceConstants;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.workspace.NewLiferayWorkspaceOp;

import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.sapphire.DefaultValueService;
import org.eclipse.sapphire.FilteredListener;
import org.eclipse.sapphire.PossibleValuesService;
import org.eclipse.sapphire.PropertyContentEvent;

/**
 * @author Terry Jia
 */
public class TargetPlatformDefaultValueService extends DefaultValueService {

	@Override
	public void dispose() {
		NewLiferayWorkspaceOp op = context(NewLiferayWorkspaceOp.class);

		if (op != null) {
			SapphireUtil.detachListener(op.property(NewLiferayWorkspaceOp.PROP_LIFERAY_VERSION), _listener);
		}

		super.dispose();
	}
	
	@Override
	protected String compute() {
		
		PossibleValuesService service = _op.getTargetPlatform().service(TargetPlatformPossibleValuesService.class);
		
		Set<String> targetPlatformValues = service.values();
		
		if (targetPlatformValues != null && targetPlatformValues.size() >0) {
			return targetPlatformValues.toArray(new String[0])[0];
		}
		else{
			String[] liferayTargetPlatformVersions = WorkspaceConstants.liferayTargetPlatformVersions.get("7.2");
	
			String defaultValue = liferayTargetPlatformVersions[0];
	
			IScopeContext[] scopeContexts = {DefaultScope.INSTANCE, InstanceScope.INSTANCE};
	
			IPreferencesService preferencesService = Platform.getPreferencesService();
	
			String defaultLiferayVersion = preferencesService.getString(
				ProjectCore.PLUGIN_ID, ProjectCore.PREF_DEFAULT_LIFERAY_VERSION_OPTION, null, scopeContexts);
	
			if (defaultLiferayVersion != null) {
				String[] targetPlatformVersions = WorkspaceConstants.liferayTargetPlatformVersions.get(
					defaultLiferayVersion);
	
				if (ListUtil.isNotEmpty(targetPlatformVersions)) {
					defaultValue = WorkspaceConstants.liferayTargetPlatformVersions.get(defaultLiferayVersion)[0];
				}
			}
			
			return defaultValue;
		}
	}

	
	
	@Override
	protected void initDefaultValueService() {
		_listener = new FilteredListener<PropertyContentEvent>() {

			@Override
			protected void handleTypedEvent(PropertyContentEvent event) {
				refresh();
			}

		};

		_op = context(NewLiferayWorkspaceOp.class);

		SapphireUtil.attachListener(_op.property(NewLiferayWorkspaceOp.PROP_LIFERAY_VERSION), _listener);
	}

	private FilteredListener<PropertyContentEvent> _listener;
	private NewLiferayWorkspaceOp _op;
}