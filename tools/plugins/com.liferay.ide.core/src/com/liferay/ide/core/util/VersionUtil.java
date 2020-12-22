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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Seiphon Wang
 */
public class VersionUtil {

	public static List<String> convertToReleaseAPIVersions(String targetPlatformVersion, String productKey) {
		List<String> possibleVersions = new ArrayList<>();

		String[] tpSegments = targetPlatformVersion.split("\\.");

		String postfix = productKey.substring(productKey.lastIndexOf("-"));

		StringBuilder sb = new StringBuilder();

		sb.append(tpSegments[0]);
		sb.append('.');
		sb.append(tpSegments[1]);
		sb.append('.');

		if (productKey.startsWith("dxp")) {
			if (tpSegments.length > 3) {
				sb.append(tpSegments[2]);

				if (postfix.contains("ga") || postfix.contains("ep")) {
					sb.append(postfix);

					possibleVersions.add(sb.toString());

					if (tpSegments[3].contains("-")) {
						String[] segments = tpSegments[3].split("-");

						sb.append("-");
						sb.append(segments[1]);

						possibleVersions.add(sb.toString());
					}
				}
				else {
					sb.append(postfix.replaceAll("-", "."));

					possibleVersions.add(sb.toString());

					if (tpSegments[3].contains("-")) {
						String[] segments = tpSegments[3].split("-");

						sb.append("-");
						sb.append(segments[1]);

						possibleVersions.add(sb.toString());
					}
				}
			}
			else {
				sb.append(tpSegments[2]);

				possibleVersions.add(sb.toString());
			}
		}
		else if (productKey.startsWith("portal")) {
			if (tpSegments[2].contains("-")) {
				String[] segments = tpSegments[2].split("-");

				sb.append(segments[0]);

				sb.append(postfix);

				possibleVersions.add(sb.toString());

				sb.append("-");
				sb.append(segments[1]);

				possibleVersions.add(sb.toString());
			}
			else {
				sb.append(tpSegments[2]);
				sb.append(postfix);

				possibleVersions.add(sb.toString());
			}
		}

		return possibleVersions;
	}

	public static String simplifyTargetPlatformVersion(String targetPlatformVersion) {
		if (targetPlatformVersion == null) {
			return null;
		}

		String[] segments = targetPlatformVersion.split("\\.");

		StringBuilder sb = new StringBuilder();

		sb.append(segments[0]);
		sb.append('.');
		sb.append(segments[1]);
		sb.append('.');

		String micro = segments[2];

		int dashPosition = micro.indexOf("-");

		if (dashPosition > 0) {
			sb.append(micro.substring(0, dashPosition));

			if (segments.length == 3) {
				sb.append(".");
				sb.append(micro.substring(dashPosition + 1));
			}
		}
		else {
			sb.append(micro);
		}

		if (segments.length > 3) {
			sb.append(".");
			String qualifier = segments[3];

			Matcher matcher = _microPattern.matcher(qualifier);

			if (matcher.matches() && (matcher.groupCount() >= 5)) {
				qualifier = matcher.group(5);
			}

			if (CoreUtil.isNotNullOrEmpty(qualifier)) {
				sb.append(qualifier);
			}
		}

		return sb.toString();
	}

	private static final Pattern _microPattern = Pattern.compile("(((e|f|s)p)|(ga))([0-9]+)(-[0-9]+)?");

}