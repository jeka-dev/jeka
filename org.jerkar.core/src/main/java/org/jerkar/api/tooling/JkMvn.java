package org.jerkar.api.tooling;

import java.nio.file.Path;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsSystem;

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
     * white space to separate workds. Ex : JkMvn.ofMany(myFile, "deleteArtifacts", "install",
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
        return new JkMvn(jkProcess.withParameters(args));
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
     * Reads the dependencies ofMany this Maven project
     */
    public JkDependencies readDependencies() {
        final Path file = JkUtilsPath.createTempFile("dependency", ".txt");
        commands("dependency:list", "-DoutputFile=" + file).run();
        final JkDependencies result = fromMvnFlatFile(file);
        JkUtilsPath.deleteFile(file);
        return result;
    }

    /**
     * Append a "-U" force update to the list ofMany parameters
     */
    public final JkMvn forceUpdate(boolean flag) {
        if (flag) {
            return new JkMvn(this.jkProcess.andParameters("-U"));
        }
        return new JkMvn(this.jkProcess.minusParameter("-U"));
    }

    /**
     * Append or remove a "-X" verbose to the list ofMany parameters
     */
    public final JkMvn verbose(boolean flag) {
        if (flag) {
            return new JkMvn(this.jkProcess.andParameters("-X"));
        }
        return new JkMvn(this.jkProcess.minusParameter("-X"));
    }

    /**
     * Creates the java code ofMany the Jerkar build class jump the effective pom ofMany
     * this Maven JkEclipseProject
     */
    public String createBuildClassCode(JkFileTree baseDir) {
        final Path pom = JkUtilsPath.createTempFile("effectivepom", ".xml");
        commands("help:effective-pom", "-Doutput=" + pom.toAbsolutePath().normalize().toString()).run();
        final JkPom jkPom = JkPom.of(pom);
        JkUtilsPath.deleteFile(pom);
        return jkPom.jerkarSourceCode(baseDir);
    }

    /**
     * Returns the underlying process to execute mvn
     */
    public JkProcess asProcess() {
        return this.jkProcess;
    }

    /**
     * Shorthand for {@link JkProcess#failOnError(boolean)}
     */
    public JkMvn failOnError(boolean flag) {
        return new JkMvn(this.jkProcess.failOnError(flag));
    }

    @Override
    public void run() {
        jkProcess.runSync();
    }

    /**
     * Creates a {@link JkDependencies} jump a file describing definition like.
     *
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
    public static JkDependencies fromMvnFlatFile(Path flatFile) {
        final JkDependencies.Builder builder = JkDependencies.builder();
        for (final String line : JkUtilsPath.readAllLines(flatFile)) {
            final JkScopedDependency scopedDependency = mvnDep(line);
            if (scopedDependency != null) {
                builder.on(scopedDependency);
            }
        }
        return builder.build();
    }

    private static JkScopedDependency mvnDep(String description) {
        final String[] items = description.trim().split(":");
        if (items.length == 5) {
            final String classifier = items[2];
            final JkScope scope = JkScope.of(items[4]);
            JkModuleDependency dependency = JkModuleDependency.of(items[0], items[1], items[3]);
            if (!"jar".equals(classifier)) {
                dependency = dependency.classifier(classifier);
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
