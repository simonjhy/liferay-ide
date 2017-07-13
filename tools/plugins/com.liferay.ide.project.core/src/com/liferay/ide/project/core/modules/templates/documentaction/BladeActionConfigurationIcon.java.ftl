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

package ${packagename};

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.portlet.configuration.icon.BasePortletConfigurationIcon;
import com.liferay.portal.kernel.portlet.configuration.icon.PortletConfigurationIcon;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;
import javax.portlet.WindowStateException;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Adds the new context menu option to the Document Detail screen options (top
 * right corner) of the Documents and Media Admin portlet.
 *
 * @author liferay
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=com_liferay_document_library_web_portlet_DLAdminPortlet",
		"path=/document_library/view_file_entry"
	},
	service = PortletConfigurationIcon.class
)
public class ${classname} extends BasePortletConfigurationIcon {

	public String getMessage(PortletRequest portletRequest) {
		return "Blade Basic Info";
	}

	public String getURL(
		PortletRequest portletRequest, PortletResponse portletResponse) {

		HttpServletRequest servletRequest = _portal.getHttpServletRequest(
			portletRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		FileEntry fileEntry = _retrieveFile(servletRequest);

		PortletURL portletURL = PortletURLFactoryUtil.create(
			servletRequest,
			"blade_document_action_portlet_BladeDocumentActionPortlet",
			themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

		String fileName = fileEntry.getFileName();
		String mimeType = fileEntry.getMimeType();
		String version = fileEntry.getVersion();
		String createdDate = fileEntry.getCreateDate().toString();
		String createdUserName = fileEntry.getUserName();
		String statusLabel = null;

		try {
			statusLabel = WorkflowConstants.getStatusLabel(
				fileEntry.getLatestFileVersion().getStatus());
		}
		catch (PortalException pe) {
			_log.error(pe);
		}

		portletURL.setParameter("fileName", fileName);
		portletURL.setParameter("mimeType", mimeType);
		portletURL.setParameter("version", version);
		portletURL.setParameter("statusLabel", statusLabel);
		portletURL.setParameter("createdDate", createdDate);
		portletURL.setParameter("createdUserName", createdUserName);

		try {
			portletURL.setWindowState(LiferayWindowState.POP_UP);
		}
		catch (WindowStateException wse) {
			_log.error(wse);
		}

		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("javascript:Liferay.Util.openWindow(");
		stringBuilder.append("{dialog: {cache: false,width:800,modal: true},");
		stringBuilder.append("title: 'basic information',id: ");
		stringBuilder.append("'testPopupIdUnique',uri: '");
		stringBuilder.append(portletURL.toString() + "'});");

		return stringBuilder.toString();
	}

	public boolean isShow(PortletRequest portletRequest) {
		return true;
	}

	private FileEntry _retrieveFile(HttpServletRequest request) {
		try {
			long fileEntryId = ParamUtil.getLong(request, "fileEntryId");

			FileEntry fileEntry = null;

			if (fileEntryId > 0) {
				fileEntry = _dlAppService.getFileEntry(fileEntryId);
			}

			if (fileEntry == null) {
				return null;
			}

			String cmd = ParamUtil.getString(request, Constants.CMD);

			if (fileEntry.isInTrash() &&
				!cmd.equals(Constants.MOVE_FROM_TRASH)) {

				return null;
			}

			return fileEntry;
		}
		catch (PortalException pe) {
			_log.error(pe);
			return null;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		${classname}.class);

	@Reference
	private DLAppService _dlAppService;

	@Reference
	private Portal _portal;

}