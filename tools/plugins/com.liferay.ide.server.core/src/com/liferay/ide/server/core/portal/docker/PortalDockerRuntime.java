package com.liferay.ide.server.core.portal.docker;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;
import com.liferay.ide.server.core.LiferayServerCore;
import com.liferay.ide.server.core.portal.PortalRuntime;
import com.liferay.ide.server.util.LiferayDockerClient;

public class PortalDockerRuntime extends PortalRuntime implements IPortalDockerRuntime{

	public static final String ID = "com.liferay.ide.server.portal.docker.runtime";
	
	@Override
	public String getImageTag() {
		return getAttribute(PROP_DOCKER_IMAGE_TAG, (String)null);
	}

	@Override
	public String getImageId() {
		return getAttribute(PROP_DOCKER_IMAGE_ID, (String)null);
	}

	@Override
	public String getImageRepo() {
		return getAttribute(PROP_DOCKER_IMAGE_REPO, (String)null);
	}

	public void setImageId(String imageId) {
		setAttribute(PROP_DOCKER_IMAGE_ID, imageId);
	}

	public static final String PROP_DOCKER_IMAGE_REPO = "docker-image-repo";
	public static final String PROP_DOCKER_IMAGE_ID = "docker-image-id";
	public static final String PROP_DOCKER_IMAGE_TAG = "docker-image-tag";
	public static final String PROP_DOCKER_IMAGE_BIND_WORKSPACEPROJECT = "docker-image-bind-workspaceprojecr";

	public void setBindWorkspaceProject(String projejctLocation) {
		setAttribute(PROP_DOCKER_IMAGE_BIND_WORKSPACEPROJECT, projejctLocation);
	}
	
	@Override
	public String getBindWorkspaceProject() {
		return getAttribute(PROP_DOCKER_IMAGE_BIND_WORKSPACEPROJECT, (String)null);
	}	
	
	public void setImageRepo(String imageRepo) {
		setAttribute(PROP_DOCKER_IMAGE_REPO, imageRepo);
	}
	
	public void setImageTag(String imageTag) {
		setAttribute(PROP_DOCKER_IMAGE_TAG, imageTag);
	}
	
	private String getImageVersion(String imageRepoTag) {
		String removeLeftBrackets = imageRepoTag.replace("[", "");
		String removeRightBrackets = removeLeftBrackets.replace("]", "");
		
		return removeRightBrackets;
	}	
	
	@Override
	public IStatus validate() {
		DockerClient _dockerClient = LiferayDockerClient.getDockerClient();

		ListImagesCmd listImagesCmd = _dockerClient.listImagesCmd();
		listImagesCmd.withShowAll(true);
		CopyOnWriteArraySet<Image> copyOnWriteArraySet = new CopyOnWriteArraySet<>(listImagesCmd.exec());
		
		List<String> repoTasList = copyOnWriteArraySet.stream().map(image -> {
				String repoTags = Arrays.toString(image.getRepoTags());
				return getImageVersion(repoTags);
			}
		).collect(Collectors.toList());
		
		
		boolean imageExisted = repoTasList.contains(getImageRepo());
		
		return imageExisted==true?Status.OK_STATUS:LiferayServerCore.createErrorStatus("Image is not existed");
	}	
}
