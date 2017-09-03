package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class ChangeStateCommand extends Command {

	private final ProcessNode node;
	private final boolean initiallyMinimized;

	public ChangeStateCommand(ProcessNode node) {
		this.node = node;
		initiallyMinimized = node.isMinimized();
	}

	@Override
	public boolean canExecute() {
		if (node == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (node.isMinimized())
			node.maximize();
		else
			node.minimize();
		node.parent().editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		if (node.isMinimized()) {
			if (initiallyMinimized)
				return M.Maximize;
			return M.Minimize;
		}
		if (initiallyMinimized)
			return M.Minimize;
		return M.Maximize;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		execute();
	}

}
