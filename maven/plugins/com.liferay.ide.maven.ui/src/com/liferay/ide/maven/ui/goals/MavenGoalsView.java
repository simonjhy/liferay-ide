package com.liferay.ide.maven.ui.goals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.wizards.IWizardDescriptor;


public class MavenGoalsView extends ViewPart {

    private PageBook pages;
    private Link emptyInputPage;
    private Label errorInputPage;
    private Composite nonEmptyInputPage;
    private TreeViewer treeViewer;
    private FilteredTree filteredTree;	
	
    private static String Label_No_Maven_Projects="There are no Maven projects in the current workspace. <a>Import a Maven project</a> to see its tasks in the Liferay Maven Goals View.";
    private static String Label_Reload_Error="There was an error loading the content of the task view. Check out the log for more information: Menu > Window > Show View > Error Log.";
	@Override
	public void createPartControl(Composite parent) {
	       // the top-level control changing its content depending on whether the content provider
        // contains task data to display or not
        this.pages = new PageBook(parent, SWT.NONE);

        // if there is no task data to display, show only a label
        this.emptyInputPage = new Link(this.pages, SWT.NONE);
        this.emptyInputPage.setText(Label_No_Maven_Projects);

        // if there is a problem loading the task data, show an error label
        this.errorInputPage = new Label(this.pages, SWT.NONE);
        this.errorInputPage.setText(Label_Reload_Error);
        this.errorInputPage.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));

        // if there is task data to display, show the task tree and the search label on the bottom
        this.nonEmptyInputPage = new Composite(this.pages, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = gridLayout.marginHeight = gridLayout.verticalSpacing = 0;
        this.nonEmptyInputPage.setLayout(gridLayout);

        // add tree with two columns
        this.filteredTree = new FilteredTree(this.nonEmptyInputPage, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, new PatternFilter(true));
        this.filteredTree.setShowFilterControls(false);
        this.treeViewer = this.filteredTree.getViewer();
        this.treeViewer.getTree().setHeaderVisible(true);
        this.treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        // set filter, comparator, and content provider
        this.treeViewer.addFilter(TaskNodeViewerFilter.createFor(getState()));
        this.treeViewer.setComparator(TaskNodeViewerSorter.createFor(this.state));
        this.treeViewer.setContentProvider(new TaskViewContentProvider(this));

        TreeViewerColumn treeViewerNameColumn = new TreeViewerColumn(this.treeViewer, SWT.LEFT);
        treeViewerNameColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new TaskNameLabelProvider()));
        final TreeColumn taskNameColumn = treeViewerNameColumn.getColumn();
        taskNameColumn.setText(TaskViewMessages.Tree_Column_Name_Text);
        taskNameColumn.setWidth(this.state.getHeaderNameColumnWidth());

        TreeViewerColumn treeViewerDescriptionColumn = new TreeViewerColumn(this.treeViewer, SWT.LEFT);
        treeViewerDescriptionColumn.setLabelProvider(new TaskDescriptionLabelProvider());
        final TreeColumn taskDescriptionColumn = treeViewerDescriptionColumn.getColumn();
        taskDescriptionColumn.setText(TaskViewMessages.Tree_Column_Description_Text);
        taskDescriptionColumn.setWidth(this.state.getHeaderDescriptionColumnWidth());

        // open the import wizard if the empty input page link is selected
        this.emptyInputPage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    IWizardDescriptor descriptor = PlatformUI.getWorkbench().getImportWizardRegistry().findWizard(UiPluginConstants.IMPORT_WIZARD_ID);
                    IWizard wizard = descriptor.createWizard();
                    WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
                    dialog.open();
                } catch (CoreException e) {
                    throw new GradlePluginsRuntimeException(e);
                }
            }
        });

        // when changed save the header width into the state
        taskNameColumn.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent e) {
                TaskView.this.state.setHeaderNameColumnWidth(taskNameColumn.getWidth());
            }
        });

        taskDescriptionColumn.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                TaskView.this.state.setHeaderDescriptionColumnWidth(taskDescriptionColumn.getWidth());
            }
        });

        // manage the selection history as required for the task execution and let the
        // SelectionHistoryManager propagate the NodeSelection to the Workbench
        this.selectionHistoryManager = new SelectionHistoryManager(this.treeViewer);
        getSite().setSelectionProvider(this.selectionHistoryManager);


        // create toolbar actions, menu items, event listeners, etc.
        this.uiContributionManager = new UiContributionManager(this);
        this.uiContributionManager.wire();

        // set initial content (use fetch strategy LOAD_IF_NOT_CACHED since
        // the model might already be available in case a project import has
        // just happened)
        reload(FetchStrategy.LOAD_IF_NOT_CACHED);

	}

	@Override
	public void setFocus() {

		// TODO Auto-generated method stub

	}

}
