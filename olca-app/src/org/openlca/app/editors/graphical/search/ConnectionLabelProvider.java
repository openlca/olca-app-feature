package org.openlca.app.editors.graphical.search;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.editors.graphical.search.ConnectionDialog.AvailableConnection;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;

class ConnectionLabelProvider extends BaseLabelProvider implements ITableLabelProvider {

	private ConnectionDialog dialog;

	ConnectionLabelProvider(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof AvailableConnection))
			return null;
		AvailableConnection con = (AvailableConnection) element;
		switch (columnIndex) {
		case 1:
			if (con.create)
				return Icon.CHECK_TRUE.get();
			if (!con.alreadyExisting)
				return Icon.CHECK_FALSE.get();
			return null;
		case 2:
			if (con.connect)
				return Icon.CHECK_TRUE.get();
			if (dialog.canBeConnected(con))
				return Icon.CHECK_FALSE.get();
			return null;
		case 3:
			if (con.alreadyExisting)
				return Icon.ACCEPT.get();
			return null; // just show a - (getColumnText)
		case 4:
			if (con.alreadyConnected)
				return Icon.ACCEPT.get();
			return null; // just show a - (getColumnText)
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof AvailableConnection))
			return null;
		AvailableConnection con = (AvailableConnection) element;
		switch (columnIndex) {
		case 0:
			return Labels.getDisplayName(con.process);
		case 3:
			if (!con.alreadyExisting)
				return "-";
			return null; // show checkmark icon (getColumnImage)
		case 4:
			if (!con.alreadyConnected)
				return "-";
			return null; // show checkmark icon (getColumnImage)
		}
		return null;
	}

}