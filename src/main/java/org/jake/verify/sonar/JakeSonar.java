package org.jake.verify.sonar;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.java.JakeJavaProcess;
import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsFile;


public final class JakeSonar {

	public static final String PROJECT_KEY = "projectKey";
	public static final String PROJECT_NAME = "projectName";
	public static final String PROJECT_VERSION = "projectVersion";
	public static final String LANGUAGE = "language";
	public static final String PROFILE = "profile";
	public static final String BRANCH = "branch";
	public static final String SOURCE_ENCODING = "sourceEncoding";
	public static final String VERBOSE = "verbose";
	public static final String WORKING_DIRECTORY = "working.directory";
	public static final String JUNIT_REPORTS_PATH = "junit.reportsPath";
	public static final String SUREFIRE_REPORTS_PATH = "surefire.reportsPath";
	public static final String JACOCO_REPORTS_PATH = "jacoco.reportPath";
	public static final String COVERTURA_REPORTS_PATH = "cobertura.reportPath";
	public static final String CLOVER_REPORTS_PATH = "clover.reportPath";
	public static final String DYNAMIC_ANALYSIS = "dynamicAnalysis";


	private static final String PROJECT_BASE_DIR = "projectBaseDir";
	private static final String SOURCES = "sources";
	private static final String BINARIES = "binaries";
	private static final String TEST = "test";
	private static final String LIBRARIES = "libraries";
	private static final String SKIP_DESIGN = "skipDesign";

	private final Map<String, String> params;

	public final boolean enabled;


	private JakeSonar(Map<String, String> params, boolean enabled) {
		super();
		this.params = Collections.unmodifiableMap(params);
		this.enabled = enabled;
	}

	public static JakeSonar of(String projectKey, String projectName, String projectVersion) {
		JakeUtilsAssert.notNull(projectName, "Project name can't be null.");
		JakeUtilsAssert.notNull(projectKey, "Project key can't be null.");
		JakeUtilsAssert.notNull(projectVersion, "Project version can't be null.");
		final Map<String, String> map = new HashMap<String, String>();
		map.put(PROJECT_KEY, projectKey);
		map.put(PROJECT_NAME, projectName);
		map.put(PROJECT_VERSION, projectVersion);
		map.put(WORKING_DIRECTORY, "build/.sonar");
		map.put(VERBOSE, Boolean.toString(JakeOptions.isVerbose()));
		final Properties properties = System.getProperties();
		for (final Object keyObject : properties.keySet()) {
			final String key= (String) keyObject;
			if (key.startsWith("sonar.")) {
				map.put(key.substring(6), properties.getProperty(key));
			}
		}
		return new JakeSonar(map, true);
	}



	public void launch() {
		if (!enabled) {
			JakeLog.info("Sonar analysis skipped.");
		}
		JakeLog.startAndNextLine("Launching Sonar analysis");
		javaProcess().startAndWaitFor("org.sonar.runner.Main");
		JakeLog.done();
	}

	public JakeSonar enabled(boolean enabled) {
		return new JakeSonar(this.params, enabled);
	}

	private JakeJavaProcess javaProcess() {
		final JakeJavaProcess result = JakeJavaProcess.of()
				.withClasspath(jarRunner())
				.andOptions(toProperties());
		return result;
	}

	private List<String> toProperties() {
		final List<String> result = new LinkedList<String>();
		for (final Map.Entry<String, String> entry : this.params.entrySet()) {
			result.add("-Dsonar." + entry.getKey() + "=" + entry.getValue());
		}
		return result;
	}

	public JakeSonar withProperty(String key, String value) {
		return new JakeSonar(andParams(key, value), enabled);
	}


	public JakeSonar withProjectBaseDir(File baseDir) {
		return withProperty(PROJECT_BASE_DIR, baseDir.getAbsolutePath());
	}

	public JakeSonar withSources(Iterable<File> files) {
		return withProperty(SOURCES, toPaths(files));
	}

	public JakeSonar withTest(Iterable<File> files) {
		return withProperty(TEST , toPaths(files));
	}

	public JakeSonar withBinaries(Iterable<File> files) {
		return withProperty(BINARIES, toPaths(files));
	}

	public JakeSonar withBinaries(File... files) {
		return withBinaries(Arrays.asList(files));
	}

	public JakeSonar withLibraries(Iterable<File> files) {
		return withProperty(LIBRARIES, toPaths(files));
	}

	public JakeSonar withSkipDesign(boolean skip) {
		return withProperty(SKIP_DESIGN, Boolean.toString(skip));
	}

	private String toPaths(Iterable<File> files) {
		final Iterator<File> it = files.iterator();
		final StringBuilder result = new StringBuilder();
		while (it.hasNext()) {
			result.append(JakeUtilsFile.getRelativePath(projectDir(), it.next()));
			if (it.hasNext()) {
				result.append(",");
			}
		}
		return result.toString();
	}

	private File jarRunner() {
		final File localJar = new File(projectDir(), "build/libs/sonar-runner/sonar-runner.jar");
		final File sharedJar = new File(JakeLocator.optionalLibsDir(), "sonar-runner.jar");
		final File jar = localJar.exists() ? localJar : sharedJar;
		if (!jar.exists()) {
			throw new IllegalStateException("No sonar-runner.jar found neither in " + localJar.getAbsolutePath()
					+ " nor in " + sharedJar.getAbsolutePath() );
		}
		return jar;
	}

	private Map<String, String> andParams(String key, String value) {
		final Map<String, String> newMap = new HashMap<String, String>(this.params);
		newMap.put(key, value);
		return newMap;
	}

	private File projectDir() {
		return new File(this.params.get(PROJECT_BASE_DIR));
	}


}
