/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Michels, stephan@apache.org - 104944 [JUnit] Unnecessary code in JUnitProgressBar
 *******************************************************************************/
package com.liferay.ide.upgrade.plan.ui.internal;

import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.ui.util.UIUtil;
import com.liferay.ide.upgrade.plan.core.UpgradeEvent;
import com.liferay.ide.upgrade.plan.core.UpgradeListener;
import com.liferay.ide.upgrade.plan.core.UpgradePlan;
import com.liferay.ide.upgrade.plan.core.UpgradePlanAcessor;
import com.liferay.ide.upgrade.plan.core.UpgradePlanner;
import com.liferay.ide.upgrade.plan.core.UpgradeStep;
import com.liferay.ide.upgrade.plan.core.UpgradeStepStatus;
import com.liferay.ide.upgrade.plan.core.UpgradeStepStatusChangedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A progress bar with a red/green indication for success or failure.
 */
public class UpgradeViewProgressBar extends Canvas implements UpgradeListener, UpgradePlanAcessor{
	private static int default_width = 160;
	private static int default_height = 18;

	private int fCurrentTickCount= 0;
	private int fMaxTickCount= 0;
	private int fColorBarWidth= 0;
	private Color fOKColor;

	public UpgradeViewProgressBar(Composite parent) {
		super(parent, SWT.NONE);
		
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				fColorBarWidth= scale(fCurrentTickCount);
				redraw();
			}
		});
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (fOKColor!=null && !fOKColor.isDisposed()) {
					fOKColor.dispose();
				}
			}
		});
		
		Bundle bundle = FrameworkUtil.getBundle(UpgradePlanViewer.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_serviceTracker = new ServiceTracker<>(bundleContext, UpgradePlanner.class, null);

		_serviceTracker.open();

		_upgradePlanner = _serviceTracker.getService();

		_upgradePlanner.addListener(this);
		
		UpgradePlan currentUpgradePlan = _upgradePlanner.getCurrentUpgradePlan();

		List<UpgradeStep> rootSteps = currentUpgradePlan.getRootSteps();
		
		final List<UpgradeStep> leafSteps= new ArrayList<>();
		
		getTotalSteps(rootSteps, leafSteps);
		
		_totalStepsCount = leafSteps.size();
		
		Display display= parent.getDisplay();
		fOKColor= new Color(display, 95, 191, 95);
	}

	private void getTotalSteps(List<UpgradeStep> upgradeSteps, final List<UpgradeStep> leafSteps) {

		if ( upgradeSteps == null || upgradeSteps.size() == 0 ) {
			return;
		}

		List<UpgradeStep> leafs = upgradeSteps.stream(
		).filter(
			step -> step.getChildIds().length == 0
		).collect(
			Collectors.toList()
		);
		
		
		if ( leafs.size() != 0) {
			leafSteps.addAll(leafs);
		}

		List<UpgradeStep> nestSteps = upgradeSteps.stream(
		).filter(
			step -> step.getChildIds().length > 0
		).flatMap(
			step -> Stream.of(step.getChildIds()).map(id -> getStep(id))
		).collect(
			Collectors.toList()
		);

		getTotalSteps(nestSteps	, leafSteps);
		
		return;
	}
	
	public void setMaximum(int max) {
		fMaxTickCount= max;
	}

	public void reset() {
		fCurrentTickCount= 0;
		fMaxTickCount= 0;
		fColorBarWidth= 0;
		redraw();
	}

	public void reset(int ticksDone, int maximum) {
		boolean noChange= fCurrentTickCount == ticksDone && fMaxTickCount == maximum;
		fCurrentTickCount= ticksDone;
		fMaxTickCount= maximum;
		fColorBarWidth= scale(ticksDone);
		if (! noChange)
			redraw();
	}

	private void paintStep(int startX, int endX) {
		GC gc = new GC(this);
		setStatusColor(gc);
		Rectangle rect= getClientArea();
		startX= Math.max(1, startX);
		gc.fillRectangle(startX, 1, endX-startX, rect.height-2);
		gc.dispose();
	}

	private void setStatusColor(GC gc) {
		gc.setBackground(fOKColor);
	}

	private int scale(int value) {
		if (fMaxTickCount > 0) {
			Rectangle r= getClientArea();
			if (r.width != 0)
				return Math.max(0, value*(r.width-2)/fMaxTickCount);
		}
		return value;
	}

	private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
		gc.setForeground(topleft);
		gc.drawLine(x, y, x+w-1, y);
		gc.drawLine(x, y, x, y+h-1);

		gc.setForeground(bottomright);
		gc.drawLine(x+w, y, x+w, y+h);
		gc.drawLine(x, y+h, x+w, y+h);
	}

	private void paint(PaintEvent event) {
		GC gc = event.gc;
		Display disp= getDisplay();

		Rectangle rect= getClientArea();
		gc.fillRectangle(rect);
		drawBevelRect(gc, rect.x, rect.y, rect.width-1, rect.height-1,
			disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
			disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

		setStatusColor(gc);
		fColorBarWidth= Math.min(rect.width-2, fColorBarWidth);
		gc.fillRectangle(1, 1, fColorBarWidth, rect.height-2);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		Point size= new Point(default_width, default_height);
		if (wHint != SWT.DEFAULT) size.x= wHint;
		if (hHint != SWT.DEFAULT) size.y= hHint;
		
		return size;
	}

	public void step(int failures) {
		fCurrentTickCount++;
		int x= fColorBarWidth;

		fColorBarWidth= scale(fCurrentTickCount);

		if (fCurrentTickCount == fMaxTickCount)
			fColorBarWidth= getClientArea().width-1;
		paintStep(x, fColorBarWidth);
	}

	public void refresh(boolean hasErrors) {
		redraw();
	}

	@Override
	public void onUpgradeEvent(UpgradeEvent upgradeEvent) {
		if (upgradeEvent instanceof UpgradeStepStatusChangedEvent) {
			UIUtil.async(
				() -> {
					UpgradeStepStatusChangedEvent statusEvent = Adapters.adapt(upgradeEvent,
							UpgradeStepStatusChangedEvent.class);

					UpgradeStepStatus newStatus = statusEvent.getNewStatus();

					UpgradeStep changeEventStep = statusEvent.getUpgradeStep();

					boolean noChildren = ListUtil.isEmpty(changeEventStep.getChildIds());

					if ((changeEventStep != null) && noChildren) {
						if (newStatus.equals(UpgradeStepStatus.COMPLETED) || newStatus.equals(UpgradeStepStatus.SKIPPED)) {	
							_completedUpgradeSteps.add(changeEventStep);
						}

						if (newStatus.equals(UpgradeStepStatus.INCOMPLETE)) {	
							_completedUpgradeSteps.remove(changeEventStep);
						}
					}

					reset(_completedUpgradeSteps.size(),_totalStepsCount);
				}
			);
		}
	}
	
	
	
	@Override
	public void dispose() {
		_upgradePlanner.removeListener(this);
		_serviceTracker.close();

		super.dispose();
	}

	private final ServiceTracker<UpgradePlanner, UpgradePlanner> _serviceTracker;
	private final UpgradePlanner _upgradePlanner;
	private final Set<UpgradeStep> _completedUpgradeSteps = new CopyOnWriteArraySet<>();
	private int _totalStepsCount = 0;
}

