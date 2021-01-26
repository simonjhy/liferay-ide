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

import com.liferay.ide.core.LiferayCore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * @author Seiphon Wang
 */
public class HttpUtil {

	public static String createToken(URI uri, String userName, String password) throws IOException {
		try (CloseableHttpClient closeableHttpClient = _getHttpClient(uri, userName, password, -1)) {
			HttpPost httpPost = new HttpPost(uri);

			InetAddress inetAddress = InetAddress.getLocalHost();

			NameValuePair deviceNameValuePair = new BasicNameValuePair(
				"device", "portal-tools-bundle-support-" + inetAddress.getHostName());

			httpPost.setEntity(new UrlEncodedFormEntity(Collections.singleton(deviceNameValuePair)));

			try (CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpPost)) {
				_checkResponseStatus(closeableHttpResponse);

				HttpEntity httpEntity = closeableHttpResponse.getEntity();

				String json = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);

				int start = json.indexOf("token\":") + 7;

				start = json.indexOf('"', start) + 1;

				int end = json.indexOf('"', start);

				return json.substring(start, end);
			}
		}
	}

	public static void download(URI uri, Path cacheDirPath, boolean showProgress) throws Exception {
		download(uri, null, null, cacheDirPath, showProgress);
	}

	public static void download(URI uri, String userName, String password, Path cacheDirPath, boolean showProgress)
		throws Exception {

		downloadFile(uri, userName, password, cacheDirPath, showProgress);
	}

	public static Path downloadFile(URI uri, String token, Path cacheDirPath, boolean showProgress) throws Exception {
		return downloadFile(uri, token, cacheDirPath, -1, showProgress);
	}

	public static Path downloadFile(
			URI uri, String token, Path cacheDirPath, int connectionTimeout, boolean showProgress)
		throws Exception {

		Path path;

		try (CloseableHttpClient closeableHttpClient = _getHttpClient(uri, token, connectionTimeout)) {
			path = _downloadFile(closeableHttpClient, uri, cacheDirPath, showProgress);
		}

		return path;
	}

	public static Path downloadFile(URI uri, String userName, String password, Path cacheDirPath, boolean showProgress)
		throws Exception {

		return downloadFile(uri, userName, password, cacheDirPath, -1, showProgress);
	}

	public static Path downloadFile(
			URI uri, String userName, String password, Path cacheDirPath, int connectionTimeout, boolean showProgress)
		throws Exception {

		Path path;

		try (CloseableHttpClient closeableHttpClient = _getHttpClient(uri, userName, password, connectionTimeout)) {
			path = _downloadFile(closeableHttpClient, uri, cacheDirPath, showProgress);
		}

		return path;
	}

	public static int getResponseStatus(URI uri, String userName, String password, int connectionTimeout)
		throws ClientProtocolException, IOException {

		int statusCode = HttpStatus.SC_NOT_FOUND;

		HttpHead httpHead = new HttpHead(uri);

		HttpContext httpContext = new BasicHttpContext();

		try (CloseableHttpClient closeableHttpClient = _getHttpClient(uri, userName, password, connectionTimeout);
			CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpHead, httpContext)) {

			StatusLine statusLine = closeableHttpResponse.getStatusLine();

			statusCode = statusLine.getStatusCode();
		}

		return statusCode;
	}

	private static void _checkResponseStatus(HttpResponse httpResponse) throws IOException {
		StatusLine statusLine = httpResponse.getStatusLine();

		if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
			throw new IOException(statusLine.getReasonPhrase());
		}
	}

	private static Path _downloadFile(
			CloseableHttpClient closeableHttpClient, URI uri, Path cacheDirPath, boolean showProgress)
		throws Exception {

		HttpHead httpHead = new HttpHead(uri);

		HttpClientContext httpContext = HttpClientContext.create();

		String fileName = null;

		Date lastModifiedDate;

		try (CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpHead, httpContext)) {
			_checkResponseStatus(closeableHttpResponse);

			Header dispositionHeader = closeableHttpResponse.getFirstHeader("Content-Disposition");

			if (dispositionHeader != null) {
				String dispositionValue = dispositionHeader.getValue();

				int index = dispositionValue.indexOf("filename=");

				if (index > 0) {
					fileName = dispositionValue.substring(index + "filename=".length());
				}
			}
			else {
				RedirectLocations redirectLocations = (RedirectLocations)httpContext.getAttribute(
					HttpClientContext.REDIRECT_LOCATIONS);

				if (redirectLocations != null) {
					uri = redirectLocations.get(redirectLocations.size() - 1);
				}
			}

			Header lastModifiedHeader = closeableHttpResponse.getFirstHeader(HttpHeaders.LAST_MODIFIED);

			if (lastModifiedHeader != null) {
				lastModifiedDate = DateUtils.parseDate(lastModifiedHeader.getValue());
			}
			else {
				lastModifiedDate = new Date();
			}
		}

		if (fileName == null) {
			String uriPath = uri.getPath();

			fileName = uriPath.substring(uriPath.lastIndexOf('/') + 1);
		}

		if (cacheDirPath == null) {
			cacheDirPath = Files.createTempDirectory(null);
		}

		Path path = cacheDirPath.resolve(fileName);

		if (Files.exists(path)) {
			FileTime fileTime = Files.getLastModifiedTime(path);

			if (fileTime.toMillis() == lastModifiedDate.getTime()) {
				return path;
			}

			Files.delete(path);
		}

		Files.createDirectories(cacheDirPath);

		HttpGet httpGet = new HttpGet(uri);

		try (CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpGet)) {
			_checkResponseStatus(closeableHttpResponse);

			HttpEntity httpEntity = closeableHttpResponse.getEntity();

			long length = httpEntity.getContentLength();

			LiferayCore.logInfo("Download " + uri);

			try (InputStream inputStream = httpEntity.getContent();
				OutputStream outputStream = Files.newOutputStream(path)) {

				byte[] buffer = new byte[10 * 1024];
				int completed = 0;
				int read = -1;

				while ((read = inputStream.read(buffer)) >= 0) {
					outputStream.write(buffer, 0, read);

					completed += read;

					if (showProgress) {
						_onDownloadProcess(completed, length);
					}
				}
			}
			finally {
				LiferayCore.logInfo("Download completed.");
			}
		}

		Files.setLastModifiedTime(path, FileTime.fromMillis(lastModifiedDate.getTime()));

		return path;
	}

	private static CloseableHttpClient _getHttpClient(URI uri, String token, int connectionTimeout) {
		HttpClientBuilder httpClientBuilder = _getHttpClientBuilder(uri, null, null, connectionTimeout);

		Header header = new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		httpClientBuilder.setDefaultHeaders(Collections.singleton(header));

		return httpClientBuilder.build();
	}

	private static CloseableHttpClient _getHttpClient(
		URI uri, String userName, String password, int connectionTimeout) {

		HttpClientBuilder httpClientBuilder = _getHttpClientBuilder(uri, userName, password, connectionTimeout);

		return httpClientBuilder.build();
	}

	private static HttpClientBuilder _getHttpClientBuilder(
		URI uri, String userName, String password, int connectionTimeout) {

		HttpClientBuilder httpClientBuilder = HttpClients.custom();

		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

		requestConfigBuilder.setConnectTimeout(connectionTimeout);
		requestConfigBuilder.setCookieSpec(CookieSpecs.STANDARD);
		requestConfigBuilder.setRedirectsEnabled(true);

		httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

		String scheme = uri.getScheme();

		String proxyHost = System.getProperty(scheme + ".proxyHost");
		String proxyPort = System.getProperty(scheme + ".proxyPort");

		if ((proxyHost != null) && (proxyPort != null)) {
			try {
				httpClientBuilder.setProxy(new HttpHost(proxyHost, Integer.parseInt(proxyPort)));

				String proxyUser = System.getProperty(scheme + ".proxyUser");
				String proxyPassword = System.getProperty(scheme + ".proxyPassword");

				if (!Objects.isNull(proxyUser) && !Objects.isNull(proxyPassword)) {
					credentialsProvider.setCredentials(
						new AuthScope(proxyHost, Integer.parseInt(proxyPort)),
						new UsernamePasswordCredentials(proxyUser, proxyPassword));
				}
			}
			catch (Exception e) {
				LiferayCore.logError("Failed to configure http builder", e);
			}
		}
		else {
			if (!Objects.isNull(userName) && !Objects.isNull(password)) {
				credentialsProvider.setCredentials(
					new AuthScope(uri.getHost(), uri.getPort()), new UsernamePasswordCredentials(userName, password));
			}
		}

		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

		httpClientBuilder.useSystemProperties();

		return httpClientBuilder;
	}

	private static void _onDownloadProcess(long completed, long length) {
		StringBuilder sb = new StringBuilder();

		sb.append(FileUtil.getFileLength(completed));

		if (length > 0) {
			sb.append('/');
			sb.append(FileUtil.getFileLength(length));
		}

		sb.append(" downloaded");

		LiferayCore.logInfo(sb.toString());
	}

}