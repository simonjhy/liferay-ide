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

package com.liferay.ide.project.core.jsf;

import com.liferay.ide.core.util.SapphireContentAccessor;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.sapphire.PossibleValuesService;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Seiphon Wang
 */
public class JSFModuleProjectComponentSuitePossibleValues
	extends PossibleValuesService implements SapphireContentAccessor {

	@Override
	protected void compute(Set<String> values) {
		List<String> possibleValues = new ArrayList<>(
			Arrays.asList("jsf", "alloy", "icefaces", "primefaces", "richfaces", "butterfaces", "bootsfaces"));

		values.addAll(possibleValues);

		NewLiferayJSFModuleProjectOp op = _op();

		List<String> newPossibleValues = _getComponentSuiteFromURL(get(op.getLiferayVersion()));

		for (String value : newPossibleValues) {
			if (!possibleValues.contains(value)) {
				values.add(value);
			}
		}
	}

	private List<String> _getComponentSuiteFromURL(String liferayVersion) {
		List<String> possibleValues = new ArrayList<>();

		StringBuffer sb = new StringBuffer(
			"https://faces.liferay.dev/home/-/archetype-portlet/liferay-portal-version/");

		sb.append(liferayVersion);

		sb.append("/jsf-version/2.2/component-suite/jsf/build-tool/maven");

		Connection connection = Jsoup.connect(sb.toString());

		try {
			Document document = connection.get();

			Element select = document.getElementById(_select_element_id);

			Elements options = select.getElementsByTag("option");

			for (Element option : options) {
				String value = option.attr("value");

				possibleValues.add(value);
			}
		}
		catch (IOException ioe) {
			return Collections.emptyList();
		}

		return possibleValues;
	}

	private NewLiferayJSFModuleProjectOp _op() {
		return context(NewLiferayJSFModuleProjectOp.class);
	}

	private String _select_element_id =
		"_1_WAR_comliferayfacessitearchetypeportlet_:form:suite:archetypeSelector_suite";

}