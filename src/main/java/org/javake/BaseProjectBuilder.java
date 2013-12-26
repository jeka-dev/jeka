package org.javake;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;

public class BaseProjectBuilder {
	
	protected static final File WORKING_DIR = new File(".");
	
	protected String version() {
		return null;
	}
	
	protected String versionSuffix() {
		if (version() == null) {
			return "";
		}
		return "-" + version();
	}
	
	protected String projectName() {
		return FileUtils.fileName(baseDir().getBase());
	}
	
	protected void clean() {
		FileUtils.deleteDirContent(buildOuputDir().getBase());
	}
	
	protected Directory baseDir() {
		return new Directory(WORKING_DIR);
	}

	protected Directory buildOuputDir() {
		return baseDir().relative("build/output", true);
	}
	
	protected Writer writer() {
		return new PrintWriter(System.out);
	}

}
