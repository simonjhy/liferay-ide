package com.liferay.ide.server.ui.portal.docker;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.liferay.ide.server.ui.LiferayServerUI;

public class DockerServerSettingComposite extends Composite {

	public DockerServerSettingComposite(Composite parent, IWizardHandle wizard) {
		super(parent, SWT.NONE);

		_wizard = wizard;

		wizard.setTitle(Msgs.dockerContainerTitle);
		wizard.setDescription(Msgs.dockerContainerDescription);
		wizard.setImageDescriptor(LiferayServerUI.getImageDescriptor(LiferayServerUI.IMG_WIZ_RUNTIME));

		createControl(parent);
	}
	private IServerWorkingCopy _serverWC;
	
	protected Layout createLayout() {
		return new GridLayout(2, false);
	}
	
	protected void init() {
		_createFields();
	}
	private GridData _createLayoutData() {
		return new GridData(GridData.FILL_BOTH);
	}
	
	public void setServer(IServerWorkingCopy newServer) {
		if (newServer == null) {
			_serverWC = null;
		}
		else {
			_serverWC = newServer;
		}

		init();

//		try {
//			validate();
//		}
//		catch (NullPointerException npe) {
//		}
	}

	
	
	protected Label createLabel(String text) {
		Label label = new Label(this, SWT.NONE);

		label.setText(text);

		GridDataFactory.generate(label, 1, 1);

		return label;
	}
	
	private void _createFields() {
		
		createLabel(Msgs.dockerContainerName);
//		
//		_dockerImageTagCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
//
//		_dockerImageTagCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//		_dockerImageTagCombo.addModifyListener(this);
//		
//		_nameField = createTextField(Msgs.name);
//
//		_dockerImageSize = createTextField(Msgs.dockerImageSize);
	}	
	
	protected void createControl(final Composite parent) {
		setLayout(createLayout());
		setLayoutData(_createLayoutData());
		setBackground(parent.getBackground());

		_createFields();

		Dialog.applyDialogFont(this);
	}

	private final IWizardHandle _wizard;
	
	private static class Msgs extends NLS {

		public static String dockerContainerTitle;
		public static String dockerContainerDescription;
		public static String dockerContainerName;
		public static String dockerContainerHttpPort;
		public static String dockerContainerGogoShellPort;
		public static String dockerContainerDebugEnable;

		static {
			initializeMessages(DockerServerSettingComposite.class.getName(), Msgs.class);
		}

	}
}
