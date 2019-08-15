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

package com.liferay.ide.gradle.action;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.google.common.collect.Lists;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.util.LiferayWorkspaceUtil;
import com.liferay.ide.server.core.portal.docker.PortalDockerRuntime;
import com.liferay.ide.server.core.portal.docker.PortalDockerServer;
import com.liferay.ide.server.util.LiferayDockerClient;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

/**
 * @author Simon Jiang
 */
public class InitDockerBundleTaskAction extends GradleTaskAction {

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);

		action.setEnabled(LiferayWorkspaceUtil.isValidWorkspace(project));
	}

	protected void afterAction() {
		addPortalRuntimeAndServer(project, null);
	}

	@Override
	protected void beforeAction() {
		deleteWorkspaceDockerServerAndRuntime(project);
	}

	private void addPortalRuntimeAndServer(IProject project, IProgressMonitor monitor){
			
		String serverRuntimeName = project.getName() + "-liferayapp";
		String dockerRepoTag = LiferayWorkspaceUtil.getGradleProperty(project.getLocation().toOSString(), "liferay.workspace.docker.image.liferay", null);
		
		try(DockerClient dockerClient = LiferayDockerClient.getDockerClient()){
			ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
			listImagesCmd.withShowAll(true);
			
			List<Image> images = listImagesCmd.exec();
			
			for(Image image : images) {
				String imageRepoTags = Arrays.toString(image.getRepoTags());
				
				if ( imageRepoTags.contains(dockerRepoTag)) {
					String[] tags = dockerRepoTag.split(":");

					IRuntimeType portalRuntimeType = ServerCore.findRuntimeType(PortalDockerRuntime.ID);
					
					IRuntimeWorkingCopy runtimeWC = portalRuntimeType.createRuntime(serverRuntimeName, monitor);
				
					runtimeWC.setName(serverRuntimeName+ "-" + tags[1]);
					
					PortalDockerRuntime portalDockerRuntime = (PortalDockerRuntime)runtimeWC.loadAdapter(PortalDockerRuntime.class, null);
					
					portalDockerRuntime.setImageRepo(dockerRepoTag);
					portalDockerRuntime.setImageId(image.getId());
					portalDockerRuntime.setBindWorkspaceProject(project.getLocation().toOSString());
					
					portalDockerRuntime.setImageTag(tags[1]);
					
					runtimeWC.save(true, monitor);
				
					IServerType serverType = ServerCore.findServerType(PortalDockerServer.ID);
				
					IServerWorkingCopy serverWC = serverType.createServer(serverRuntimeName, null, runtimeWC, monitor);
				
					serverWC.setName(serverRuntimeName + "-" + tags[1]);
					
					ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
					listContainersCmd.withNameFilter(Lists.newArrayList(project.getName() + "-liferayapp"));
					listContainersCmd.withShowAll(true);
					List<Container> containers = listContainersCmd.exec();
					
					if (ListUtil.isEmpty(containers)) {
						return;
					}
					
					Container[] containerArray = containers.toArray(new Container[containers.size()]);
					
					PortalDockerServer portalDockerServer = (PortalDockerServer)serverWC.loadAdapter(PortalDockerServer.class, null);
					portalDockerServer.setContainerName(project.getName() + "-liferayapp");
					portalDockerServer.settContainerId(containerArray[0].getId());
					portalDockerServer.setImageId(portalDockerRuntime.getImageId());

					serverWC.save(true, monitor);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void deleteWorkspaceDockerServerAndRuntime(IProject project) {
		
		for(IServer server : ServerCore.getServers()) {
			PortalDockerServer portalDockerServer = (PortalDockerServer)server.loadAdapter(PortalDockerServer.class, null);

			if (portalDockerServer == null || portalDockerServer.getContainerName().equals(project.getName() +"-liferayapp")) {
				continue;
			}	

			try(DockerClient dockerClient = LiferayDockerClient.getDockerClient()){
				PortalDockerServer dockerServer = (PortalDockerServer)server.loadAdapter(PortalDockerServer.class, null);
				
				RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(dockerServer.getContainerId());
				removeContainerCmd.withForce(true);
				removeContainerCmd.withRemoveVolumes(true);
				removeContainerCmd.exec();
				
				IRuntime runtime = server.getRuntime();

				server.delete();

				if (runtime != null) {
					runtime.delete();
				}
			}
			catch (Exception e) {
				ProjectCore.logError("Failed to delete server and runtime", e);
			}
		}
	}
	
	@Override
	protected String getGradleTaskName() {
		return "createDockerContainer";
	}

}