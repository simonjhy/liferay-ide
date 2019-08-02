package com.liferay.ide.server.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

public class LiferayDockerClient {

	
	public static DockerClient getDockerClient() {
//			DefaultDockerClientConfig config
//					= DefaultDockerClientConfig.createDefaultConfigBuilder()
//							.withRegistryUrl("https://registry.hub.docker.com/v2/repositories/liferay/portal")
//							/* .withDockerHost("unix:///var/run/docker.sock") */.build();
//			
//			JerseyDockerCmdExecFactory cmdFactory = new JerseyDockerCmdExecFactory();
//				cmdFactory.withReadTimeout(1000)
//				  .withConnectTimeout(1000)
//				  .withMaxTotalConnections(100)
//				  .withMaxPerRouteConnections(10);
//				
//				DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(cmdFactory);
//
				
				DefaultDockerClientConfig config
				  = DefaultDockerClientConfig.createDefaultConfigBuilder().withRegistryUrl("https://registry.hub.docker.com/v2/repositories/liferay/portal/").withDockerHost("unix:///var/run/docker.sock").build();
				
				DockerClientBuilder dockerClientBuilder = DockerClientBuilder.getInstance(config);
				return dockerClientBuilder.build();					
	}
}
