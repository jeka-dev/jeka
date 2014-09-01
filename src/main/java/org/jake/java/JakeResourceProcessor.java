package org.jake.java;

import java.io.File;

import org.jake.JakeLog;
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

	public void runTo(File outputDir) {
		JakeLog.start("Coping resource files to " + outputDir.getPath());
		final int count = jakeDirSet.copyTo(outputDir);
		JakeLog.done(count + " file(s) copied.");
	}



}
