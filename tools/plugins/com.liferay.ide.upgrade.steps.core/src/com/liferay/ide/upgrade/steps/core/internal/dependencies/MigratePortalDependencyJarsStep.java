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

package com.liferay.ide.upgrade.steps.core.internal.dependencies;

import com.liferay.ide.upgrade.plan.core.BaseUpgradeStep;
import com.liferay.ide.upgrade.plan.core.UpgradePlanner;
import com.liferay.ide.upgrade.plan.core.UpgradeStep;
import com.liferay.ide.upgrade.steps.core.dependencies.MigratePortalDependencyJarsStepKeys;
import com.liferay.ide.upgrade.steps.core.dependencies.UpdatePluginsSDKDependenciesStepKeys;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Terry Jia
 */
@Component(
	property = {
		"id=" + MigratePortalDependencyJarsStepKeys.ID,
		"description=" + MigratePortalDependencyJarsStepKeys.DESCRIPTION, "order=1", "requirement=recommended",
		"parentId=" + UpdatePluginsSDKDependenciesStepKeys.ID, "title=" + MigratePortalDependencyJarsStepKeys.TITLE
	},
	scope = ServiceScope.PROTOTYPE, service = UpgradeStep.class
)
public class MigratePortalDependencyJarsStep extends BaseUpgradeStep {

	@Activate
	public void activate(ComponentContext componentContext) {
		super.activate(_upgradePlanner, componentContext);
	}

	@Override
	public IStatus perform(IProgressMonitor progressMonitor) {
		return Status.OK_STATUS;
	}

	@Reference
	private UpgradePlanner _upgradePlanner;

}