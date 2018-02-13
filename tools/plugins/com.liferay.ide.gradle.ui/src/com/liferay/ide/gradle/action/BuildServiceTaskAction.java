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

import com.liferay.ide.core.IBundleProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.gradle.core.GradleUtil;
import com.liferay.ide.project.core.util.SearchFilesVisitor;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.EclipseProject;

/**
 * @author Lovett Li
 * @author Terry Jia
 * @author Andy Wu
 */
public class BuildServiceTaskAction extends GradleTaskAction {

	public boolean getServiceParentProject(IProject checkProject) {

		if (checkProject != null) {
			List<IFile> serviceXmlFiles = new SearchFilesVisitor().searchFiles(
				checkProject, "service.xml");
			IBundleProject serviceProject =
				LiferayCore.create(IBundleProject.class, checkProject);

			if (serviceXmlFiles.size() > 0 && serviceProject == null &&
				hasChildProject(checkProject)) {
				project = checkProject;
				return true;
			}
			else {
				return getServiceParentProject(getParentProject(checkProject));
			}
		}
		else {
			return false;
		}
	}

	@Override
	protected void setEnableTaskAction(IAction action) {

		boolean enabled = getServiceParentProject(project);
		action.setEnabled(enabled);
	}

	private IProject getParentProject(IProject checkProject) {

		ProjectConnection connection = null;

		GradleConnector connector =
			GradleConnector.newConnector().forProjectDirectory(
				checkProject.getLocation().toFile());

		connection = connector.connect();

		ModelBuilder<EclipseProject> modelBuilder =
			connection.model(EclipseProject.class);
		EclipseProject eclipseProject = modelBuilder.get();
		EclipseProject serviceParentEclipseProject = eclipseProject.getParent();

		if (serviceParentEclipseProject != null) {
			return CoreUtil.getProject(
				serviceParentEclipseProject.getProjectDirectory());
		}

		return null;
	}

	public boolean hasChildProject(IProject selectedProject) {

		ProjectConnection connection = null;

		GradleConnector connector =
			GradleConnector.newConnector().forProjectDirectory(
				selectedProject.getLocation().toFile());

		connection = connector.connect();

		ModelBuilder<EclipseProject> modelBuilder =
			connection.model(EclipseProject.class);
		EclipseProject project = modelBuilder.get();
		return project.getChildren().size() > 0;
	}

	protected void afterTask() {

		GradleUtil.refreshGradleProject(project);
	}

	@Override
	protected String getGradleTask() {

		return "buildService";
	}

}
