package org.jake.java;

import java.io.File;

import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.file.JakeDir;
import org.jake.file.JakeDirSet;

public class JakeResourceProcessor {

	private final JakeDirSet jakeDirSet;

	private JakeResourceProcessor(JakeDirSet jakeDirSet) {
		super();
		this.jakeDirSet = jakeDirSet;
	}

	public static JakeResourceProcessor of(JakeDirSet dirSet) {
		return new JakeResourceProcessor(dirSet);
	}

	public void generateTo(File outputDir) {
		JakeLog.start("Coping resource files to " + outputDir.getPath());
		if (jakeDirSet.countFiles(true) > 0 && JakeOptions.isVerbose()) {
			JakeLog.nextLine();
		}
		final int count = jakeDirSet.copyTo(outputDir);
		JakeLog.done(count + " file(s) copied.");
	}

	public JakeResourceProcessor and(JakeDirSet dirSet) {
		return new JakeResourceProcessor(dirSet.and(dirSet));
	}

	public JakeResourceProcessor and(JakeDir dir) {
		return new JakeResourceProcessor(jakeDirSet.and(dir));
	}

	public JakeResourceProcessor andIfExist(File ...dirs) {
		JakeDirSet dirSet = this.jakeDirSet;
		for (final File dir : dirs) {
			if (dir.exists()) {
				dirSet = dirSet.and(JakeDir.of(dir));
			}
		}
		return new JakeResourceProcessor(dirSet);
	}





}
