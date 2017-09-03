package org.openlca.app.navigation.actions;

import org.openlca.app.M;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.validation.ValidationView;

class DatabaseValidateAction extends Action implements INavigationAction {

	public DatabaseValidateAction() {
		setText(M.Validate);
		setImageDescriptor(Icon.VALIDATE.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		IDatabaseConfiguration config = e.getContent();
		return Database.isActive(config);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		ValidationView.refresh();
	}

}
