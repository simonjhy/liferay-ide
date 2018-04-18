package com.liferay.ide.project.core.descriptor;

import com.liferay.ide.core.describer.LiferayPropertiesFileDescriber;
import com.liferay.ide.sdk.core.SDKUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

public class LiferayBuildPropertiesFileDescriber  extends LiferayPropertiesFileDescriber {

	public LiferayBuildPropertiesFileDescriber() {
	}

	@Override
	protected boolean isPropertiesFile(Object file) {
		if (file instanceof IFile && isLiferaySdkBuildPropertiesFile(((IFile)file).getLocation())) {
			return true;
		}

		return false;
	}

	private boolean isLiferaySdkBuildPropertiesFile(IPath buildFilePath) {
		return SDKUtil.isValidSDKLocation(buildFilePath.toOSString());
	}
}
