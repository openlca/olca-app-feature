package org.openlca.app.editors.projects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.reports.ReportViewer;
import org.openlca.app.editors.reports.Reports;
import org.openlca.app.editors.reports.model.ReportCalculator;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProjectSetupPage extends ModelPage<Project> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ProjectEditor editor;
	private IDatabase database = Database.get();

	private Project project;
	private TableViewer variantViewer;
	private ProjectParameterTable parameterTable;
	private ReportVariantSync variantSync;
	private ScrolledForm form;

	ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", M.ProjectSetup);
		this.editor = editor;
		project = editor.getModel();
		variantSync = new ReportVariantSync(editor);
		editor.onSaved(() -> {
			project = editor.getModel();
			if (variantViewer != null)
				variantViewer.setInput(project.getVariants());
		});
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
		createButtons(infoSection.getContainer());
		new ImpactSection(editor).render(body, toolkit);
		createVariantsSection(body);
		createParameterSection(body);
		initialInput();
		new ProcessContributionSection(editor).create(body, toolkit);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Project + ": " + getModel().getName());
	}

	private void initialInput() {
		List<ProjectVariant> variants = project.getVariants();
		Collections.sort(variants,
				(v1, v2) -> Strings.compare(v1.getName(), v2.getName()));
		variantViewer.setInput(variants);
	}

	private void createButtons(Composite composite) {
		toolkit.createLabel(composite, "");
		Composite buttonContainer = toolkit.createComposite(composite);
		UI.gridLayout(buttonContainer, 2).marginHeight = 5;
		createReportButton(buttonContainer);
	}

	private void createReportButton(Composite composite) {
		Button button = toolkit.createButton(composite, M.Report,
				SWT.NONE);
		UI.gridData(button, false, false).widthHint = 100;
		button.setImage(Images.get(ModelType.PROJECT));
		Controls.onSelect(button, (e) -> {
			App.run(M.Calculate,
					new ReportCalculator(getModel(), editor.getReport()),
					() -> {
						Reports.save(getModel(), editor.getReport(), database);
						ReportViewer.open(editor.getReport());
					});
		});
	}

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, M.Variants);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { M.Name, M.ProductSystem,
				M.AllocationMethod, M.Flow, M.Amount,
				M.Unit, M.Description };
		variantViewer = Tables.createViewer(composite, properties);
		variantViewer.setLabelProvider(new VariantLabelProvider());
		Tables.bindColumnWidths(variantViewer, 0.15, 0.15, 0.15, 0.15, 0.125,
				0.125, 0.15);
		ModifySupport<ProjectVariant> support = new ModifySupport<>(
				variantViewer);
		support.bind(M.Name, new VariantNameEditor());
		support.bind(M.AllocationMethod, new VariantAllocationEditor());
		support.bind(M.Amount, new DoubleModifier<>(editor, "amount"));
		support.bind(M.Unit, new VariantUnitEditor());
		support.bind(M.Description, new VariantDescriptionEditor());
		addVariantActions(variantViewer, section);
	}

	private void createParameterSection(Composite body) {
		Section section = UI.section(body, toolkit, M.Parameters);
		parameterTable = new ProjectParameterTable(editor);
		parameterTable.render(section, toolkit);
	}

	private void addVariantActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(this::addVariant);
		Action remove = Actions.onRemove(this::removeVariant);
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null) {
				addVariant();
				return;
			}
			ProjectVariant variant = Viewers.getFirstSelected(viewer);
			if (variant != null)
				App.openEditor(variant.getProductSystem());
		});
	}

	private void addVariant() {
		log.trace("add variant");
		BaseDescriptor d = ModelSelectionDialog
				.select(ModelType.PRODUCT_SYSTEM);
		if (d == null)
			return;
		ProductSystemDao dao = new ProductSystemDao(database);
		ProductSystem system = dao.getForId(d.getId());
		if (system == null) {
			log.error("failed to load product system");
			return;
		}
		List<ProjectVariant> variants = project.getVariants();
		ProjectVariant variant = createVariant(system, variants.size() + 1);
		variants.add(variant);
		variantViewer.setInput(variants);
		parameterTable.addVariant(variant);
		variantSync.variantAdded(variant);
		editor.setDirty(true);
	}

	private ProjectVariant createVariant(ProductSystem system, int i) {
		ProjectVariant variant = new ProjectVariant();
		variant.setProductSystem(system);
		variant.setName(M.Variant + i);
		variant.setAllocationMethod(AllocationMethod.NONE);
		variant.setAmount(system.getTargetAmount());
		variant.setFlowPropertyFactor(system.getTargetFlowPropertyFactor());
		variant.setUnit(system.getTargetUnit());
		for (ParameterRedef redef : system.getParameterRedefs())
			variant.getParameterRedefs().add(redef.clone());
		return variant;
	}

	private void removeVariant() {
		log.trace("remove variant");
		List<ProjectVariant> selection = Viewers.getAllSelected(variantViewer);
		if (selection == null || selection.isEmpty())
			return;
		List<ProjectVariant> variants = project.getVariants();
		for (ProjectVariant var : selection) {
			variants.remove(var);
			parameterTable.removeVariant(var);
		}
		variantViewer.setInput(variants);
		variantSync.variantsRemoved(selection);
		editor.setDirty(true);
	}

	private class VariantNameEditor extends TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return variant.getName();
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			if (Objects.equals(text, variant.getName()))
				return;
			variantSync.updateName(variant, text);
			variant.setName(text);
			parameterTable.updateVariant(variant);
			editor.setDirty(true);
		}
	}

	private class VariantDescriptionEditor extends
			TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return variantSync.getDescription(variant);
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			String oldText = variantSync.getDescription(variant);
			if (Objects.equals(text, oldText))
				return;
			variantSync.updateDescription(variant, text);
			editor.setDirty(true);
		}
	}

	private class VariantAllocationEditor extends
			ComboBoxCellModifier<ProjectVariant, AllocationMethod> {
		@Override
		protected AllocationMethod getItem(ProjectVariant var) {
			return var.getAllocationMethod() != null ? var
					.getAllocationMethod() : AllocationMethod.NONE;
		}

		@Override
		protected AllocationMethod[] getItems(ProjectVariant element) {
			return AllocationMethod.values();
		}

		@Override
		protected String getText(AllocationMethod value) {
			return Labels.getEnumText(value);
		}

		@Override
		protected void setItem(ProjectVariant var, AllocationMethod item) {
			var.setAllocationMethod(item);
			editor.setDirty(true);
		}
	}

	private class VariantUnitEditor extends
			ComboBoxCellModifier<ProjectVariant, Unit> {
		@Override
		protected Unit getItem(ProjectVariant var) {
			return var.getUnit();
		}

		@Override
		protected Unit[] getItems(ProjectVariant var) {
			FlowPropertyFactor fac = var.getFlowPropertyFactor();
			if (fac == null || fac.getFlowProperty() == null
					|| fac.getFlowProperty().getUnitGroup() == null)
				return new Unit[0];
			UnitGroup unitGroup = fac.getFlowProperty().getUnitGroup();
			Unit[] units = unitGroup.getUnits().toArray(
					new Unit[unitGroup.getUnits().size()]);
			Arrays.sort(units, (u1, u2) -> {
				if (u1 == null || u2 == null)
					return 0;
				return Strings.compare(u1.getName(), u2.getName());
			});
			return units;
		}

		@Override
		protected String getText(Unit unit) {
			if (unit == null)
				return "";
			return unit.getName();
		}

		@Override
		protected void setItem(ProjectVariant var, Unit unit) {
			var.setUnit(unit);
			editor.setDirty(true);
		}
	}

	private class VariantLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 1)
				return Images.get(ModelType.PRODUCT_SYSTEM);
			if (columnIndex == 3)
				return Images.get(FlowType.PRODUCT_FLOW);
			if (columnIndex == 5)
				return Images.get(ModelType.UNIT);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ProjectVariant))
				return null;
			ProjectVariant variant = (ProjectVariant) element;
			ProductSystem system = variant.getProductSystem();
			if (system == null)
				return null;
			switch (columnIndex) {
			case 0:
				return variant.getName();
			case 1:
				return system.getName();
			case 2:
				return Labels.getEnumText(variant.getAllocationMethod());
			case 3:
				return getFlowText(system);
			case 4:
				return Double.toString(variant.getAmount());
			case 5:
				Unit unit = variant.getUnit();
				return unit == null ? null : unit.getName();
			case 6:
				return variantSync.getDescription(variant);
			default:
				return null;
			}
		}

		private String getFlowText(ProductSystem system) {
			if (system == null || system.getReferenceExchange() == null)
				return null;
			Exchange refExchange = system.getReferenceExchange();
			return Labels.getDisplayName(refExchange.flow);
		}
	}
}
