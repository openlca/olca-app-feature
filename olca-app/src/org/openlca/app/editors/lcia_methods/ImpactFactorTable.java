package org.openlca.app.editors.lcia_methods;

import java.util.ArrayList;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class ImpactFactorTable {

	private final String FLOW = M.Flow;
	private final String CATEGORY = M.Category;
	private final String FLOW_PROPERTY = M.FlowProperty;
	private final String UNIT = M.Unit;
	private final String FACTOR = M.Factor;
	private final String UNCERTAINTY = M.Uncertainty;

	private boolean showFormulas = true;
	private IDatabase database = Database.get();
	private ImpactMethodEditor editor;
	private ImpactCategory category;
	private TableViewer viewer;

	public ImpactFactorTable(ImpactMethodEditor editor) {
		this.editor = editor;
		editor.getParameterSupport().afterEvaluation(this::refresh);
	}

	void refresh() {
		viewer.refresh();
	}

	public void render(Composite parent, Section section) {
		viewer = Tables.createViewer(parent, new String[] { FLOW, CATEGORY, FLOW_PROPERTY, FACTOR, UNIT, UNCERTAINTY });
		FactorLabelProvider label = new FactorLabelProvider();
		Viewers.sortByLabels(viewer, label, 0, 1, 2, 4, 5);
		Viewers.sortByDouble(viewer, (ImpactFactor f) -> f.value, 3);
		viewer.setLabelProvider(label);
		Tables.bindColumnWidths(viewer, 0.2, 0.2, 0.15, 0.15, 0.15, 0.15);
		ModifySupport<ImpactFactor> support = new ModifySupport<>(viewer);
		support.bind(FLOW_PROPERTY, new FlowPropertyModifier());
		support.bind(UNIT, new UnitModifier());
		support.bind(FACTOR, new ValueModifier());
		support.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable(), editor));
		bindActions(viewer, section);
		viewer.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
	}

	void setImpactCategory(ImpactCategory impact, boolean sort) {
		if (impact == null) {
			viewer.setInput(Collections.emptyList());
			this.category = null;
			return;
		}
		this.category = impact;
		List<ImpactFactor> factors = impact.impactFactors;
		if (sort)
			sortFactors(factors);
		viewer.setInput(factors);
	}

	private void sortFactors(List<ImpactFactor> factors) {
		Collections.sort(factors, (o1, o2) -> {
			Flow f1 = o1.flow;
			Flow f2 = o2.flow;
			int c = Strings.compare(f1.getName(), f2.getName());
			if (c != 0)
				return c;
			String cat1 = CategoryPath.getShort(f1.getCategory());
			String cat2 = CategoryPath.getShort(f2.getCategory());
			return Strings.compare(cat1, cat2);
		});
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action formulaSwitch = new FormulaSwitchAction();
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDeletePressed(viewer, (e) -> onRemove());
		Tables.addDropSupport(viewer, (descriptors) -> createFactors(descriptors));
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private void onAdd() {
		if (category == null)
			return;
		BaseDescriptor[] descriptors = ModelSelectionDialog.multiSelect((ModelType.FLOW));
		if (descriptors != null)
			createFactors(Arrays.asList(descriptors));
	}

	private void createFactors(List<BaseDescriptor> descriptors) {
		if (descriptors == null || descriptors.isEmpty())
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (descriptors == null || descriptor.getModelType() != ModelType.FLOW)
				continue;
			Flow flow = database.createDao(Flow.class).getForId(descriptor.getId());
			ImpactFactor f = new ImpactFactor();
			f.flow = flow;
			f.flowPropertyFactor = flow.getReferenceFactor();
			f.unit = flow.getReferenceFactor()
					.getFlowProperty().getUnitGroup().getReferenceUnit();
			f.value = 1.0;
			category.impactFactors.add(f);
		}
		viewer.setInput(category.impactFactors);
		editor.setDirty(true);
	}

	private void onRemove() {
		if (category == null)
			return;
		List<ImpactFactor> factors = Viewers.getAllSelected(viewer);
		for (ImpactFactor factor : factors)
			category.impactFactors.remove(factor);
		viewer.setInput(category.impactFactors);
		editor.setDirty(true);
	}

	private class FactorLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int column) {
			if (column != 0)
				return null;
			if (!(o instanceof ImpactFactor))
				return null;
			ImpactFactor f = (ImpactFactor) o;
			return Images.get(f.flow);
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactFactor))
				return null;
			ImpactFactor f = (ImpactFactor) o;
			switch (col) {
			case 0:
				return Labels.getDisplayName(f.flow);
			case 1:
				return CategoryPath.getShort(f.flow.getCategory());
			case 2:
				if (f.flowPropertyFactor == null)
					return null;
				return Labels.getDisplayName(f.flowPropertyFactor.getFlowProperty());
			case 3:
				if (f.formula == null || !showFormulas)
					return Double.toString(f.value);
				else
					return f.formula;
			case 4:
				return getFactorUnit(f);
			case 5:
				return UncertaintyLabel.get(f.uncertainty);
			default:
				return null;
			}
		}

		private String getFactorUnit(ImpactFactor factor) {
			if (factor.unit == null || category == null)
				return null;
			String impactUnit = category.referenceUnit;
			if (Strings.notEmpty(impactUnit))
				return impactUnit + "/" + factor.unit.getName();
			else
				return "1/" + factor.unit.getName();
		}

	}

	private class FlowPropertyModifier extends ComboBoxCellModifier<ImpactFactor, FlowProperty> {

		@Override
		protected FlowProperty[] getItems(ImpactFactor element) {
			List<FlowProperty> items = new ArrayList<>();
			for (FlowPropertyFactor factor : element.flow.getFlowPropertyFactors())
				items.add(factor.getFlowProperty());
			return items.toArray(new FlowProperty[items.size()]);
		}

		@Override
		protected FlowProperty getItem(ImpactFactor element) {
			if (element.flowPropertyFactor == null)
				return null;
			return element.flowPropertyFactor.getFlowProperty();
		}

		@Override
		protected String getText(FlowProperty value) {
			return value.getName();
		}

		@Override
		protected void setItem(ImpactFactor f, FlowProperty prop) {
			if (f.flowPropertyFactor == null
					|| !Objects.equals(prop, f.flowPropertyFactor.getFlowProperty())) {
				FlowPropertyFactor factor = f.flow.getFactor(prop);
				f.flowPropertyFactor = factor;
				editor.setDirty(true);
			}
		}
	}

	private class UnitModifier extends ComboBoxCellModifier<ImpactFactor, Unit> {

		@Override
		protected Unit[] getItems(ImpactFactor f) {
			if (f.flowPropertyFactor == null)
				return new Unit[0];
			if (f.flowPropertyFactor.getFlowProperty() == null)
				return new Unit[0];
			if (f.flowPropertyFactor.getFlowProperty().getUnitGroup() == null)
				return new Unit[0];
			List<Unit> items = new ArrayList<>();
			for (Unit unit : f.flowPropertyFactor.getFlowProperty().getUnitGroup().getUnits())
				items.add(unit);
			return items.toArray(new Unit[items.size()]);
		}

		@Override
		protected Unit getItem(ImpactFactor f) {
			return f.unit;
		}

		@Override
		protected String getText(Unit value) {
			return value.getName();
		}

		@Override
		protected void setItem(ImpactFactor f, Unit u) {
			if (!Objects.equals(u, f.unit)) {
				f.unit = u;
				editor.setDirty(true);
			}
		}
	}

	private class ValueModifier extends TextCellModifier<ImpactFactor> {

		@Override
		protected String getText(ImpactFactor factor) {
			if (factor.formula == null)
				return Double.toString(factor.value);
			else
				return factor.formula;
		}

		@Override
		protected void setText(ImpactFactor factor, String text) {
			try {
				double value = Double.parseDouble(text);
				if (value == factor.value && factor.formula == null)
					return; // nothing changed
				factor.value = value;
				factor.formula = null;
				editor.setDirty(true);
			} catch (NumberFormatException e) {
				try {
					factor.formula = text;
					editor.setDirty(true);
					editor.getParameterSupport().evaluate();
				} catch (Exception ex) {
					Error.showBox(M.InvalidFormula, text + " " + M.IsInvalidFormula);
				}
			}
		}
	}

	private class FormulaSwitchAction extends Action {
		public FormulaSwitchAction() {
			setImageDescriptor(Icon.NUMBER.descriptor());
			setText(M.ShowValues);
		}

		@Override
		public void run() {
			showFormulas = !showFormulas;
			if (showFormulas) {
				setImageDescriptor(Icon.NUMBER.descriptor());
				setText(M.ShowValues);
			} else {
				setImageDescriptor(Icon.FORMULA.descriptor());
				setText(M.ShowFormulas);
			}
			viewer.refresh();
		}
	}

}
