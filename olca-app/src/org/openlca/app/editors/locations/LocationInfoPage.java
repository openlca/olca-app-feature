package org.openlca.app.editors.locations;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.KmlPrettifyFunction;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.web.WebEngine;

public class LocationInfoPage extends ModelPage<Location> implements WebPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private WebEngine webkit;
	private String kml;
	private boolean isValidKml = true;
	private ScrolledForm form;

	LocationInfoPage(LocationEditor editor) {
		super(editor, "LocationInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		createMapEditorArea(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Location + ": " + getModel().getName());
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				M.AdditionalInformation);
		createText(M.Code, "code", composite);
		createDoubleText(M.Longitude, "longitude", composite);
		createDoubleText(M.Latitude, "latitude", composite);
	}

	private void createMapEditorArea(Composite body) {
		Section section = toolkit.createSection(body,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		UI.gridData(section, true, true);
		section.setText(M.KmlEditor);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		Actions.bind(section, new ClearAction());
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		Control canvas = UI.createWebView(composite, this);
		UI.gridData(canvas, true, true).minimumHeight = 360;
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_EDITOR.getUrl();
	}

	String getKml() {
		return kml;
	}

	boolean isValidKml() {
		return isValidKml;
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		UI.bindVar(webkit, "java", new KmlChangedFunction());
		UI.bindVar(webkit, "prettifier", new KmlPrettifyFunction(b -> {
			isValidKml = b;
		}));
		updateKml();
	}

	void updateKml() {
		kml = KmlUtil.toKml(getModel().getKmz());
		if (kml == null)
			kml = "";
		kml = kml.replace("\r\n", "").replace("\n", "").replace("\r", "");
		try {
			webkit.executeScript("setKML('" + kml + "')");
			webkit.executeScript("setEmbedded()");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	public class KmlChangedFunction {

		public void kmlChanged(String data) {
			kml = data;
			try {
				isValidKml = (Boolean) webkit.executeScript("isValidKml();");
				getEditor().setDirty(true);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to call isValidKml", e);
			}
		}
	}

	private class ClearAction extends Action {

		private ClearAction() {
			super(M.ClearData);
		}

		@Override
		public void run() {
			try {
				webkit.executeScript("onClear();");
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to call onClear", e);
			}
		}
	}
}
