package org.jake.sonar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jake.JakeClassLoader;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.depmanagement.JakeVersion;
import org.jake.java.JakeJavaProcess;
import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsObject;

/**
 * Sonar wrapper class for launching sonar analysis in a convenient way.
 * This Sonar wrapper is not specific to Java project so can be used for to analyse
 * any kind of project supported by SonarQube.
 * 
 * @author Jerome Angibaud
 */
public final class JakeSonar {

    private static final String RUNNER_JAR_NAME_24 = "sonar-runner-2.4.jar";

    private static final String RUNNER_LOCAL_PATH = "build/output/temp/" + RUNNER_JAR_NAME_24;

    private static final String SONAR_PREFIX = ".sonar";
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

    private final boolean enabled;


    private JakeSonar(Map<String, String> params, boolean enabled) {
        super();
        this.params = Collections.unmodifiableMap(params);
        this.enabled = enabled;
    }

    public static JakeSonar of(String projectKey, String projectName, JakeVersion projectVersion) {
        JakeUtilsAssert.notNull(projectName, "Project name can't be null.");
        JakeUtilsAssert.notNull(projectKey, "Project key can't be null.");
        JakeUtilsAssert.notNull(projectVersion, "Project version can't be null.");
        final Map<String, String> map = new HashMap<String, String>();
        map.put(PROJECT_KEY, projectKey);
        map.put(PROJECT_NAME, projectName);
        map.put(PROJECT_VERSION, projectVersion.name());
        map.put(WORKING_DIRECTORY, ".sonarTempDir");
        map.put(VERBOSE, Boolean.toString(JakeOptions.isVerbose()));
        final Properties properties = System.getProperties();
        for (final Object keyObject : properties.keySet()) {
            final String key= (String) keyObject;
            if (key.startsWith(SONAR_PREFIX)) {
                map.put(key.substring(SONAR_PREFIX.length()), properties.getProperty(key));
            }
        }
        return new JakeSonar(map, true);
    }



    public void launch() {
        if (!enabled) {
            JakeLog.info("Sonar analysis skipped.");
        }
        JakeLog.startln("Launching Sonar analysis");
        javaProcess().startAndWaitFor("org.sonar.runner.Main");
        JakeLog.done();
    }

    public JakeSonar enabled(boolean enabled) {
        return new JakeSonar(this.params, enabled);
    }

    private JakeJavaProcess javaProcess() {
        final File sonarRunnerJar = JakeUtilsObject.firstNonNull(
                JakeClassLoader.current().fullClasspath().getEntryContainingClass("org.sonar.runner.Main"),
                jarRunner());
        final JakeJavaProcess result = JakeJavaProcess.of()
                .withClasspath(sonarRunnerJar)
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
    	final File globalJar = new File(JakeUtilsFile.tempDir(), "/jake/" + RUNNER_JAR_NAME_24);
    	if (!globalJar.exists()) {
    		try {
    			return createRunnerJar(JakeUtilsFile.tempDir()); 
    		} catch (Exception e) {
    			return createRunnerJar(new File(projectDir(), RUNNER_LOCAL_PATH));
    		}
    	} 
    	return globalJar;
    }
    
    private static File createRunnerJar(File parent) {
    	parent.mkdirs();
		File file = new File(parent, RUNNER_JAR_NAME_24);
		try {
			file.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException();
		}
		JakeUtilsIO.copyUrlToFile(JakeSonar.class.getResource(RUNNER_JAR_NAME_24), file);
		return file;
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
