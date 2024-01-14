package dev.jeka.core.api.tooling.maven;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Convenient class wrapping maven process.
 *
 * @author Jerome Angibaud
 */
public final class JkMvn implements Runnable {

    private final static String MVN_CMD = mvnCmd();

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    /**
     * Path to the local Maven repository dir containing user configuration and local repo.
     */
    public static final Path USER_M2_DIR = USER_HOME.resolve(".m2");

    /**
     * Returns the path to the local Maven repository.
     */
    public static Path getM2LocalRepo() {
        return USER_M2_DIR.resolve("repository");  // TODO naive implementation : handle settings.xml
    }

    /**
     * Returns the XML document representing the Maven settings file, or null if the file does not exist.
     */
    public static JkDomDocument settingsXmlDoc() {
        Path file = USER_M2_DIR.resolve("settings");
        if (!Files.exists(file)) {
            return null;
        }
        return JkDomDocument.parse(file);
    }



    /**
     * Creates a Maven command. Separate argument in different string, don't use
     * white space to separate words. Ex : JkMvn.of(myFile, "deleteArtifacts", "install",
     * "-U").
     */
    public static JkMvn of(Path workingDir, String... args) {
        if (MVN_CMD == null) {
            throw new IllegalStateException("Maven not installed on this machine");
        }
        final JkProcess jkProcess = JkProcess.of(MVN_CMD, args).setWorkingDir(workingDir);
        return new JkMvn(jkProcess);
    }

    private final JkProcess jkProcess;

    private JkMvn(JkProcess jkProcess) {
        super();
        this.jkProcess = jkProcess;
    }

    /**
     * return a new maven command for this working directory. Separate arguments
     * in different strings, don't use white space to separate words. Ex :
     * withCommand("deleteArtifacts", "install", "-U").
     */
    public JkMvn commands(String... args) {
        return new JkMvn(jkProcess.addParams(args));
    }

    /**
     * Shorthand for #withCommand("deleteArtifacts", "package").
     */
    public JkMvn cleanPackage() {
        return commands("deleteArtifacts", "package");
    }

    /**
     * Shorthand for #withCommand("deleteArtifacts", "install").
     */
    public JkMvn cleanInstall() {
        return commands("deleteArtifacts", "install");
    }

    /**
     * Reads the dependencies of this Maven project
     */
    public JkQualifiedDependencySet readDependencies() {
        final Path file = JkUtilsPath.createTempFile("dependency", ".txt");
        commands("dependency:list", "-DoutputFile=" + file).run();
        final JkQualifiedDependencySet result = fromMvnFlatFile(file);
        JkUtilsPath.deleteFile(file);
        return result;
    }

    /**
     * Append a "-U" force update to the list of parameters
     */
    public JkMvn withForceUpdate(boolean flag) {
        if (flag) {
            return new JkMvn(this.jkProcess.addParams("-U"));
        }
        return new JkMvn(this.jkProcess.removeParam("-U"));
    }

    /**
     * Append or remove a "-X" verbose to the list of parameters
     */
    public JkMvn withVerbose(boolean flag) {
        if (flag) {
            return new JkMvn(this.jkProcess.addParams("-X"));
        }
        return new JkMvn(this.jkProcess.removeParam("-X"));
    }

    /**
     * Returns the underlying process to execute mvn
     */
    public JkProcess toProcess() {
        return this.jkProcess;
    }

    @Override
    public void run() {
        jkProcess.exec();
    }

    /**
     * Creates a {@link JkDependencySet} from file describing dependencies the following way :
     * <pre>
     * <code>
     * org.springframework:spring-aop:jar:4.2.3.BUILD-SNAPSHOT:compile
     * org.yaml:snakeyaml:jar:1.16:runtime
     * org.slf4j:log4j-over-slf4j:jar:1.7.12:compile
     * org.springframework.boot:spring-boot:jar:1.3.0.BUILD-SNAPSHOT:compile
     * org.hamcrest:hamcrest-core:jar:1.3:test
     * aopalliance:aopalliance:jar:1.0:compile
     * org.springframework:spring-test:jar:4.2.3.BUILD-SNAPSHOT:test
     * org.springframework.boot:spring-boot-autoconfigure:jar:1.3.0.BUILD-SNAPSHOT:compile
     * ch.qos.logback:logback-core:jar:1.1.3:compile
     * org.hamcrest:hamcrest-library:jar:1.3:test
     * junit:junit:jar:4.12:test
     * org.slf4j:slf4j-api:jar:1.7.12:compile
     * </code>
     * </pre>
     *
     * The following format are accepted for each line :
     * <ul>
     * <li>group:name:classifier:version:scope (classifier "jar" equals to no
     * classifier)</li>
     * <li>group:name:version:scope (no classifier)</li>
     * <li>group:name:version (default version is scope)</li>
     * </ul>
     *
     */
    public static JkQualifiedDependencySet fromMvnFlatFile(Path flatFile) {
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (final String line : JkUtilsPath.readAllLines(flatFile)) {
            JkQualifiedDependency scopedDependency = mvnDep(line);
            if (scopedDependency != null) {
                result.add(scopedDependency);
            }
        }
        return JkQualifiedDependencySet.of(result);
    }

    private static JkQualifiedDependency mvnDep(String description) {
        final String[] items = description.trim().split(":");
        if (items.length == 5) {
            final String classifier = items[2];
            final String scope = JkPom.toScope(items[4]);
            JkCoordinate coordinate = JkCoordinate.of(items[0], items[1], items[3]);
            if (!"jar".equals(classifier)) {
                coordinate = coordinate.withClassifiers(classifier);
            }
            JkCoordinateDependency dependency = JkCoordinateDependency.of(coordinate);
            return JkQualifiedDependency.of(scope, dependency);
        }
        final JkCoordinate coordinate = JkCoordinate.of(items[0], items[1], items[2]);
        final JkCoordinateDependency dependency = JkCoordinateDependency.of(coordinate);
        if (items.length == 4) {
            final String scope = JkPom.toScope(items[3]);
            return JkQualifiedDependency.of(scope, dependency);
        }
        if (items.length == 3) {
            return JkQualifiedDependency.of(null, dependency);
        }
        return null;
    }

    private static String mvnCmd() {
        if (JkUtilsSystem.IS_WINDOWS) {
            if (exist("mvn.bat")) {
                return "mvn.bat";
            } else if (exist("mvn.cmd")) {
                return "mvn.cmd";
            } else {
                return null;
            }
        }
        if (exist("mvn")) {
            return "mvn";
        }
        return null;
    }

    private static boolean exist(String cmd) {
        String command = cmd + " -version";
        try {
            final int result = Runtime.getRuntime().exec(command).waitFor();
            return result == 0;
        } catch (final Exception e) {  //NOSONAR
            JkLog.trace("Error while executing command '" + command + "' : " + e.getMessage());
            return false;
        }
    }



}
