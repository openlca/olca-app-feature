package org.openlca.app.editors.systems;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;

public class CalculateCostsAction extends Action {

	private ProductSystemEditor editor;

	public CalculateCostsAction() {
		setToolTipText(M.CalculateCosts);
		setImageDescriptor(Icon.CALCULATE_COSTS.descriptor());
	}

	public void setActiveEditor(ProductSystemEditor editor) {
		this.editor = editor;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		// final ProductSystem system = (ProductSystem)
		// editor.getModelComponent();
		// final CostCalculator costCalculator = new CostCalculator(
		// Cache.getMatrixCache());
		// BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
		// @Override
		// public void run() {
		// CostResult costResult = costCalculator.calculate(system);
		// CostResultEditorInput input = new CostResultEditorInput(system,
		// costResult);
		// Editors.open(input, CostResultEditor.ID);
		// }
		// });
	}

}
