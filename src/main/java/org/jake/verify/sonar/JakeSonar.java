package org.jake.verify.sonar;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJava;
import org.jake.java.JakeJavaProcess;


public class JakeSonar {

	public static final String PROJECT_KEY = "projectKey";
	public static final String PROJECT_NAME = "projectName";
	public static final String PROJECT_VERSION = "projectVersion";
	public static final String LANGUAGE = "language";
	public static final String PROFILE = "profile";
	public static final String BRANCH = "branch";
	public static final String SOURCE_ENCODING = "sourceEncoding";

	private static final String PROJECT_BASE_DIR = "projectBaseDir";
	private static final String SOURCES = "sources";
	private static final String BINARIES = "binaries";
	private static final String TEST = "test";
	private static final String LIBRARIES = "libraries";
	private static final String SKIP_DESIGN = "skipDesign";

	public enum AnalyseMode {
		analysis, preview, incremental
	}

	private final Map<String, String> params;


	private JakeSonar(Map<String, String> params) {
		super();
		this.params = Collections.unmodifiableMap(params);
	}

	@SuppressWarnings("unchecked")
	public static JakeSonar of() {
		return new JakeSonar(Collections.EMPTY_MAP);
	}

	public static JakeSonar of(JakeBuildJava buildJava) {
		return JakeSonar.of()
				.withProjectBaseDir(buildJava.baseDir().root())
				.withBinaries(buildJava.classDir())
				.withLibraries(buildJava.deps().compileScope())
				.withSources(buildJava.editedSourceDirs())
				.withTest(buildJava.testSourceDirs());

	}

	public void launch() {
		JakeLog.startAndNextLine("Launching Sonar analysis");
		javaProcess().startAndWaitFor("org.sonar.runner.Main");
		JakeLog.done();
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
		return new JakeSonar(andParams(key, value));
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
