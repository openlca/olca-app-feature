package org.openlca.app.editors.lcia_methods;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;
import org.openlca.util.Strings;

class ImpactMethodInfoPage extends ModelPage<ImpactMethod> {

	private final String NAME = M.Name;
	private final String DESCRIPTION = M.Description;
	private final String REFERENCE_UNIT = M.ReferenceUnit;

	private TableViewer viewer;
	private FormToolkit toolkit;
	private ImpactMethodEditor editor;
	private ScrolledForm form;

	ImpactMethodInfoPage(ImpactMethodEditor editor) {
		super(editor, "ImpactMethodInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, editor);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createImpactCategoryViewer(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.ImpactAssessmentMethod + ": " + getModel().getName());
	}

	private void createImpactCategoryViewer(Composite body) {
		Section section = UI.section(body, toolkit, M.ImpactCategories);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		String[] properties = { NAME, DESCRIPTION, REFERENCE_UNIT };
		viewer = Tables.createViewer(client, properties);
		viewer.setLabelProvider(new CategoryLabelProvider());
		viewer.setInput(getCategories(true));
		Tables.bindColumnWidths(viewer, 0.5, 0.25, 0.25);
		bindModifySupport();
		bindActions(viewer, section);
		editor.onSaved(() -> viewer.setInput(getCategories(false)));
	}

	private void bindModifySupport() {
		ModifySupport<ImpactCategory> support = new ModifySupport<>(viewer);
		support.bind(NAME, ImpactCategory::getName, (category, text) -> {
			category.setName(text);
			fireCategoryChange();
		});
		support.bind(DESCRIPTION, ImpactCategory::getDescription, (category,
				text) -> {
			category.setDescription(text);
			fireCategoryChange();
		});
		support.bind(REFERENCE_UNIT, c -> c.referenceUnit, (c, text) -> {
			c.referenceUnit = text;
			fireCategoryChange();
		});
	}

	private List<ImpactCategory> getCategories(boolean sorted) {
		ImpactMethod method = editor.getModel();
		List<ImpactCategory> categories = method.impactCategories;
		if (!sorted)
			return categories;
		Collections.sort(categories,
				(c1, c2) -> Strings.compare(c1.getName(), c2.getName()));
		return categories;
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(viewer, add, remove, copy);
		Actions.bind(section, add, remove);
		Tables.onDeletePressed(viewer, (event) -> onRemove());
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null) {
				onAdd();
			}
		});
	}

	private void onAdd() {
		ImpactMethod method = editor.getModel();
		ImpactCategory category = new ImpactCategory();
		category.setRefId(UUID.randomUUID().toString());
		category.setName(M.NewImpactCategory);
		method.impactCategories.add(category);
		viewer.setInput(method.impactCategories);
		fireCategoryChange();
	}

	private void onRemove() {
		ImpactMethod method = editor.getModel();
		List<ImpactCategory> categories = Viewers.getAllSelected(viewer);
		for (ImpactCategory category : categories) {
			method.impactCategories.remove(category);
			for (NwSet set : method.nwSets) {
				NwFactor factor = set.getFactor(category);
				if (factor != null)
					set.factors.remove(factor);
			}
		}
		viewer.setInput(method.impactCategories);
		fireCategoryChange();
	}

	private void fireCategoryChange() {
		editor.postEvent(editor.IMPACT_CATEGORY_CHANGE, this);
		editor.setDirty(true);
	}

	private class CategoryLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column == 0)
				return Images.get(ModelType.IMPACT_CATEGORY);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ImpactCategory))
				return null;
			ImpactCategory category = (ImpactCategory) element;
			switch (columnIndex) {
			case 0:
				return category.getName();
			case 1:
				return category.getDescription();
			case 2:
				return category.referenceUnit;
			default:
				return null;
			}
		}
	}
}
