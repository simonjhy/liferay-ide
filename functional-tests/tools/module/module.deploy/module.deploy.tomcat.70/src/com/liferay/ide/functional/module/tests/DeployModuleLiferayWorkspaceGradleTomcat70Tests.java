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

package com.liferay.ide.functional.module.tests;

import com.liferay.ide.functional.liferay.support.server.LiferaryWorkspaceTomcat70Support;
import com.liferay.ide.functional.liferay.support.workspace.LiferayWorkspaceGradle70Support;
import com.liferay.ide.functional.liferay.support.workspace.LiferayWorkspaceSupport;
import com.liferay.ide.functional.liferay.util.RuleUtil;
import com.liferay.ide.functional.module.deploy.base.DeployModuleLiferayWorkspaceGradleTomcat7xBase;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Lily Li
 */
public class DeployModuleLiferayWorkspaceGradleTomcat70Tests extends DeployModuleLiferayWorkspaceGradleTomcat7xBase {

	public static LiferayWorkspaceGradle70Support workspace = new LiferayWorkspaceGradle70Support(bot);
	public static LiferaryWorkspaceTomcat70Support server = new LiferaryWorkspaceTomcat70Support(bot, workspace);

	@ClassRule
	public static RuleChain chain = RuleUtil.getTomcat70RunningLiferayWorkspaceRuleChain(bot, workspace, server);

	@Test
	public void deployApi() {
		super.deployApi();
	}

	@Test
	public void deployControlMenuEntry() {
		super.deployControlMenuEntry();
	}

	@Test
	public void deployFormField() {
		super.deployFormField();
	}

	@Test
	public void deployPanelApp() {
		super.deployPanelApp();
	}

	@Test
	public void deployPortletConfigurationIcon() {
		super.deployPortletConfigurationIcon();
	}

	@Test
	public void deployPortletProvider() {
		super.deployPortletProvider();
	}

	@Test
	public void deployPortletToolbarContributor() {
		super.deployPortletToolbarContributor();
	}

	@Test
	public void deployRest() {
		super.deployRest();
	}

	@Test
	public void deployService() {
		super.deployService();
	}

	@Test
	public void deployServiceWrapper() {
		super.deployServiceWrapper();
	}

	@Test
	public void deploySimulationPanelEntry() {
		super.deploySimulationPanelEntry();
	}

	@Test
	public void deployTemplateContextContributor() {
		super.deployTemplateContextContributor();
	}

	@Test
	public void deployThemeContributor() {
		super.deployThemeContributor();
	}

	@Test
	public void deployWarHook() {
		super.deployWarHook();
	}

	@Test
	public void deployWarMvcPortlet() {
		super.deployWarMvcPortlet();
	}

	@Override
	protected LiferayWorkspaceSupport getLiferayWorkspace() {
		return workspace;
	}

	@Override
	protected String getServerName() {
		return server.getServerName();
	}

	@Override
	protected String getStartedLabel() {
		return server.getStartedLabel();
	}

	@Override
	protected String getVersion() {
		return "7.0";
	}

}