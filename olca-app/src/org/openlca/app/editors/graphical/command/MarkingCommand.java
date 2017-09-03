package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class MarkingCommand extends Command {

	private final ProcessNode node;

	public MarkingCommand(ProcessNode node) {
		this.node = node;
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
		if (node.isMarked())
			node.unmark();
		else
			node.mark();
		node.parent().editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		if (node.isMarked())
			return M.Unmark;
		return M.Mark;
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
