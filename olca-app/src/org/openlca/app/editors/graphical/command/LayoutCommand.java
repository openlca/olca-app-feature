package org.openlca.app.editors.graphical.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.layout.LayoutType;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class LayoutCommand extends Command {

	private final ProductSystemNode model;
	private final LayoutManager layoutManager;
	private final LayoutType type;
	private final Map<IFigure, Rectangle> oldConstraints = new HashMap<>();

	public LayoutCommand(ProductSystemNode model, LayoutManager layoutManager, LayoutType type) {
		this.model = model;
		this.layoutManager = layoutManager;
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		if (type == null)
			return false;
		if (layoutManager == null)
			return false;
		if (model == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		for (ProcessNode node : model.getChildren())
			if (node.figure.isVisible())
				oldConstraints.put(node.figure, node.figure
						.getBounds().getCopy());
		layoutManager.layout(model.figure, type);
		model.editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.Layout + ": " + type.getDisplayName();
	}

	@Override
	public void redo() {
		layoutManager.layout(model.figure, type);
		model.editor.setDirty(true);
	}

	@Override
	public void undo() {
		for (ProcessNode node : model.getChildren())
			if (oldConstraints.get(node.figure) != null)
				node.setXyLayoutConstraints(oldConstraints.get(node.figure));
		model.editor.setDirty(true);
	}

}
