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

package com.liferay.ide.server.tomcat.core;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileListing;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.core.portal.AbstractPortalBundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Gregory Amerson
 * @author Simon Jiang
 * @author Terry Jia
 */
public class PortalTomcatBundle extends AbstractPortalBundle {

	public PortalTomcatBundle(IPath path) {
		super(path);
	}

	public PortalTomcatBundle(Map<String, String> appServerProperties) {
		super(appServerProperties);
	}

	@Override
	public IPath getAppServerDeployDir() {
		return getAppServerDir().append("webapps");
	}

	@Override
	public IPath getAppServerLibGlobalDir() {
		return getAppServerDir().append("/lib/ext");
	}

	@Override
	public IPath getAppServerPortalDir() {
		IPath retval = null;

		if (bundlePath != null) {
			retval = bundlePath.append("webapps/ROOT");
		}

		return retval;
	}

	@Override
	public String getDisplayName() {
		return "Tomcat";
	}

	@Override
	public String getHttpPort() {
		String retVal = "8080";

		File serverXmlFile = new File(getAppServerDir().toPortableString(), "conf/server.xml");

		String portValue = getHttpPortValue(serverXmlFile, "Connector", "protocol", "HTTP/1.1", "port");

		if (!CoreUtil.empty(portValue)) {
			return portValue;
		}

		return retVal;
	}

	@Override
	public String getMainClass() {
		return "org.apache.catalina.startup.Bootstrap";
	}

	@Override
	public IPath[] getRuntimeClasspath() {
		List<IPath> paths = new ArrayList<>();

		IPath binPath = bundlePath.append("bin");

		if (FileUtil.exists(binPath)) {
			paths.add(binPath.append("bootstrap.jar"));

			IPath juli = binPath.append("tomcat-juli.jar");

			if (FileUtil.exists(juli)) {
				paths.add(juli);
			}
		}

		return paths.toArray(new IPath[0]);
	}

	@Override
	public String[] getRuntimeStartProgArgs() {
		String[] retval = new String[1];

		retval[0] = "start";

		return retval;
	}

	@Override
	public String[] getRuntimeStartVMArgs() {
		return _getRuntimeVMArgs();
	}

	@Override
	public String[] getRuntimeStopProgArgs() {
		String[] retval = new String[1];

		retval[0] = "stop";

		return retval;
	}

	@Override
	public String[] getRuntimeStopVMArgs() {
		return _getRuntimeVMArgs();
	}

	@Override
	public String getType() {
		return "tomcat";
	}

	@Override
	public IPath[] getUserLibs() {
		List<IPath> libs = new ArrayList<>();

		try {
			IPath libDir = getAppServerPortalDir().append("WEB-INF/lib");

			List<File> portallibFiles = FileListing.getFileListing(new File(libDir.toPortableString()));

			for (File lib : portallibFiles) {
				if (FileUtil.exists(lib)) {
					String libName = lib.getName();

					if (libName.endsWith(".jar")) {
						libs.add(new Path(lib.getPath()));
					}
				}
			}

			List<File> libFiles = FileListing.getFileListing(new File(getAppServerLibDir().toPortableString()));

			for (File lib : libFiles) {
				if (FileUtil.exists(lib)) {
					String libName = lib.getName();

					if (libName.endsWith(".jar")) {
						libs.add(new Path(lib.getPath()));
					}
				}
			}

			List<File> extlibFiles = FileListing.getFileListing(
				new File(getAppServerLibGlobalDir().toPortableString()));

			for (File lib : extlibFiles) {
				if (FileUtil.exists(lib)) {
					String libName = lib.getName();

					if (libName.endsWith(".jar")) {
						libs.add(new Path(lib.getPath()));
					}
				}
			}
		}
		catch (FileNotFoundException fnfe) {
		}

		return libs.toArray(new IPath[libs.size()]);
	}

	@Override
	public void setHttpPort(String port) {
		_setHttpPortValue(
			new File(getAppServerDir().toPortableString(), "conf/server.xml"), "Connector", "protocol", "HTTP/1.1",
			"port", port);
	}

	@Override
	protected IPath getAppServerLibDir() {
		return getAppServerDir().append("lib");
	}

	@Override
	protected int getDefaultJMXRemotePort() {
		int retval = 8099;

		IPath setenv = bundlePath.append("bin/setenv." + _getShellExtension());

		String contents = FileUtil.readContents(setenv.toFile());

		String port = null;

		if (contents != null) {
			_pattern = Pattern.compile(".*-Dcom.sun.management.jmxremote.port(\\s*)=(\\s*)([0-9]+).*");

			_matcher = _pattern.matcher(contents);

			if (_matcher.matches()) {
				port = _matcher.group(3);
			}
		}

		if (port != null) {
			try {
				retval = Integer.parseInt(port);
			}
			catch (NumberFormatException nfe) {
			}
		}

		return retval;
	}

	private String[] _getRuntimeVMArgs() {
		List<String> args = new ArrayList<>();
		IPath tempPath = bundlePath.append("temp");
		IPath endorsedPath = bundlePath.append("endorsed");

		args.add("-Dcatalina.base=\"" + bundlePath.toPortableString() + "\"");
		args.add("-Dcatalina.home=\"" + bundlePath.toPortableString() + "\"");
		args.add("-Dcom.sun.management.jmxremote");
		args.add("-Dcom.sun.management.jmxremote.authenticate=false");
		args.add("-Dcom.sun.management.jmxremote.port=" + getJmxRemotePort());
		args.add("-Dcom.sun.management.jmxremote.ssl=false");
		args.add("-Dfile.encoding=UTF8");
		args.add("-Djava.endorsed.dirs=\"" + endorsedPath.toPortableString() + "\"");
		args.add("-Djava.io.tmpdir=\"" + tempPath.toPortableString() + "\"");
		args.add("-Djava.net.preferIPv4Stack=true");
		args.add("-Djava.util.logging.config.file=\"" + bundlePath.append("conf/logging.properties") + "\"");
		args.add("-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager");
		args.add("-Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false");
		args.add("-Duser.timezone=GMT");

		return args.toArray(new String[0]);
	}

	private String _getShellExtension() {
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			return "bat";
		}

		return "sh";
	}

	private void _setHttpPortValue(
		File xmlFile, String tagName, String attriName, String attriValue, String targetName, String value) {

		DocumentBuilder db = null;

		DocumentBuilderFactory dbf = null;

		try {
			dbf = DocumentBuilderFactory.newInstance();

			db = dbf.newDocumentBuilder();

			Document document = db.parse(xmlFile);

			NodeList connectorNodes = document.getElementsByTagName(tagName);

			for (int i = 0; i < connectorNodes.getLength(); i++) {
				Node node = connectorNodes.item(i);

				NamedNodeMap attributes = node.getAttributes();

				Node protocolNode = attributes.getNamedItem(attriName);

				if (protocolNode != null) {
					String nodeName = protocolNode.getNodeValue();

					if (nodeName.equals(attriValue)) {
						Node portNode = attributes.getNamedItem(targetName);

						portNode.setNodeValue(value);

						break;
					}
				}
			}

			TransformerFactory factory = TransformerFactory.newInstance();

			Transformer transformer = factory.newTransformer();

			DOMSource domSource = new DOMSource(document);

			StreamResult result = new StreamResult(xmlFile);

			transformer.transform(domSource, result);
		}
		catch (Exception e) {
			LiferayServerCore.logError(e);
		}
	}

	private Matcher _matcher;
	private Pattern _pattern;


	@Override
	public IPath getApperServerLog() {
		return getAppServerDir().append("logs").append("catalina.out");
	}

	@Override
	public IPath getPortalBundleLog() {
		return null;
	}

}