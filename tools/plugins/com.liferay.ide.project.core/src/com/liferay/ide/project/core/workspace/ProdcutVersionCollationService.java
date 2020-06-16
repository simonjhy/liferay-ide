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

package com.liferay.ide.project.core.workspace;

import aQute.bnd.version.Version;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.StringUtil;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.sapphire.CollationService;

/**
 * @author Simon Jiang
 */
public class ProdcutVersionCollationService extends CollationService {

	@Override
	protected Comparator<String> compute() {
		return new WorkspaceProductComparator();
	}

	private class WorkspaceProductComparator implements Comparator<String> {

		@Override
		public int compare(String a, String b) {
			if (a.startsWith("dxp") && !b.startsWith("dxp")) {
				return -1;
			}
			else if (a.startsWith("portal") && b.startsWith("dxp")) {
				return 1;
			}
			else if (a.startsWith("portal") && b.startsWith("commerce")) {
				return -1;
			}
			else if (a.startsWith("commerce") && !b.startsWith("commerce")) {
				return 1;
			}
			else if (!StringUtil.equals(_getProductMainVersion(a), _getProductMainVersion(b))) {
				Version aProductMainVerson = Version.parseVersion(_getProductMainVersion(a));
				Version bProductMainVerson = Version.parseVersion(_getProductMainVersion(b));

				return -1 * aProductMainVerson.compareTo(bProductMainVerson);
			}
			else {
				String aProductMicroVersion = _getProductMicroVersion(a);
				String bProductMicroVersion = _getProductMicroVersion(b);

				if (CoreUtil.isNullOrEmpty(aProductMicroVersion)) {
					return 1;
				}
				else if (CoreUtil.isNullOrEmpty(bProductMicroVersion)) {
					return -1;
				}
				else if (Version.isVersion(aProductMicroVersion) && Version.isVersion(bProductMicroVersion)) {
					Version aMicroVersion = Version.parseVersion(aProductMicroVersion);
					Version bMicroVersion = Version.parseVersion(bProductMicroVersion);

					return -1 * aMicroVersion.compareTo(bMicroVersion);
				}
				else {
					String aMicroVersionPrefix = aProductMicroVersion.substring(0, 2);
					String bMicroVersionPrefix = bProductMicroVersion.substring(0, 2);

					if (!aMicroVersionPrefix.equalsIgnoreCase(bMicroVersionPrefix)) {
						return -1 * aMicroVersionPrefix.compareTo(bMicroVersionPrefix);
					}

					String aMicroVersionString = aProductMicroVersion.substring(2);
					String bMicroVersionString = bProductMicroVersion.substring(2);

					return Integer.parseInt(bMicroVersionString) - Integer.parseInt(aMicroVersionString);
				}
			}
		}

		private String _getProductMainVersion(String productKey) {
			Matcher aMatcher = _versionPattern.matcher(productKey.substring(productKey.indexOf('-') + 1));

			if (aMatcher.find()) {
				return aMatcher.group(1);
			}

			return "";
		}

		private String _getProductMicroVersion(String productKey) {
			String[] prodcutKeyArrays = StringUtil.split(productKey, "-");

			if (prodcutKeyArrays.length > 2) {
				return prodcutKeyArrays[2];
			}

			return null;
		}

		private static final Pattern _versionPattern = Pattern.compile("([0-9\\.]+).*");

}