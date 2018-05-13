package com.liferay.ide.core;

import java.nio.file.Path;

public class TargetPlatformDependency {
	private Path _libFilePath;
	private String _group;
	private String _name;
	private String _version;
	private boolean _defaultDependency = false;
	private boolean _noDependency = false;
	private String _exportPackage;
	private String _providerCapability;
	private String _fragmentHost;
	
	
	public TargetPlatformDependency(boolean noDependency) {
		_noDependency = noDependency;
	}
	
	public TargetPlatformDependency() {
		_defaultDependency = true;
	}
	
	public TargetPlatformDependency(Path libFilePath,String group, String name, String version) {
		_libFilePath = libFilePath;
		_group = group;
		_name = name;
		_version = version;
	}

	public TargetPlatformDependency(String group, String name, String version) {
		_group = group;
		_name = name;
		_version = version;
	}	

	public void setExportPackage(String exportPackage) {
		_exportPackage = exportPackage;
	}

	public void setProviderCapability(String providerCapability) {
		_providerCapability = providerCapability;
	}

	public void setFragmentHost(String fragmentHost) {
		_fragmentHost = fragmentHost;
	}

	
	public void setLibFilePath(Path libFilePath) {
		_libFilePath = libFilePath;
	}

	public void setGroup(String group) {
		_group = group;
	}

	public void setName(String name) {
		_name = name;
	}
	
	public void setVersion(String version) {
		_version = version;
	}

	public String getExportPackages() {
		return _exportPackage;
	}

	public String getProviderCapability() {
		return _providerCapability;
	}

	public String getFragmentHost() {
		return _fragmentHost;
	}

	public boolean isNoDependency() {
		return _noDependency;
	}
	
	public boolean isDefaultDependency() {
		return _defaultDependency;
	}
	
	public Path getLibFilePath() {
		return _libFilePath;
	}
	
	public String getGroup() {
		return _group;
	}
	
	public String getName() {
		return _name;
	}
	
	public String getVersion() {
		return _version;
	}	
}
