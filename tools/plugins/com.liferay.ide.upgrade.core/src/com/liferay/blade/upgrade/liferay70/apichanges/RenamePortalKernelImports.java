/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

@Component(
	property = {
		"file.extensions=java,jsp,jspf",
		"problem.summary=The portal-kernel and portal-impl folders have many packages with the same name. Therefore, all of these packages are affected by the split package problem",
		"problem.tickets=LPS-61952",
		"problem.title=Renamed Packages to Fix the Split Packages Problem",
		"problem.section=#renamed-packages-to-fix-the-split-packages-problem",
		"auto.correct=import",
		"implName=RenamePortalKernelImports"
	},
	service = {
		AutoMigrator.class,
		FileMigrator.class
	}
)
public class RenamePortalKernelImports extends ImportStatementMigrator {

	public RenamePortalKernelImports() {
		super(getImports());
	}

	private static Map<String, String> getImports() {
		Map<String, String> imports = new HashMap<>();

		String[][] csv = _readCSV();

		String[] oldImports = getOldImports(csv);
		String[] fixedImports = getFixedImports(csv);

		for (int i = 0; i < oldImports.length; i++) {
			imports.put(oldImports[i], fixedImports[i]);
		}

		return imports;
	}

	public static String[] getOldImports(String[][] packageChangeMap) {
		String[] _oldImports = new String[packageChangeMap.length];

		for (int i = 0; i < packageChangeMap.length; i++) {
			_oldImports[i] = packageChangeMap[i][0];
		}

		return _oldImports;
	}

	public static String[] getFixedImports(String[][] packageChangeMap) {
		String[] _newImports = new String[packageChangeMap.length];

		for (int i = 0; i < packageChangeMap.length; i++) {
			_newImports[i] = packageChangeMap[i][1];
		}

		return _newImports;
	}

	public static String[][] _readCSV() {
		try (InputStream in = RenamePortalKernelImports.class.getResourceAsStream("/com/liferay/blade/upgrade/liferay70/apichanges/kernel-rename.csv")) {
			List<String> lines = new ArrayList<>();

			try (BufferedReader bufferedReader =
					new BufferedReader(new InputStreamReader(in))) {

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

		} catch (IOException e) {
		}

		return null;
	}

	@Override
	public List<SearchResult> searchFile(File file, JavaFile javaFile) {
		final List<SearchResult> searchResults = new ArrayList<>();

		final List<SearchResult> importResult = javaFile.findImports(_importFixes.keySet().toArray(new String[0]));

		if (importResult.size() != 0) {
		    String[] fixedImportArray = _importFixes.values().toArray(new String[0]);
		    
			for (SearchResult result : importResult) {
				// make sure that our import is not in list of fixed imports
				boolean skip = false;

				if (result.searchContext != null) {
					for (String fixed : fixedImportArray) {
						if (result.searchContext.contains(fixed)) {
							skip = true;
							break;
						}
					}
				}

				if (!skip) {
					result.autoCorrectContext = getPrefix() + getImportNameFromResult(result);
					searchResults.add(result);
				}
			}
		}

		return removeDuplicate(searchResults);
	}

	private List<SearchResult> removeDuplicate(List<SearchResult> searchResults) {
		final List<SearchResult> newList = new ArrayList<>();

		for (SearchResult searchResult : searchResults) {
			if (!newList.contains(searchResult)) {
				newList.add(searchResult);
			}
		}

		return newList;
	}

	private String getImportNameFromResult(SearchResult result) {
		String searchContext = result.searchContext;

		if (searchContext != null) {
			int offSet = result.endOffset - result.startOffset;
			return searchContext.substring(0, offSet);

		}

		return "";
	}

}