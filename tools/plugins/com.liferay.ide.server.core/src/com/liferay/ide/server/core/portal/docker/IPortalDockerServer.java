package com.liferay.ide.server.core.portal.docker;

public interface IPortalDockerServer{

	String getName();
	
	String getImageId();
	
	String getHealthCheckUrl();
	
//	Pair<String, String> getExposedPorts();
}
