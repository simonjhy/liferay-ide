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

import java.net.InetAddress;

import org.eclipse.sapphire.DefaultValueService;

/**
 * @author Simon.Jiang
 */
public class CanConnectInternetDefaultValueService extends DefaultValueService {

	@Override
	protected String compute() {
		try {
			InetAddress inetAddress = InetAddress.getByName(_productJsonHost);

			if (inetAddress.isReachable(1000)) {
				return Boolean.toString(true);
			}
		}
		catch (Exception e) {
			return Boolean.toString(false);
		}

		return Boolean.toString(false);
	}

	private static String _productJsonHost = "releases.liferay.com";

}