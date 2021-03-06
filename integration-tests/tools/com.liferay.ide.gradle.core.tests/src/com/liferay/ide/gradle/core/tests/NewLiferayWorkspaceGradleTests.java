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

package com.liferay.ide.gradle.core.tests;

import com.liferay.ide.core.workspace.WorkspaceConstants;
import com.liferay.ide.project.core.workspace.NewLiferayWorkspaceOp;
import com.liferay.ide.test.core.base.support.LiferayWorkspaceSupport;
import com.liferay.ide.test.project.core.base.ProjectOpBase;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author Andy Wu
 * @author Joye Luo
 * @author Ashley Yuan
 */
public class NewLiferayWorkspaceGradleTests extends ProjectOpBase<NewLiferayWorkspaceOp> {

	@Test
	public void createWorkspace() throws Exception {
		NewLiferayWorkspaceOp op = NewLiferayWorkspaceOp.TYPE.instantiate();

		op.setWorkspaceName(workspace.getName());
		op.setProjectProvider(provider());
		op.setProductVersion("dxp-7.2-sp3");

		waitForBuildAndValidation();

		createOrImportAndBuild(op, workspace.getName());

		deleteProject(workspace.getName());
	}

	@Test
	public void createWorkspace71() throws Exception {
		NewLiferayWorkspaceOp op = NewLiferayWorkspaceOp.TYPE.instantiate();

		op.setWorkspaceName(workspace.getName());
		op.setProjectProvider(provider());
		op.setProductVersion("portal-7.1-ga4");

		waitForBuildAndValidation();

		createOrImportAndBuild(op, workspace.getName());

		waitForBuildAndValidation();

		assertPropertyValue(
			workspace.getName(), "gradle.properties", "liferay.workspace.product",
			WorkspaceConstants.WORKSPACE_PRODUCT_7_1);

		deleteProject(workspace.getName());
	}

	@Rule
	public LiferayWorkspaceSupport workspace = new LiferayWorkspaceSupport();

	@Override
	protected String provider() {
		return "gradle-liferay-workspace";
	}

	protected void verifyProjectFiles(String projectName) {
		assertProjectFileExists(projectName, "build.gradle");
		assertProjectFileNotExists(projectName, "pom.xml");
	}

}