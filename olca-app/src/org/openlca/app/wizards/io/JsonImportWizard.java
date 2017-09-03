package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.images.Icon;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.openlca.jsonld.input.UpdateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonImportWizard extends Wizard implements IImportWizard {

	private FileImportPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
		setWindowTitle("openLCA JSON-LD");
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD
				.descriptor());
	}

	@Override
	public void addPages() {
		page = new FileImportPage(new String[] { "zip" }, false);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		File zip = getZip();
		if (zip == null)
			return false;
		try {
			Database.getIndexUpdater().beginTransaction();
			doRun(zip);
			return true;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("JSON import failed", e);
			return false;
		} finally {
			Database.getIndexUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private File getZip() {
		File[] files = page.getFiles();
		if (files == null || files.length == 0)
			return null;
		File file = files[0];
		if (file == null || !file.exists())
			return null;
		else
			return file;
	}

	private void doRun(File zip) throws Exception {
		getContainer().run(true, true, (monitor) -> {
			monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
			try (ZipStore store = ZipStore.open(zip)) {
				JsonImport importer = new JsonImport(store, Database.get());
				UpdateMode updateMode = UpdateMode.NEVER;
				if (FeatureFlag.JSONLD_UPDATES.isEnabled())
					updateMode = UpdateMode.IF_NEWER;
				importer.setUpdateMode(updateMode);
				importer.run();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		});
	}
}
