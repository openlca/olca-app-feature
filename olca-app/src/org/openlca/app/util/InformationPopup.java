package org.openlca.app.util;

import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;

/**
 * A pop-up for information messages.
 */
public class InformationPopup extends Popup {

	public InformationPopup(String message) {
		this(null, message);
	}

	public InformationPopup(String title, String message) {
		super(title, message);
		defaultTitle(M.Notification);
		popupShellImage(Icon.INFO);
	}

	public static void show(final String message) {
		show(null, message);
	}

	public static void show(final String title, final String message) {
		new InformationPopup(title, message).show();
	}

}
