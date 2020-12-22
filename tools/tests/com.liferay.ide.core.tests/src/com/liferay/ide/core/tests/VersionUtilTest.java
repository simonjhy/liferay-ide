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

package com.liferay.ide.core.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.liferay.ide.core.util.VersionUtil;

/**
 * @author Seiphon Wang
 */
public class VersionUtilTest {

	@Test
	public void testSimplifyTargetPlatformVersion() throws Exception {
		Assert.assertEquals("7.3.2", VersionUtil.simplifyTargetPlatformVersion("7.3.2"));

		Assert.assertEquals("7.1.10.1", VersionUtil.simplifyTargetPlatformVersion("7.1.10.1"));

		Assert.assertEquals("7.3.2.1", VersionUtil.simplifyTargetPlatformVersion("7.3.2-1"));

		Assert.assertEquals("7.2.10.1", VersionUtil.simplifyTargetPlatformVersion("7.2.10.ga1"));

		Assert.assertEquals("7.3.10.5", VersionUtil.simplifyTargetPlatformVersion("7.3.10.ep5"));

		Assert.assertEquals("7.2.10.1", VersionUtil.simplifyTargetPlatformVersion("7.2.10.fp1-1"));
	}

	@Test
	public void testConvertToReleaseAPIVersion() {
		List<String> possibleValues = new ArrayList<String>();

		possibleValues = VersionUtil.convertToReleaseAPIVersions("7.2.10.fp1-1", "dxp-7.2-fp1");

		Assert.assertArrayEquals(new String[] {"7.2.10.fp1", "7.2.10.fp1-1"}, possibleValues.toArray());

		possibleValues = VersionUtil.convertToReleaseAPIVersions("7.1.10.fp19", "dxp-7.1-fp19");

		Assert.assertArrayEquals(new String[] {"7.1.10.fp19"}, possibleValues.toArray());

		possibleValues = VersionUtil.convertToReleaseAPIVersions("7.3.10.0-2", "dxp-7.3-ga1");

		Assert.assertArrayEquals(new String[] {"7.3.10-ga1", "7.3.10-ga1-2"}, possibleValues.toArray());

		possibleValues = VersionUtil.convertToReleaseAPIVersions("7.3.4", "portal-7.3-ga5");

		Assert.assertArrayEquals(new String[]{"7.3.4-ga5"}, possibleValues.toArray());

		possibleValues = VersionUtil.convertToReleaseAPIVersions("7.3.3-1", "portal-7.3-ga4");

		Assert.assertArrayEquals(new String[]{"7.3.3-ga4", "7.3.3-ga4-1"}, possibleValues.toArray());

		possibleValues = VersionUtil.convertToReleaseAPIVersions("7.0.6-2", "portal-7.0-ga7");

		Assert.assertArrayEquals(new String[]{"7.0.6-ga7", "7.0.6-ga7-2"}, possibleValues.toArray());
	}
}
