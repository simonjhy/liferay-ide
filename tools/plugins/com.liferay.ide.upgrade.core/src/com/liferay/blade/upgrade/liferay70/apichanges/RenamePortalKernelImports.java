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

package com.liferay.blade.upgrade.liferay70.apichanges;

import com.liferay.blade.api.AutoMigrator;
import com.liferay.blade.api.FileMigrator;
import com.liferay.blade.api.JavaFile;
import com.liferay.blade.api.SearchResult;
import com.liferay.blade.upgrade.liferay70.ImportStatementMigrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Gregory Amerson
 */
@Component(property = {
	"file.extensions=java,jsp,jspf",
	"problem.summary=The portal-kernel and portal-impl folders have many packages with the same name. Therefore, al" +
		"l of these packages are affected by the split package problem",
	"problem.tickets=LPS-61952", "problem.title=Renamed Packages to Fix the Split Packages Problem",
	"problem.section=#renamed-packages-to-fix-the-split-packages-problem", "auto.correct=import",
	"implName=RenamePortalKernelImports"
},
	service = {AutoMigrator.class, FileMigrator.class})
public class RenamePortalKernelImports extends ImportStatementMigrator {

	static {
		_imports = _getImports();
	}
	
	private static Map<String, String> _imports;
	// In my code I didn't handle the following package or class as I didn't
	// find them in
	// 6.2.x portal-service source or they are wrong.
	// Keep them here for future research to make sure need them or not at all.

	// private static final String[] IMPORTS = {
	// "com.liferay.portal.exception",
	// "com.liferay.portal.jdbc.pool.metrics",
	// "com.liferay.portal.mail",
	// "com.liferay.portal.model.adapter",
	// "com.liferay.portal.security.exportimport",
	// "com.liferay.portal.service.configuration",
	// "com.liferay.portal.service.http",
	// "com.liferay.portal.verify.model",
	// "com.liferay.portlet.admin.util",
	// "com.liferay.portlet.blogs.exception",
	// "com.liferay.portlet.exportimport",
	// "com.liferay.portlet.imagegallerydisplay.display.context",
	// "com.liferay.portlet.journal.util",
	// "com.liferay.portlet.messageboards.constants",
	// "com.liferay.portlet.messageboards.exception",
	// "com.liferay.portlet.useradmin.util",
	// "com.liferay.portlet.ratings.definition",
	// "com.liferay.portlet.ratings.display.context",
	// "com.liferay.portlet.ratings.exception",
	// "com.liferay.portlet.ratings.transformer"
	// };

	//

	// private static final String[] IMPORTS_FIXED = {
	// "com.liferay.portal.kernel.exception",
	// "com.liferay.portal.kernel.jdbc.pool.metrics",
	// "com.liferay.portal.kernel.mail",
	// "com.liferay.portal.kernel.model.adapter",
	// "com.liferay.portal.kernel.security.exportimport",
	// "com.liferay.portal.kernel.service.configuration",
	// "com.liferay.portal.kernel.service.http",
	// "com.liferay.portal.kernel.verify.model",
	// "com.liferay.admin.kernel.util",
	// "com.liferay.blogs.kernel.exception",
	// "com.liferay.exportimport.kernel",
	// "com.liferay.image.gallery.display.kernel.display.context",
	// "com.liferay.journal.kernel.util",
	// "com.liferay.message.boards.kernel.constants",
	// "com.liferay.message.boards.kernel.exception",
	// "com.liferay.users.admin.kernel.util",
	// "com.liferay.ratings.kernel.definition",
	// "com.liferay.ratings.kernel.display.context",
	// "com.liferay.ratings.kernel.exception",
	// "com.liferay.ratings.kernel.transformer"
	// };

	public static String[] getFixedImports(String[][] packageChangeMap) {
		String[] newImports = new String[packageChangeMap.length];

		for (int i = 0; i < packageChangeMap.length; i++) {
			newImports[i] = packageChangeMap[i][1];
		}

		return newImports;
	}

	public static String[] getOldImports(String[][] packageChangeMap) {
		String[] oldImports = new String[packageChangeMap.length];

		for (int i = 0; i < packageChangeMap.length; i++) {
			oldImports[i] = packageChangeMap[i][0];
		}

		return oldImports;
	}

	public static String[][] readCSV() {
		try (InputStream in = RenamePortalKernelImports.class.getResourceAsStream(
				"/com/liferay/blade/upgrade/liferay70/apichanges/kernel-rename.csv")) {

			List<String> lines = new ArrayList<>();

			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in))) {
				String line;

				while ((line = bufferedReader.readLine()) != null) {
					StringBuffer contents = new StringBuffer(line);

					lines.add(contents.toString());
				}
			}
			catch (Exception e) {
			}

			String[] lineArray = lines.toArray(new String[lines.size()]);

			String[][] results = new String[lineArray.length][2];

			for (int i = 0; i < lineArray.length; i++) {
				String line = lineArray[i];

				String[] columns = line.split(",");

				results[i][0] = columns[0];
				results[i][1] = columns[1];
			}

			return results;
		}
		catch (IOException ioe) {
		}

		return null;
	}

	public RenamePortalKernelImports() {
		super(_imports);
	}

	@Override
	public List<SearchResult> searchFile(File file, JavaFile javaFile) {
		List<SearchResult> searchResults = new ArrayList<>();

		List<SearchResult> importResult = javaFile.findImports(_imports.keySet().toArray(new String[0]));

		if (!importResult.isEmpty()) {
			for (SearchResult result : importResult) {

				// make sure that our import is not in list of fixed imports

				boolean skip = false;

				if (result.searchContext != null) {
					for (String fixed : _imports.values().toArray(new String[0])) {
						if (result.searchContext.contains(fixed)) {
							skip = true;
							break;
						}
					}
				}

				if (!skip) {
					result.autoCorrectContext = getPrefix() + _getImportNameFromResult(result);
					searchResults.add(result);
				}
			}
		}

		return _removeDuplicate(searchResults);
	}

	private static Map<String, String> _getImports() {
		Map<String, String> imports = new HashMap<>();

		String[][] csv = readCSV();

		String[] oldImports = getOldImports(csv);
		String[] fixedImports = getFixedImports(csv);

		for (int i = 0; i < oldImports.length; i++) {
			imports.put(oldImports[i], fixedImports[i]);
		}

		return imports;
	}

	private String _getImportNameFromResult(SearchResult result) {
		String searchContext = result.searchContext;

		if (searchContext != null) {
			int offSet = result.endOffset - result.startOffset;

			return searchContext.substring(0, offSet);
		}

		return "";
	}

	private List<SearchResult> _removeDuplicate(List<SearchResult> searchResults) {
		List<SearchResult> newList = new ArrayList<>();

		for (SearchResult searchResult : searchResults) {
			if (!newList.contains(searchResult)) {
				newList.add(searchResult);
			}
		}

		return newList;
	}

}