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

package com.liferay.ide.upgrade.plan.core;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

/**
 * @author Gregory Amerson
 */
public abstract class BaseUpgradePlanElement implements UpgradePlanElement {

	@Activate
	public void activate(ComponentContext componentContext) {
		Dictionary<String, Object> properties = componentContext.getProperties();

		_description = getStringProperty(properties, "description");
		_id = getStringProperty(properties, "id");
		_imagePath = getStringProperty(properties, "imagePath");
		_title = getStringProperty(properties, "title");
		_order = getDoubleProperty(properties, "order");
	}

	@Override
	public String getDescription() {
		if (_description == null) {
			return getTitle();
		}

		return _description;
	}

	@Override
	public String getId() {
		return _id;
	}

	@Override
	public String getImagePath() {
		return _imagePath;
	}

	@Override
	public double getOrder() {
		return _order;
	}

	@Override
	public String getTitle() {
		return _title;
	}

	@Override
	public String toString() {
		return getId();
	}

	private String _description;
	private String _id;
	private String _imagePath;
	private double _order;
	private String _title;

}