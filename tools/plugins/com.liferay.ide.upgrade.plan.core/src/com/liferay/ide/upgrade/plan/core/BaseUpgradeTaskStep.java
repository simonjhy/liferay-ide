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

package com.liferay.ide.upgrade.plan.core;

import com.liferay.ide.upgrade.plan.core.util.ServicesLookup;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

/**
 * @author Gregory Amerson
 * @author Terry Jia
 */
public abstract class BaseUpgradeTaskStep extends BaseUpgradePlanElement implements UpgradeTaskStep {

	@Activate
	public void activate(ComponentContext componentContext) {
		super.activate(componentContext);

		_componentContext = componentContext;

		Dictionary<String, Object> properties = componentContext.getProperties();

		_requirement = getStringProperty(properties, "requirement");
		_taskId = getStringProperty(properties, "taskId");
		_url = getStringProperty(properties, "url");

		_lookupActions(componentContext);
	}

	@Override
	public boolean completed() {
		Stream<UpgradeTaskStepAction> stream = getActions().stream();

		long count = stream.filter(
			action -> UpgradeTaskStepActionStatus.INCOMPLETE.equals(action.getStatus())
		).count();

		if (count == 0) {
			return true;
		}

		return false;
	}

	@Override
	public boolean enabled() {
		BundleContext bundleContext = _componentContext.getBundleContext();

		List<UpgradeTaskStep> upgradeTaskSteps = ServicesLookup.getOrderedServices(
			bundleContext, UpgradeTaskStep.class, "&(taskId=" + getTaskId() + ")(requirement=required)");

		Stream<UpgradeTaskStep> upgradeTaskStepsStream = upgradeTaskSteps.stream();

		long count = upgradeTaskStepsStream.filter(
			upgradeTaskStep -> upgradeTaskStep.getOrder() < getOrder()
		).map(
			upgradeTaskStep -> upgradeTaskStep.getActions()
		).flatMap(
			actions -> actions.stream()
		).filter(
			action -> UpgradeTaskStepActionStatus.INCOMPLETE.equals(action.getStatus())
		).count();

		if (count > 0) {
			return false;
		}

		return true;
	}

	@Override
	public List<UpgradeTaskStepAction> getActions() {
		return Collections.unmodifiableList(_upgradeTaskStepActions);
	}

	@Override
	public UpgradeTaskStepRequirement getRequirement() {
		return UpgradeTaskStepRequirement.valueOf(UpgradeTaskStepRequirement.class, _requirement.toUpperCase());
	}

	@Override
	public String getTaskId() {
		return _taskId;
	}

	@Override
	public String getUrl() {
		return _url;
	}

	private void _lookupActions(ComponentContext componentContext) {
		BundleContext bundleContext = componentContext.getBundleContext();

		List<UpgradeTaskStepAction> upgradeTaskStepActions = ServicesLookup.getOrderedServices(
			bundleContext, UpgradeTaskStepAction.class, "(stepId=" + getId() + ")");

		Stream<UpgradeTaskStepAction> stream = upgradeTaskStepActions.stream();

		UpgradePlanner upgradePlanner = ServicesLookup.getSingleService(UpgradePlanner.class, null);

		_upgradeTaskStepActions = stream.filter(
			upgradeTaskStepAction -> upgradeTaskStepAction.appliesTo(upgradePlanner.getCurrentUpgradePlan())
		).collect(
			Collectors.toList()
		);
	}

	private ComponentContext _componentContext;
	private String _requirement;
	private String _taskId;
	private List<UpgradeTaskStepAction> _upgradeTaskStepActions;
	private String _url;

}