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

package com.liferay.ide.functional.hook.tests;

import com.liferay.ide.functional.liferay.SwtbotBase;
import com.liferay.ide.functional.liferay.support.project.SdkProjectSupport;
import com.liferay.ide.functional.liferay.support.server.PureTomcat70Support;
import com.liferay.ide.functional.liferay.util.RuleUtil;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Terry Jia
 * @author Ying Xu
 */
public class NewHookConfigTests extends SwtbotBase {

	public static PureTomcat70Support tomcat = new PureTomcat70Support(bot);

	@ClassRule
	public static RuleChain chain = RuleUtil.getTomcat7xSdkRuleChain(bot, tomcat);

	@Ignore("")
	@Test
	public void createSampleProject() {
		viewAction.switchLiferayPerspective();

		wizardAction.openNewLiferayPluginProjectWizard();

		wizardAction.newPlugin.prepareHookSdk(project.getNameHook());

		wizardAction.finish();

		jobAction.waitForIvy();

		jobAction.waitForValidate(project.getNameHook());

		viewAction.project.closeAndDelete(project.getNameHook());
	}

	@Rule
	public SdkProjectSupport project = new SdkProjectSupport(bot);

}