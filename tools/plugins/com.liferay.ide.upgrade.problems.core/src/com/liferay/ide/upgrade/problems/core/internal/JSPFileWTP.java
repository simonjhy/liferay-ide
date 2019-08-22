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

package com.liferay.ide.upgrade.problems.core.internal;

import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.upgrade.problems.core.CUCache;
import com.liferay.ide.upgrade.problems.core.FileSearchResult;
import com.liferay.ide.upgrade.problems.core.JSPFile;
import com.liferay.ide.upgrade.problems.core.JavaFile;

import java.io.File;
import java.io.InputStream;

import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * @author Gregory Amerson
 * @author Simon Jiang
 */
@Component(property = "file.extension=jsp", service = {JavaFile.class, JSPFile.class})
@SuppressWarnings({"rawtypes", "restriction"})
public class JSPFileWTP extends JavaFileJDT implements JSPFile {

	public JSPFileWTP() {
	}

	public JSPFileWTP(File file) {
		super(file);
	}

	@Override
	public List<FileSearchResult> findJSPTags(String tagName) {
		if ((tagName == null) || tagName.isEmpty()) {
			throw new IllegalArgumentException("tagName can not be null or empty");
		}

		List<FileSearchResult> searchResults = new ArrayList<>();

		NodeList nodeList = _getTagNodes(tagName);

		for (int i = 0; i < nodeList.getLength(); i++) {
			IDOMNode domNode = (IDOMNode)nodeList.item(i);

			int startOffset = domNode.getStartOffset();
			int endOffset = domNode.getEndOffset();

			int jspStartLine = _getJspLine(startOffset);
			int jspEndLine = _getJspLine(endOffset);

			searchResults.add(super.createSearchResult(null, startOffset, endOffset, jspStartLine, jspEndLine, true));
		}

		return searchResults;
	}

	@Override
	public List<FileSearchResult> findJSPTags(String tagName, String[] attrNames) {
		if ((tagName == null) || tagName.isEmpty() || ListUtil.isEmpty(attrNames)) {
			throw new IllegalArgumentException("tagName can not be null or empty");
		}

		List<FileSearchResult> searchResults = new ArrayList<>();

		NodeList nodeList = _getTagNodes(tagName);

		for (int i = 0; i < nodeList.getLength(); i++) {
			IDOMNode domNode = (IDOMNode)nodeList.item(i);

			for (String attrName : attrNames) {
				NamedNodeMap atrributes = domNode.getAttributes();

				IDOMNode attrNode = (IDOMNode)atrributes.getNamedItem(attrName);

				if (attrNode != null) {
					int startOffset = attrNode.getStartOffset();

					int endOffset = startOffset + attrName.length();
					int jspStartLine = _getJspLine(startOffset);

					int jspEndLine = _getJspLine(endOffset);

					searchResults.add(
						super.createSearchResult(null, startOffset, endOffset, jspStartLine, jspEndLine, true));
				}
			}
		}

		return searchResults;
	}

	@Override
	public List<FileSearchResult> findJSPTags(String tagName, String[] attrNames, String[] attrValues) {
		if ((tagName == null) || tagName.isEmpty() || ListUtil.isEmpty(attrNames) || ListUtil.isEmpty(attrValues)) {
			throw new IllegalArgumentException("tagName can not be null or empty");
		}

		if (attrNames.length != attrValues.length) {
			throw new IllegalArgumentException("If attrValues is specified it must match the attrNames array in lengh");
		}

		List<FileSearchResult> searchResults = new ArrayList<>();

		NodeList nodeList = _getTagNodes(tagName);

		for (int i = 0; i < nodeList.getLength(); i++) {
			IDOMNode domNode = (IDOMNode)nodeList.item(i);

			for (int j = 0; j < attrNames.length; j++) {
				NamedNodeMap atrributes = domNode.getAttributes();

				IDOMNode attrNode = (IDOMNode)atrributes.getNamedItem(attrNames[j]);

				if (attrNode != null) {
					if ((attrValues != null) && !attrValues[j].equals(attrNode.getNodeValue())) {
						continue;
					}

					int startOffset = attrNode.getStartOffset() + attrNames[j].length() + 2;

					int endOffset = startOffset + attrValues[j].length();
					int jspStartLine = _getJspLine(startOffset);

					int jspEndLine = _getJspLine(endOffset);

					FileSearchResult searchResult = super.createSearchResult(
						null, startOffset, endOffset, jspStartLine, jspEndLine, true);

					searchResults.add(searchResult);
				}
			}
		}

		return searchResults;
	}

	@Override
	public void setFile(File file) {
		try {
			Bundle bundle = FrameworkUtil.getBundle(getClass());

			BundleContext context = bundle.getBundleContext();

			Collection<ServiceReference<CUCache>> sr = context.getServiceReferences(CUCache.class, "(type=jsp)");

			Iterator<ServiceReference<CUCache>> iterator = sr.iterator();

			ServiceReference<CUCache> ref = iterator.next();

			@SuppressWarnings("unchecked")
			CUCache<JSPTranslationPrime> cache = context.getService(ref);

			_translation = cache.getCU(file, null);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

		super.setFile(file);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected FileSearchResult createSearchResult(
		String searchContext, int startOffset, int endOffset, int startLine, int endLine, boolean fullMatch) {

		IDOMModel jspModel = null;

		try {
			int jspStartOffset = _translation.getJspOffset(startOffset);
			int jspEndOffset = _translation.getJspOffset(endOffset);

			File jspFile = _translation.getJspFile();

			IModelManager modelManager = StructuredModelManager.getModelManager();

			try (InputStream input = FileUtil.newInputStream(Paths.get(jspFile.toURI()))) {
				jspModel = (IDOMModel)modelManager.getModelForRead(jspFile.getAbsolutePath(), input, null);
			}

			IDOMDocument domDocument = jspModel.getDocument();

			IStructuredDocument structuredDocument = domDocument.getStructuredDocument();

			int jspStartLine = structuredDocument.getLineOfOffset(jspStartOffset) + 1;
			int jspEndLine = structuredDocument.getLineOfOffset(jspEndOffset) + 1;

			return super.createSearchResult(
				searchContext, jspStartOffset, jspEndOffset, jspStartLine, jspEndLine, fullMatch);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (jspModel != null) {
				jspModel.releaseFromRead();
			}
		}

		return super.createSearchResult(searchContext, startOffset, endOffset, startLine, endLine, fullMatch);
	}

	@Override
	protected char[] getJavaSource() {
		String text = _translation.getJavaText();

		return text.toCharArray();
	}

	@SuppressWarnings("deprecation")
	private int _getJspLine(int offset) {
		IDOMModel jspModel = null;

		try {
			File jspFile = _translation.getJspFile();

			IModelManager modelManager = StructuredModelManager.getModelManager();

			try (InputStream input = FileUtil.newInputStream(Paths.get(jspFile.toURI()))) {
				jspModel = (IDOMModel)modelManager.getModelForRead(jspFile.getAbsolutePath(), input, null);
			}

			IDOMDocument domDocument = jspModel.getDocument();

			IStructuredDocument structuredDocument = domDocument.getStructuredDocument();

			return structuredDocument.getLineOfOffset(offset) + 1;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (jspModel != null) {
				jspModel.releaseFromRead();
			}
		}

		return 0;
	}

	@SuppressWarnings("deprecation")
	private NodeList _getTagNodes(String tagName) {
		IDOMModel jspModel = null;

		try {
			File jspFile = _translation.getJspFile();

			IModelManager modelManager = StructuredModelManager.getModelManager();

			try (InputStream input = FileUtil.newInputStream(Paths.get(jspFile.toURI()))) {
				jspModel = (IDOMModel)modelManager.getModelForRead(jspFile.getAbsolutePath(), input, null);
			}

			IDOMDocument domDocument = jspModel.getDocument();

			return domDocument.getElementsByTagName(tagName);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (jspModel != null) {
				jspModel.releaseFromRead();
			}
		}

		return null;
	}

	private JSPTranslationPrime _translation;

}