package org.jerkar.api.tooling;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

/**
 * Convenient class wrapping maven process.
 *
 * @author Jerome Angibaud
 */
public final class JkMvn implements Runnable {

    private final static String MVN_CMD = mvnCmd();

    /** Returns <code>true</code> if Maven is installed on the machine running this code. */
    public static final boolean INSTALLED = MVN_CMD != null;

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
        try {
            final int result = Runtime.getRuntime().exec(cmd + " -version").waitFor();
            return result == 0;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Creates a Maven command. Separate argument in different string, don't use
     * white space to separate workds. Ex : JkMvn.of(myFile, "deleteArtifacts", "install",
     * "-U").
     */
    public static final JkMvn of(Path workingDir, String... args) {
        if (MVN_CMD == null) {
            throw new IllegalStateException("Maven not installed on this machine");
        }
        final JkProcess jkProcess = JkProcess.of(MVN_CMD, args).withWorkingDir(workingDir);
        return new JkMvn(jkProcess);
    }

    private final JkProcess jkProcess;

    private JkMvn(JkProcess jkProcess) {
        super();
        this.jkProcess = jkProcess;
    }

    /**
     * return a new maven command for this working directory. Separate arguments
     * in different strings, don't use white space to separate workds. Ex :
     * withCommand("deleteArtifacts", "install", "-U").
     */
    public final JkMvn commands(String... args) {
        return new JkMvn(jkProcess.withParams(args));
    }

    /**
     * Short hand for #withCommand("deleteArtifacts", "package").
     */
    public final JkMvn cleanPackage() {
        return commands("deleteArtifacts", "package");
    }

    /**
     * Short hand for #withCommand("deleteArtifacts", "install").
     */
    public final JkMvn cleanInstall() {
        return commands("deleteArtifacts", "install");
    }

    /**
     * Reads the dependencies of this Maven project
     */
    public JkDependencySet readDependencies() {
        final Path file = JkUtilsPath.createTempFile("dependency", ".txt");
        commands("dependency:list", "-DoutputFile=" + file).run();
        final JkDependencySet result = fromMvnFlatFile(file);
        JkUtilsPath.deleteFile(file);
        return result;
    }

    /**
     * Append a "-U" force update to the list of parameters
     */
    public final JkMvn withForceUpdate(boolean flag) {
        if (flag) {
            return new JkMvn(this.jkProcess.andParams("-U"));
        }
        return new JkMvn(this.jkProcess.minusParam("-U"));
    }

    /**
     * Append or remove a "-X" verbose to the list of parameters
     */
    public final JkMvn withVerbose(boolean flag) {
        if (flag) {
            return new JkMvn(this.jkProcess.andParams("-X"));
        }
        return new JkMvn(this.jkProcess.minusParam("-X"));
    }

    /**
     * Returns the underlying process to execute mvn
     */
    public JkProcess toProcess() {
        return this.jkProcess;
    }

    @Override
    public void run() {
        jkProcess.runSync();
    }

    /**
     * Creates a {@link JkDependencySet} from file describing dependencies the followiung way :
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
    public static JkDependencySet fromMvnFlatFile(Path flatFile) {
        List<JkScopedDependency> scopedDependencies = new LinkedList<>();
        for (final String line : JkUtilsPath.readAllLines(flatFile)) {
            final JkScopedDependency scopedDependency = mvnDep(line);
            if (scopedDependency != null) {
                scopedDependencies.add(scopedDependency);
            }
        }
        return JkDependencySet.of(scopedDependencies);
    }

    private static JkScopedDependency mvnDep(String description) {
        final String[] items = description.trim().split(":");
        if (items.length == 5) {
            final String classifier = items[2];
            final JkScope scope = JkScope.of(items[4]);
            JkModuleDependency dependency = JkModuleDependency.of(items[0], items[1], items[3]);
            if (!"jar".equals(classifier)) {
                dependency = dependency.withClassifier(classifier);
            }
            return JkScopedDependency.of(dependency, scope);
        }
        if (items.length == 4) {
            final JkScope scope = JkScope.of(items[3]);
            final JkModuleDependency dependency = JkModuleDependency.of(items[0], items[1],
                    items[2]);
            return JkScopedDependency.of(dependency, scope);
        }
        if (items.length == 3) {
            final JkModuleDependency dependency = JkModuleDependency.of(items[0], items[1],
                    items[2]);
            return JkScopedDependency.of(dependency);
        }
        return null;

    }

}
