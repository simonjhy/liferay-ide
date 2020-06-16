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

import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.core.util.SapphireContentAccessor;
import com.liferay.ide.core.util.SapphireUtil;
import com.liferay.ide.project.core.WorkspaceProductInfo;

import java.util.Calendar;
import java.util.List;

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
		WorkspaceProductInfo instance = WorkspaceProductInfo.getInstance();
		
		NewLiferayWorkspaceOp op = context(NewLiferayWorkspaceOp.class);

		String string = get(op.getProductCategory());
		Boolean boolean1 = get(op.getShowAllVersionProduct());
		
		List<String> productVersionList = instance.getProductVersionList(string, boolean1);
		
		if (ListUtil.isEmpty(productVersionList)) {
			return null;
		}
		
		System.out.println("ProductVersionDefaultValueService Start time is " + Calendar.getInstance().toString());

		return productVersionList.toArray(new String[0])[0];
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
		

	}

	private FilteredListener<PropertyContentEvent> _listener;
	private NewLiferayWorkspaceOp _op;
	private String[] _workspaceProducts;

}