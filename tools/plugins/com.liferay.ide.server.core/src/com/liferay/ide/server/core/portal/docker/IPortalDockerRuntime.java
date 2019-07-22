package com.liferay.ide.server.core.portal.docker;

import java.util.Map;

public interface IPortalDockerRuntime{

	String getImageTag();
	
	String getImageId();
	
	String getImageRepo();
	
	Map<String,Object> getDockerEnv();
	
	String getImageHealthCheckUrl();
}
