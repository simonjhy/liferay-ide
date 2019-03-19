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

package com.liferay.ide.upgrade.steps.ui.internal.sdk;

import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.MultiStatusBuilder;
import com.liferay.ide.core.util.SapphireContentAccessor;
import com.liferay.ide.project.core.ProjectSynchronizer;
import com.liferay.ide.project.core.model.ProjectNamedItem;
import com.liferay.ide.project.core.modules.BladeCLI;
import com.liferay.ide.ui.util.UIUtil;
import com.liferay.ide.upgrade.plan.core.BaseUpgradeStep;
import com.liferay.ide.upgrade.plan.core.UpgradePlan;
import com.liferay.ide.upgrade.plan.core.UpgradePlanner;
import com.liferay.ide.upgrade.plan.core.UpgradeStep;
import com.liferay.ide.upgrade.plan.core.UpgradeStepPerformedEvent;
import com.liferay.ide.upgrade.plan.core.UpgradeStepStatus;
import com.liferay.ide.upgrade.steps.core.ImportSDKProjectsOp;
import com.liferay.ide.upgrade.steps.core.sdk.MigratePluginsSDKProjectsStepKeys;
import com.liferay.ide.upgrade.steps.ui.internal.ImportSDKProjectsWizard;
import com.liferay.ide.upgrade.steps.ui.internal.UpgradeStepsUIPlugin;

import java.nio.file.Path;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.sapphire.ElementList;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Terry Jia
 * @author Gregory Amerson
 */
@Component(
	property = {
		"description=" + ConvertPluginsSDKProjectsToModulesStepKeys.DESCRIPTION,
		"id=" + ConvertPluginsSDKProjectsToModulesStepKeys.ID, "imagePath=icons/export.png", "requirement=recommended",
		"order=6", "parentId=" + MigratePluginsSDKProjectsStepKeys.ID,
		"title=" + ConvertPluginsSDKProjectsToModulesStepKeys.TITLE
	},
	scope = ServiceScope.PROTOTYPE, service = UpgradeStep.class
)
public class ConvertPluginsSDKProjectsToModulesStep extends BaseUpgradeStep implements SapphireContentAccessor {

	@Override
	public IStatus perform(IProgressMonitor progressMonitor) {
		UpgradePlan upgradePlan = _upgradePlanner.getCurrentUpgradePlan();

		Path targetProjectLocation = upgradePlan.getTargetProjectLocation();

		if (targetProjectLocation == null) {
			return UpgradeStepsUIPlugin.createErrorStatus("There is no target project configured for current plan.");
		}

		if (FileUtil.notExists(targetProjectLocation.toFile())) {
			return UpgradeStepsUIPlugin.createErrorStatus("There is no code located at " + targetProjectLocation);
		}

		Path pluginsSDKPath = targetProjectLocation.resolve("plugins-sdk");

		final AtomicInteger returnCode = new AtomicInteger();

		ImportSDKProjectsOp sdkProjectsImportOp = ImportSDKProjectsOp.TYPE.instantiate();

		UIUtil.sync(
			() -> {
				ImportSDKProjectsWizard importSDKProjectsWizard = new ImportSDKProjectsWizard(
					sdkProjectsImportOp, pluginsSDKPath);

				IWorkbench workbench = PlatformUI.getWorkbench();

				IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

				Shell shell = workbenchWindow.getShell();

				WizardDialog wizardDialog = new WizardDialog(shell, importSDKProjectsWizard);

				returnCode.set(wizardDialog.open());
			});

		IStatus status = Status.OK_STATUS;

		if (returnCode.get() == Window.OK) {
			ElementList<ProjectNamedItem> projects = sdkProjectsImportOp.getSelectedProjects();

			Stream<ProjectNamedItem> stream = projects.stream();

			MultiStatusBuilder multiStatusBuilder = new MultiStatusBuilder(UpgradeStepsUIPlugin.PLUGIN_ID);

			stream.map(
				projectNamedItem -> get(projectNamedItem.getLocation())
			).forEach(
				location -> {
					StringBuilder sb = new StringBuilder();

					org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path(location.toString());

					String name = path.lastSegment();

					progressMonitor.subTask("Converting " + name + " to a module");

					sb.append("convert ");
					sb.append("--source \"");
					sb.append(pluginsSDKPath.toString());
					sb.append("\" --base \"");
					sb.append(targetProjectLocation.toString());
					sb.append("\" \"");
					sb.append(name);
					sb.append("\"");

					try {
						BladeCLI.execute(sb.toString());

						multiStatusBuilder.add(Status.OK_STATUS);
					}
					catch (Exception e) {
						IStatus errorStatus = UpgradeStepsUIPlugin.createErrorStatus(
							"Convert failed on project " + name, e);

						UpgradeStepsUIPlugin.log(errorStatus);

						multiStatusBuilder.add(errorStatus);
					}
				}
			);

			org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path(targetProjectLocation.toString());

			IStatus syncStatus = _synchronizer.synchronizePath(path, progressMonitor);

			multiStatusBuilder.add(syncStatus);

			status = multiStatusBuilder.build();

			if (status.isOK()) {
				setStatus(UpgradeStepStatus.COMPLETED);

				Stream<ProjectNamedItem> projectNamedItems = projects.stream();

				List<String> projectNames = projectNamedItems.map(
					projectNamedItem -> get(projectNamedItem.getLocation())
				).collect(
					Collectors.toList()
				);

				_upgradePlanner.dispatch(new UpgradeStepPerformedEvent(this, projectNames));
			}
		}

		return status;
	}

	@Reference(target = "(type=gradle)")
	private ProjectSynchronizer _synchronizer;

	@Reference
	private UpgradePlanner _upgradePlanner;

}