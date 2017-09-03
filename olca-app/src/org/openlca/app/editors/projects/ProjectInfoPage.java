package org.openlca.app.editors.projects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;

class ProjectInfoPage extends ModelPage<Project> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	public ProjectInfoPage(ProjectEditor editor) {
		super(editor, "ProjectInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createGoalAndScopeSection(body);
		createTimeInfoSection(body);
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Project + ": " + getModel().getName());
	}

	private void createGoalAndScopeSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				M.GoalAndScope);

		createMultiText(M.Goal, "goal", composite);
		createMultiText(M.FunctionalUnit, "functionalUnit", composite);
	}

	private void createTimeInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				M.TimeAndAuthor);

		createReadOnly(M.CreationDate, "creationDate", composite);
		createReadOnly(M.LastModificationDate, "lastModificationDate",
				composite);
		createDropComponent(M.Author, "author", ModelType.ACTOR,
				composite);
	}

}
