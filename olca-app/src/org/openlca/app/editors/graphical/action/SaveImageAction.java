package org.openlca.app.editors.graphical.action;

import java.io.File;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SaveImageAction extends EditorAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	SaveImageAction() {
		setId(ActionIds.SAVE_IMAGE);
		setText(M.SaveAsImage);
		setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		File file = FileChooser.forExport("*.png", "graph.png");
		if (file == null)
			return;
		App.run(M.SaveAsImage, new Runner(file));
	}

	@Override
	protected boolean accept(ISelection selection) {
		return true;
	}
	
	private class Runner implements Runnable {

		private File file;

		public Runner(File file) {
			this.file = file;
		}

		@Override
		public void run() {
			if (file == null)
				return;
			log.trace("export product graph as image: {}", file);
			ScalableRootEditPart editPart = (ScalableRootEditPart) editor.getGraphicalViewer().getRootEditPart();
			IFigure rootFigure = editPart.getLayer(LayerConstants.PRINTABLE_LAYERS);
			Rectangle bounds = rootFigure.getBounds();
			Image img = new Image(null, bounds.width, bounds.height);
			GC imageGC = new GC(img);
			Graphics graphics = new SWTGraphics(imageGC);
			rootFigure.paint(graphics);
			ImageLoader imgLoader = new ImageLoader();
			imgLoader.data = new ImageData[] { img.getImageData() };
			imgLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
		}
	}
}
