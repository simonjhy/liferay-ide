package com.liferay.ide.server.util;

import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.SearchImagesCmd;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

public class LiferayDockerClient {

	private static DockerClient _instance;

	
	public static DockerClient getDockerClient() {
		if (_instance == null) {
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
				_instance = dockerClientBuilder.build();					
		}

		return _instance;
	}
	
	public boolean getContainers() {

		SearchImagesCmd searchImagesCmd = dockerClient.searchImagesCmd("portal");
		
		List<SearchItem> items = searchImagesCmd.exec();
		
		for( SearchItem item : items) {
			System.out.println(item.getName());
		}
//
//		ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
//		
//		List<Image> liferayImages = listImagesCmd.exec();
//		
//		for(Image image :liferayImages) {
//			String[] repoTags = image.getRepoTags();
//			List<String> tagList = Arrays.asList(repoTags);
//			String[] tagsRepo = tagList.get(0).split(":");
//			if ( tagsRepo[1].equalsIgnoreCase("7.1.1-ga2")) {
//				
//				Map<String, String> binds = new HashMap<>();
//				binds.put("./", "/etc/liferay/mount");
//				
//				
//				Bind bind = new Bind("./", new Volume("/etc/liferay/mount"));
//				HostConfig hostConfig = HostConfig.newHostConfig();
//				hostConfig.withBinds(bind);
//
//				CreateContainerCmd contanerCreateCmd = dockerClient.createContainerCmd(Arrays.toString(image.getRepoTags()));
//				
//				List<PortBinding> portBindings = new ArrayList<PortBinding>();
//
//				portBindings.add(new PortBinding(Binding.bindIpAndPort("0.0.0.0",8080), new ExposedPort(8080, InternetProtocol.TCP)));
//				portBindings.add(new PortBinding(Binding.bindIpAndPort("0.0.0.0",11311), new ExposedPort(11311, InternetProtocol.TCP)));				
//				
//				List<ExposedPort> exports = portBindings.stream().map( binding -> binding.getExposedPort()).collect(Collectors.toList());
//				
//				contanerCreateCmd.withExposedPorts(exports);
//				
//				contanerCreateCmd.withName("testLiferay");
//				
//				contanerCreateCmd.withEnv("LIFERAY_JPDA_ENABLED=true");
//				
//				contanerCreateCmd.withImage(image.getId());
//
//				CreateContainerResponse createResponse = contanerCreateCmd.exec();
//				
//				dockerClient.commitCmd(createResponse.getId());
//				
//				ListContainersCmd listContainersCmd = dockerClient.listContainersCmd().withShowAll(true);
//				
//				List<Container> containers = listContainersCmd.exec();
//
//
//				for(Container container : containers) {
//					
//					String names = Arrays.toString(container.getNames());
//					System.out.println(container.getId()+ " " +  names);
//				}
//			}
//		}
//
//		
//		dockerClient.createContainerCmd(image)
//		
//		ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
//		listContainersCmd.withShowAll(true);
//		
//		List<Container> containers = listContainersCmd.exec();
//
//		for(Container container : containers) {
//			
//			String names = Arrays.toString(container.getNames());
//			System.out.println(container.getId()+ " " +  names);
//			
//			
//			if (names.contains("liferay")) {
//				StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(container.getId());
//				startContainerCmd.exec();
//				
//				
//				InspectContainerCmd inspectContainerCmd = dockerClient.inspectContainerCmd(container.getId());
//				
//				InspectContainerResponse inspectContainerResponse = inspectContainerCmd.exec();
//				
//				
//				LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(container.getId());
//				logContainerCmd.withStdOut(true);
////				logContainerCmd.withFollowStream(true);
//				
//				logContainerCmd.exec(new LogContainerResultCallback());
//				
//				logContainerCmd.exec(new ResultCallback<Frame>() {
//
//					@Override
//					public void close() throws IOException {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public void onStart(Closeable closeable) {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public void onNext(Frame frame) {
//						// TODO Auto-generated method stub
//						
//						byte[] payload = frame.getPayload();
//						String output = new String(payload);
//						
//						ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
//						System.out.println(byteArrayInputStream);
//					}
//
//					@Override
//					public void onError(Throwable throwable) {
//						// TODO Auto-generated method stub
//						
//					}
//
//					@Override
//					public void onComplete() {
//						// TODO Auto-generated method stub
//						
//					}
//					
//				});
//			}
//		}

		return true;
	}

	private DockerClient dockerClient;
}
