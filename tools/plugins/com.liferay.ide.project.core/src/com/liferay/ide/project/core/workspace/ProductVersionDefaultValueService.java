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

import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.core.util.SapphireContentAccessor;
import com.liferay.ide.core.util.SapphireUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.WorkspaceProductInfo;
import com.liferay.ide.project.core.modules.BladeCLI;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sapphire.DefaultValueService;
import org.eclipse.sapphire.FilteredListener;
import org.eclipse.sapphire.PropertyContentEvent;

/**
 * @author Ethan Sun
 */
public class ProductVersionDefaultValueService extends DefaultValueService implements SapphireContentAccessor {

	@Override
	public void dispose() {
		if (_op != null) {
			SapphireUtil.detachListener(_op.property(NewLiferayWorkspaceOp.PROP_PRODUCT_CATEGORY), _listener);
			SapphireUtil.detachListener(_op.property(NewLiferayWorkspaceOp.PROP_SHOW_ALL_VERSION_PRODUCT), _listener);
		}

		super.dispose();
	}

	@Override
	protected String compute() {
		if (ListUtil.isEmpty(_workspaceProducts)) {
			return null;
		}

		String category = get(_op.getProductCategory());

		List<String> productVersionsList = NewLiferayWorkspaceOpMethods.getProductVersionList(
			category, _workspaceProducts);

		return productVersionsList.get(productVersionsList.size() - 1);
	}

	@Override
	protected void initDefaultValueService() {
		_op = context(NewLiferayWorkspaceOp.class);


		_listener = new FilteredListener<PropertyContentEvent>() {
			@Override
			protected void handleTypedEvent(PropertyContentEvent event) {
				refresh();
			}
		};
		
		SapphireUtil.attachListener(_op.property(NewLiferayWorkspaceOp.PROP_PRODUCT_CATEGORY), _listener);
		SapphireUtil.attachListener(_op.property(NewLiferayWorkspaceOp.PROP_SHOW_ALL_VERSION_PRODUCT), _listener);
		
		if (FileUtil.notExists(WorkspaceProductInfo.workspaceCacheFile)){
			Job refreshWorkspaceProductJob = new Job("") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						boolean showAll = get(_op.getShowAllVersionProduct());

						_workspaceProducts = BladeCLI.getWorkspaceProduct(showAll);

						refresh();
					}
					catch (Exception exception) {
						ProjectCore.logError("Failed to init workspace product default value", exception);
					}

					return Status.OK_STATUS;
				}

			};

			refreshWorkspaceProductJob.setSystem(true);

			refreshWorkspaceProductJob.schedule();
		}
	}

	private FilteredListener<PropertyContentEvent> _listener;
	private NewLiferayWorkspaceOp _op;
	private String[] _workspaceProducts;

}