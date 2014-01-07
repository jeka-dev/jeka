package org.javake;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base project builder defining some commons tasks and utilities 
 * necessary for any kind of project, regardless the technology.
 * 
 * @author Djeang
 */
public class BaseProjectBuilder {
	
	protected static final File WORKING_DIR = new File(".");
	
	private final Logger DEFAULT_LOGGER = LogUtils.createDefaultLogger();
	
	protected BaseProjectBuilder() {
		LogUtils.setSystemPropertyLevelOr(logger(), getDefaultLogLevel());
	}
	
	/**
	 * The default level to use for the default logger when 
	 * 'java.util.logging.level' system property is not set. 
	 */
	protected Level getDefaultLogLevel() {
		return Level.INFO;
	}
	
	/**
	 * The logger to use to log all build operation.
	 */
	protected Logger logger() {
		return DEFAULT_LOGGER;
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
		if (version() == null) {
			return "";
		}
		return "-" + version();
	}
	
	/**
	 * The project name. This is likely to used in produced artefacts. 
	 */
	protected String projectName() {
		return FileUtils.fileName(baseDir().getBase());
	}
	
	/**
	 * The base directory for this project. All file/directory path are 
	 * resolved from this directory.
	 */
	protected Directory baseDir() {
		return new Directory(WORKING_DIR);
	}

	/**
	 * The output directory where all the final and intermediate 
	 * artefacts are generated.  
	 */
	protected Directory buildOuputDir() {
		return baseDir().relative("build/output", true);
	}
	
	// ------------ Operations ------------
	
	/**
	 * Task for cleaning up the output directory.
	 */
	public void clean() {
		FileUtils.deleteDirContent(buildOuputDir().getBase());
		logger().info("Output directory " + buildOuputDir().getBase().getPath() + " cleaned.");
	}
	
	/**
	 * Conventional method standing for the default operations to perform.
	 */
	public void doDefault() {
		clean();
	}
	
		

}
