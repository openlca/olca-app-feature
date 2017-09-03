package org.openlca.app.util;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pop-up for error messages.
 */
public class ErrorPopup extends Popup {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ErrorPopup(String message) {
		this(null, message);
	}

	public ErrorPopup(String title, String message) {
		super(title, message);
		defaultTitle(M.ErrorPopupTitle);
		popupShellImage(Icon.ERROR);
	}

	@Override
	protected void makeLink(Composite comp) {
		Hyperlink link = new Hyperlink(comp, SWT.NONE);
		link.setText(M.ErrorPopupMessage);
		link.setForeground(comp.getDisplay().getSystemColor(SWT.COLOR_BLUE));
		Controls.onClick(link, evt -> {
			try {
				File workspaceDir = Platform.getLocation().toFile();
				File logFile = new File(workspaceDir, "log.html");
				Desktop.browse(logFile.toURI().toString());
			} catch (Exception e) {
				log.error("Writing file failed", e);
			}
		});
	}

	public static void show(final String message) {
		show(null, message);
	}

	public static void show(final String title, final String message) {
		new ErrorPopup(title, message).show();
	}

}
