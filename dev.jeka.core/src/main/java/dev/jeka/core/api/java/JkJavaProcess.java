package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static dev.jeka.core.api.utils.JkUtilsIterable.listOf;

/**
 * Offers fluent interface for launching Java processes.
 *
 * @author Jerome Angibaud
 */
public class JkJavaProcess extends JkProcess<JkJavaProcess> {

    public static final Path CURRENT_JAVA_HOME = Paths.get(System.getProperty("java.home"));

    public static final Path CURRENT_JAVA_EXEC_DIR = CURRENT_JAVA_HOME.resolve("bin");

    private static final List<String> PROXY_PROPS = listOf("http.proxyHost", "http.proxyPort",
            "https.proxyHost", "https.proxyPort", "http.nonProxyHosts", "java.net.useSystemProxies");

    private boolean inheritSystemProperties = false;

    protected JkJavaProcess() {
        super(CURRENT_JAVA_EXEC_DIR.resolve("java").toString());
    }

    protected JkJavaProcess(JkJavaProcess other) {
        super(other);
        this.inheritSystemProperties = other.inheritSystemProperties;
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
     * Creates a process launching the current JDK java command to execute the specified jar.
     * This method assumes that a main method is specified in the jar manifest.
     */
    public static JkJavaProcess ofJavaJar(Path jar) {
        return ofJavaJar(jar, null);
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

    public JkJavaProcess addSystemProperty(String key, String value) {
        addParams("-D" + key + "=" + value);
        return this;
    }

    public JkJavaProcess setInheritSystemProperties(boolean inheritSystemProperties) {
        this.inheritSystemProperties = inheritSystemProperties;
        return this;
    }

    public JkJavaProcess copy() {
        return new JkJavaProcess(this)
                .setInheritSystemProperties(this.inheritSystemProperties);
    }

    @Override
    protected void customizeCommand(List<String> commands) {
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            String name = (String) key;
            String prefix = "-D" + name + "=";
            if (commands.stream().anyMatch(command -> command.startsWith(prefix))) {
                continue;
            }
            if (inheritSystemProperties || PROXY_PROPS.contains(name)) {
                this.addSystemProperty(name, props.getProperty(name));
            }
        }

    }
}
