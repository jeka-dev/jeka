package org.jerkar.plugins.sonar;

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

import org.jerkar.JkClassLoader;
import org.jerkar.JkLog;
import org.jerkar.JkOptions;
import org.jerkar.depmanagement.JkVersion;
import org.jerkar.java.JkJavaProcess;
import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;
import org.jerkar.utils.JkUtilsObject;

/**
 * Sonar wrapper class for launching sonar analysis in a convenient way.
 * This Sonar wrapper is not specific to Java project so can be used for to analyse
 * any kind of project supported by SonarQube.
 * 
 * @author Jerome Angibaud
 */
public final class JkSonar {

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
    
    private static final String HOST_URL = "host.url";
    private static final String JDBC_URL = "jdbc.url";
    private static final String JDBC_USERNAME = "jdbc.username";
    private static final String JDBC_PASSWORD = "jdbc.password";
    
    private final Map<String, String> params;

    private final boolean enabled;


    private JkSonar(Map<String, String> params, boolean enabled) {
        super();
        this.params = Collections.unmodifiableMap(params);
        this.enabled = enabled;
    }

    public static JkSonar of(String projectKey, String projectName, JkVersion projectVersion) {
        JkUtilsAssert.notNull(projectName, "Project name can't be null.");
        JkUtilsAssert.notNull(projectKey, "Project key can't be null.");
        JkUtilsAssert.notNull(projectVersion, "Project version can't be null.");
        final Map<String, String> map = new HashMap<String, String>();
        map.put(PROJECT_KEY, projectKey);
        map.put(PROJECT_NAME, projectName);
        map.put(PROJECT_VERSION, projectVersion.name());
        map.put(WORKING_DIRECTORY, ".sonarTempDir");
        map.put(VERBOSE, Boolean.toString(JkOptions.isVerbose()));
        final Properties properties = System.getProperties();
        for (final Object keyObject : properties.keySet()) {
            final String key= (String) keyObject;
            if (key.startsWith(SONAR_PREFIX)) {
                map.put(key.substring(SONAR_PREFIX.length()), properties.getProperty(key));
            }
        }
        return new JkSonar(map, true);
    }



    public void launch() {
        if (!enabled) {
            JkLog.info("Sonar analysis skipped.");
        }
        JkLog.startln("Launching Sonar analysis");
        javaProcess().startAndWaitFor("org.sonar.runner.Main","-e");
        JkLog.done();
    }

    public JkSonar enabled(boolean enabled) {
        return new JkSonar(this.params, enabled);
    }

    private JkJavaProcess javaProcess() {
        final File sonarRunnerJar = JkUtilsObject.firstNonNull(
                JkClassLoader.current().fullClasspath().getEntryContainingClass("org.sonar.runner.Main"),
                jarRunner());
        final JkJavaProcess result = JkJavaProcess.of()
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

    public JkSonar withProperty(String key, String value) {
        return new JkSonar(andParams(key, value), enabled);
    }
    
    public JkSonar withProperties(Map<String, String> props) {
    	final Map<String, String> newProps = new HashMap<String, String>(this.params);
    	newProps.putAll(props);
        return new JkSonar(props, enabled);
    }


    public JkSonar withProjectBaseDir(File baseDir) {
        return withProperty(PROJECT_BASE_DIR, baseDir.getAbsolutePath());
    }

    public JkSonar withSources(Iterable<File> files) {
        return withProperty(SOURCES, toPaths(files));
    }

    public JkSonar withTest(Iterable<File> files) {
        return withProperty(TEST , toPaths(files));
    }

    public JkSonar withBinaries(Iterable<File> files) {
        return withProperty(BINARIES, toPaths(files));
    }

    public JkSonar withBinaries(File... files) {
        return withBinaries(Arrays.asList(files));
    }

    public JkSonar withLibraries(Iterable<File> files) {
        return withProperty(LIBRARIES, toPaths(files));
    }

    public JkSonar withSkipDesign(boolean skip) {
        return withProperty(SKIP_DESIGN, Boolean.toString(skip));
    }
    
    public JkSonar withHostUrl(String url) {
        return withProperty(HOST_URL, url);
    }
    
    public JkSonar withJdbcUrl(String url) {
        return withProperty(JDBC_URL, url);
    }
    
    public JkSonar withJdbcUserName(String userName) {
        return withProperty(JDBC_USERNAME, userName);
    }
    
    public JkSonar withJdbcPassword(String pwd) {
        return withProperty(JDBC_PASSWORD, pwd);
    }
    
    


    private String toPaths(Iterable<File> files) {
        final Iterator<File> it = files.iterator();
        final StringBuilder result = new StringBuilder();
        final File projectDir = projectDir();
        while (it.hasNext()) {
        	final File file = it.next();
        	final String relativePath = JkUtilsFile.getRelativePath(projectDir, file);
        	result.append(relativePath);
            if (it.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    } 

    private File jarRunner() {
    	final File globalJar = new File(JkUtilsFile.tempDir(), "/jerkar/" + RUNNER_JAR_NAME_24);
    	if (!globalJar.exists()) {
    		try {
    			return createRunnerJar(JkUtilsFile.tempDir()); 
    		} catch (final Exception e) {
    			return createRunnerJar(new File(projectDir(), RUNNER_LOCAL_PATH));
    		}
    	} 
    	return globalJar;
    }
    
    private static File createRunnerJar(File parent) {
    	parent.mkdirs();
		final File file = new File(parent, RUNNER_JAR_NAME_24);
		try {
			file.createNewFile();
		} catch (final IOException e) {
			throw new RuntimeException();
		}
		JkUtilsIO.copyUrlToFile(JkSonar.class.getResource(RUNNER_JAR_NAME_24), file);
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
