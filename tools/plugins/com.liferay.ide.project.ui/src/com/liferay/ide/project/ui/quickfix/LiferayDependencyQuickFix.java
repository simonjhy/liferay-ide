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

package com.liferay.ide.project.ui.quickfix;

import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.TargetPlatformDependency;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.project.core.IProjectBuilder;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.ServiceContainer;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.util.TargetPlatformUtil;
import com.liferay.ide.project.ui.ProjectUI;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;

/**
 * @author Simon Jiang
 */
@SuppressWarnings("restriction")
public class LiferayDependencyQuickFix implements IQuickFixProcessor {

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {

		if (ListUtil.isEmpty(locations)) {
			return new IJavaCompletionProposal[0];
		}

		List<IJavaCompletionProposal> resultingCollections = new ArrayList<>();

		for (IProblemLocation curr : locations) {
			List<IJavaCompletionProposal> newProposals = _process(context, curr);

			for (IJavaCompletionProposal newProposal : newProposals) {
				boolean existed = false;

				for (IJavaCompletionProposal existedProposal : resultingCollections) {
					if (existedProposal.getDisplayString().equals(newProposal.getDisplayString())) {
						existed = true;
					}
				}

				if (existed == false) {
					resultingCollections.add(newProposal);
				}
			}
		}

		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		switch (problemId) {
			case IProblem.ImportNotFound:
			case IProblem.UndefinedType:
				return true;
			default:
				return false;
		}
	}

	private IJavaCompletionProposal _createDepProposal(IInvocationContext context, TargetPlatformDependency dependency) {
		final String bundleGroup = dependency.getGroup();
		final String bundleName = dependency.getName();
		final String bundleVersion = dependency.getVersion();
		boolean defaultDependency = dependency.isDefaultDependency();
		
		String proposalDispalyString = (defaultDependency==true)?"Loading, please wait moment.":bundleName;
		
		return new CUCorrectionProposal("Add module dependency: " + proposalDispalyString, context.getCompilationUnit(), null, -0) {

			@Override
			public void apply(IDocument document) {
				try {
					IJavaProject javaProject = context.getCompilationUnit().getJavaProject();

					IProject project = javaProject.getProject();

					ILiferayProject liferayProject = LiferayCore.create(project);

					IProjectBuilder builder = liferayProject.adapt(IProjectBuilder.class);

					if (builder != null) {
						Version retriveVersion = new Version(bundleVersion);

						String[] dependency = {
							bundleGroup, bundleName, retriveVersion.getMajor() + "." + retriveVersion.getMinor() + ".0"
						};

						List<String[]> dependencyList = new ArrayList<>();

						dependencyList.add(dependency);
						builder.updateProjectDependency(project, dependencyList);
					}
				}
				catch (Exception e) {
					ProjectUI.logError("Error adding module dependency", e);
				}
			}

			@Override
			public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
				return "Add module dependency";
			}

			@Override
			public Image getImage() {
				return ProjectUI.getPluginImageRegistry().get(ProjectUI.MODULE_DEPENDENCY_IAMGE_ID);
			}

		};
	}

	private List<IJavaCompletionProposal> _process(IInvocationContext context, IProblemLocation problem) {
		int id = problem.getProblemId();

		List<IJavaCompletionProposal> proposals = new ArrayList<>();

		if (id != 0) {
			switch (id) {
				case IProblem.ImportNotFound:
					proposals = _processImportNotFoundProposals(context, problem);

					break;
				case IProblem.UndefinedType:
					proposals = _processUndefinedTypeProposals(context, problem);

					break;
			}
		}

		return proposals;
	}

	private List<IJavaCompletionProposal> _processImportNotFoundProposals(
		IInvocationContext context, IProblemLocation problem) {

		List<IJavaCompletionProposal> proposals = new ArrayList<>();

		ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());

		if (selectedNode == null) {
			return proposals;
		}

		ImportDeclaration importDeclaration = (ImportDeclaration)ASTNodes.getParent(
			selectedNode, ASTNode.IMPORT_DECLARATION);

		if (importDeclaration == null) {
			return proposals;
		}

		String importName = importDeclaration.getName().toString();
		List<String> servicesList;

		try {
			IProject wsProject = LiferayWorkspaceUtil.getWorkspaceProject();
			
			IWorkspaceProject workspaceProject = LiferayCore.create(IWorkspaceProject.class, wsProject);
			
			TargetPlatformDependency dependency = workspaceProject.getDependency(importName);
			
			if ( dependency==null ) {
				List<String> serviceWrapperList = TargetPlatformUtil.getServiceWrapperList().getServiceList();

				servicesList = TargetPlatformUtil.getServicesList().getServiceList();
				if (serviceWrapperList.contains(importName)) {
					ServiceContainer serviceWrapperBundle = TargetPlatformUtil.getServiceWrapperBundle(importName);
					dependency = new TargetPlatformDependency(serviceWrapperBundle.getBundleGroup(),serviceWrapperBundle.getBundleName(), serviceWrapperBundle.getBundleVersion());
				}
				else if (servicesList.contains(importName)) {
					ServiceContainer serviceWrapperBundle = TargetPlatformUtil.getServiceBundle(importName);
					dependency = new TargetPlatformDependency(serviceWrapperBundle.getBundleGroup(),serviceWrapperBundle.getBundleName(), serviceWrapperBundle.getBundleVersion());
				}
				else if (TargetPlatformUtil.getThirdPartyBundleList(importName) != null) {
					ServiceContainer serviceWrapperBundle = TargetPlatformUtil.getThirdPartyBundleList(importName);
					dependency = new TargetPlatformDependency(serviceWrapperBundle.getBundleGroup(),serviceWrapperBundle.getBundleName(), serviceWrapperBundle.getBundleVersion());
				}			
			}

			proposals.add(_createDepProposal(context,dependency) );
		}
		catch (Exception e) {
			ProjectCore.logError("Error processing import not found proposals", e);
		}

		return proposals;
	}

	private List<IJavaCompletionProposal> _processUndefinedTypeProposals(
		IInvocationContext context, IProblemLocation problem) {

		ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
		String fullyQualifiedName = null;

		if (selectedNode instanceof Name) {
			Name node = (Name)selectedNode;

			fullyQualifiedName = node.getFullyQualifiedName();
		}

		boolean depWrapperCanFixed = false;
		List<IJavaCompletionProposal> proposals = new ArrayList<>();

		try {
			List<String> serviceWrapperList = TargetPlatformUtil.getServiceWrapperList().getServiceList();

			for (String wrapper : serviceWrapperList) {
				if (wrapper.endsWith(fullyQualifiedName)) {
					ServiceContainer bundle = TargetPlatformUtil.getServiceWrapperBundle(wrapper);
					TargetPlatformDependency dependency = new TargetPlatformDependency(bundle.getBundleGroup(),bundle.getBundleName(),bundle.getBundleVersion());
					proposals.add(_createDepProposal(context, dependency));
				}
			}

			if (!depWrapperCanFixed) {
				List<String> servicesList = TargetPlatformUtil.getServicesList().getServiceList();

				for (String service : servicesList) {
					if (service.endsWith(fullyQualifiedName)) {
						ServiceContainer bundle = TargetPlatformUtil.getServiceBundle(service);
						TargetPlatformDependency dependency = new TargetPlatformDependency(bundle.getBundleGroup(),bundle.getBundleName(),bundle.getBundleVersion());
						proposals.add(_createDepProposal(context, dependency));
					}
				}
			}
		}
		catch (Exception e) {
			ProjectCore.logError("Error processing undefined type proposals", e);
		}

		return proposals;
	}

}