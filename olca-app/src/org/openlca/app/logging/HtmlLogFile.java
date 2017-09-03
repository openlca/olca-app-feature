package org.openlca.app.logging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.HTMLLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.WriterAppender;
import org.eclipse.core.runtime.Platform;

/**
 * The configuration of the HTML log file.
 */
public class HtmlLogFile {

	private static final String FILENAME = "log.html";

	private HtmlLogFile() {
	}

	static void create(Logger logger) {
		try {
			File logFile = createLogFile();
			WriterAppender appender = createAppender(logFile);
			logger.addAppender(appender);
		} catch (Exception e) {
			logger.log(Level.ERROR, e.getMessage(), e);
		}
	}

	private static File createLogFile() {
		File workspaceDir = Platform.getLocation().toFile();
		if (!workspaceDir.exists()) {
			workspaceDir.mkdirs();
		}
		return new File(workspaceDir, FILENAME);
	}

	private static WriterAppender createAppender(File logFile)
			throws IOException {
		HTMLLayout layout = new HTMLLayout();
		RollingFileAppender app = new RollingFileAppender(layout,
				logFile.getAbsolutePath(), true);
		app.setMaxFileSize("3MB");
		app.setMaxBackupIndex(3);
		return app;
	}

	public static List<File> getAllFiles() {
		File workspaceDir = Platform.getLocation().toFile();
		if (!workspaceDir.exists())
			workspaceDir.mkdirs();
		List<File> files = new ArrayList<>();
		for (File file : workspaceDir.listFiles())
			if (file.getName().startsWith(FILENAME))
				files.add(file);
		Collections.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return files;
	}
}
