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

package com.liferay.ide.core.util;

import java.net.URI;

import java.nio.file.Path;

/**
 * @author Seiphon Wang
 */
public class DownloadUtil {

	public static void download(URI uri, Path cacheDirPath) throws Exception {
		download(uri, null, null, cacheDirPath);
	}

	public static void download(URI uri, String userName, String password, Path cacheDirPath) throws Exception {
		HttpUtil.downloadFile(uri, userName, password, cacheDirPath);
	}

}