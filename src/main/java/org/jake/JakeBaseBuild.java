package org.jake;

import java.io.File;
import java.util.logging.Level;

import org.jake.file.JakeDirView;
import org.jake.file.utils.FileUtils;

/**
 * Base project builder defining some commons tasks and utilities 
 * necessary for any kind of project, regardless involved technologies.
 * 
 * @author Jerome Angibaud
 */
public class JakeBaseBuild {
	
	protected static final File WORKING_DIR = FileUtils.canonicalFile(new File("."));
	
	protected JakeBaseBuild() {
	}
	
	/**
	 * The default level to use for the default logger when 
	 * 'java.util.logging.level' system property is not set. 
	 */
	protected Level getDefaultLogLevel() {
		return Level.INFO;
	}

	/**
	 * The current version for this project. Might look like "0.6.3". 
	 */
	protected String version() {
		return null;
	}
	
	/**
	 * The string used to suffix produced artefacts name to indicate version.
	 * Might look like "-0.6.3". 
	 */
	protected String versionSuffix() {
		if (version() == null || version().isEmpty()) {
			return "";
		}
		return "-" + version();
	}
	
	/**
	 * The project name. This is likely to used in produced artefacts. 
	 */
	protected String projectName() {
		return FileUtils.fileName(baseDir().root());
	}
	
	/**
	 * The base directory for this project. All file/directory path are 
	 * resolved from this directory.
	 */
	protected JakeDirView baseDir() {
		return JakeDirView.of(WORKING_DIR);
	}
	
	protected File baseDir(String relativePath) {
		return JakeDirView.of(WORKING_DIR).file(relativePath);
	}

	/**
	 * The output directory where all the final and intermediate 
	 * artefacts are generated.  
	 */
	protected JakeDirView buildOuputDir() {
		return baseDir().sub("build/output").createIfNotExist();
	}
	
	protected File buildOuputDir(String relativePath) {
		return buildOuputDir().file(relativePath);
	}
	
	
	
	// ------------ Operations ------------
	
	/**
	 * Task for cleaning up the output directory.
	 */
	public void clean() {
		JakeLogger.start("Cleaning output directory " + buildOuputDir().root().getPath() );
		FileUtils.deleteDirContent(buildOuputDir().root());
		JakeLogger.done();
	}
	
	/**
	 * Conventional method standing for the default operations to perform.
	 */
	public void doDefault() {
		clean();
	}

}
