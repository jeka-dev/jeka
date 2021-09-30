package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.api.utils.JkUtilsIO.JkStreamGobbler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Offers fluent interface for launching Java processes.
 *
 * @author Jerome Angibaud
 */
public class JkJavaProcess extends JkProcess<JkJavaProcess> {

    public static final Path CURRENT_JAVA_DIR = Paths.get(System.getProperty("java.home")).resolve("bin");

    protected JkJavaProcess() {
        super(CURRENT_JAVA_DIR.resolve("java").toString());
    }

    /**
     * Creates a process launching the current JDK java command on the specified class.
     */
    public static JkJavaProcess ofJava(String className) {
        JkUtilsAssert.argument(className != null, "className cannot be null");
        return new JkJavaProcess().addParams(className);
    }

    /**
     * Creates a process launching the current JDK java command to execute the specified jar.
     * @param className Can be null.
     */
    public static JkJavaProcess ofJavaJar(Path jar, String className) {
        return new JkJavaProcess().addParams("-jar", jar.toString(), className);
    }

    /**
     * Adds the specified agent to the command line.
     * @param agentOption Can be null
     */
    public JkJavaProcess addAgent(Path agentLib, String agentOption) {
        JkUtilsAssert.argument(agentLib != null,"agentLib can't be null.");
        JkUtilsAssert.argument(Files.exists(agentLib),"agentLib " + agentLib + " not found.");
        JkUtilsAssert.argument(Files.isRegularFile(agentLib),"agentLib "
                + agentLib + " is a directory, should be a file.");
        String arg = "-javaagent:" + agentLib;
        if (agentOption != null) {
            arg = arg + "=" + agentOption;
        }
        return addParamsFirst(arg);
    }

    /**
     * Adds the specified java options to the command line.
     * Options are command line parameters prepending the Java class to launch (e.g. 'cp', '/mylibs/foo.jar')
     */
    public JkJavaProcess addJavaOptions(Collection<String> options) {
        return addParamsFirst(options);
    }

    public JkJavaProcess addJavaOptionsIf(boolean condition, String... options) {
        if (condition) {
            return addJavaOptions(options);
        }
        return this;
    }

    public JkJavaProcess addJavaOptions(String... options) {
        return this.addJavaOptions(Arrays.asList(options));
    }

    /**
     * Convenient method to set classpath option.
     */
    public JkJavaProcess setClasspath(Iterable<Path> paths) {
        if (paths == null) {
            throw new IllegalArgumentException("Classpath can't be null.");
        }
        final JkPathSequence classpath = JkPathSequence.of(JkUtilsPath.disambiguate(paths));
        return this.addJavaOptions("-cp", classpath.toPath());
    }

}
