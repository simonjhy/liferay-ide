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

package com.liferay.ide.upgrade.plan.core.internal;

import com.liferay.ide.upgrade.plan.core.UpgradePlan;
import com.liferay.ide.upgrade.plan.core.UpgradeProblem;
import com.liferay.ide.upgrade.plan.core.UpgradeStep;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Adapters;

/**
 * @author Gregory Amerson
 * @author Simon Jiang
 * @author Terry Jia
 */
public class StandardUpgradePlan implements UpgradePlan {

	public StandardUpgradePlan(
		String name, String currentVersion, String targetVersion, Path currentProjectLocation,
		String upgradePlanOutline, List<UpgradeStep> upgradeSteps) {

		_name = name;
		_currentVersion = currentVersion;
		_targetVersion = targetVersion;
		_currentProjectLocation = currentProjectLocation;
		_upgradePlanOutline = upgradePlanOutline;
		_upgradeProblems = new CopyOnWriteArraySet<>();
		_upgradeSteps = upgradeSteps;
	}

	@Override
	public void addUpgradeProblems(Collection<UpgradeProblem> upgradeProblems) {
		_upgradeProblems.addAll(upgradeProblems);
	}

	@Override
	public boolean equals(Object object) {
		if ((object instanceof StandardUpgradePlan) == false) {
			return false;
		}

		StandardUpgradePlan targetUpgradePlan = Adapters.adapt(object, StandardUpgradePlan.class);

		if (targetUpgradePlan == null) {
			return false;
		}

		if (isEqualIgnoreCase(_name, targetUpgradePlan._name) &&
			isEqualIgnoreCase(_currentVersion, targetUpgradePlan._currentVersion) &&
			isEqualIgnoreCase(_targetVersion, targetUpgradePlan._targetVersion) &&
			isEqualIgnoreCase(_upgradePlanOutline, targetUpgradePlan._upgradePlanOutline) &&
			isEqualIgnoreCase(_currentProjectLocation, targetUpgradePlan._currentProjectLocation) &&
			_upgradeProblems.equals(targetUpgradePlan._upgradeProblems) &&
			_upgradeSteps.equals(targetUpgradePlan._upgradeSteps)) {

			return true;
		}

		return false;
	}

	@Override
	public Path getCurrentProjectLocation() {
		return _currentProjectLocation;
	}

	@Override
	public String getCurrentVersion() {
		return _currentVersion;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public Path getTargetProjectLocation() {
		return _targetProjectLocation;
	}

	@Override
	public String getTargetVersion() {
		return _targetVersion;
	}

	public String getUpgradePlanOutline() {
		return _upgradePlanOutline;
	}

	@Override
	public Set<UpgradeProblem> getUpgradeProblems() {
		return _upgradeProblems;
	}

	@Override
	public List<UpgradeStep> getUpgradeSteps() {
		return Collections.unmodifiableList(_upgradeSteps);
	}

	@Override
	public List<String> getUpgradeVersions() {
		String currentVersion = getCurrentVersion();
		String targetVersion = getTargetVersion();

		boolean begin = false;

		List<String> upgradeVersions = new ArrayList<>();

		for (String liferayVersion : _liferayVersions) {
			if (begin) {
				upgradeVersions.add(liferayVersion);

				if (liferayVersion.equals(targetVersion)) {
					break;
				}
			}
			else if (liferayVersion.equals(currentVersion)) {
				begin = true;
			}
		}

		return upgradeVersions;
	}

	@Override
	public int hashCode() {
		int hash = 31;

		hash = 31 * hash + ((_name != null) ? _name.hashCode() : 0);
		hash = 31 * hash + ((_currentVersion != null) ? _currentVersion.hashCode() : 0);
		hash = 31 * hash + ((_targetVersion != null) ? _targetVersion.hashCode() : 0);
		hash = 31 * hash + ((_currentProjectLocation != null) ? _currentProjectLocation.hashCode() : 0);
		hash = 31 * hash + ((_upgradePlanOutline != null) ? _upgradePlanOutline.hashCode() : 0);

		Stream<UpgradeStep> stepStream = _upgradeSteps.stream();

		int stepChildrenHashCodes = stepStream.map(
			Object::hashCode
		).reduce(
			0, Integer::sum
		).intValue();

		Stream<UpgradeProblem> problemStream = _upgradeProblems.stream();

		int problemChildrenHashCodes = problemStream.map(
			Object::hashCode
		).reduce(
			0, Integer::sum
		).intValue();

		hash = 31 * hash + stepChildrenHashCodes;
		hash = 31 * hash + problemChildrenHashCodes;

		return hash;
	}

	public boolean isEqualIgnoreCase(Path original, Path target) {
		if (original != null) {
			return original.equals(target);
		}

		if (target == null) {
			return true;
		}

		return false;
	}

	public boolean isEqualIgnoreCase(String original, String target) {
		if (original != null) {
			return original.equalsIgnoreCase(target);
		}

		if (target == null) {
			return true;
		}

		return false;
	}

	@Override
	public void setCurrentProjectLocation(Path path) {
		_currentProjectLocation = path;
	}

	@Override
	public void setTargetProjectLocation(Path path) {
		_targetProjectLocation = path;
	}

	@SuppressWarnings("serial")
	private static final List<String> _liferayVersions = new ArrayList<String>() {
		{
			add("6.2");
			add("7.0");
			add("7.1");
		}
	};

	private Path _currentProjectLocation;
	private final String _currentVersion;
	private final String _name;
	private Path _targetProjectLocation;
	private final String _targetVersion;
	private String _upgradePlanOutline;
	private Set<UpgradeProblem> _upgradeProblems;
	private final List<UpgradeStep> _upgradeSteps;

}