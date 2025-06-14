/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkAbstractProcess;
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
public class JkJavaProcess extends JkAbstractProcess<JkJavaProcess> {

    public static final Path CURRENT_JAVA_HOME = Paths.get(System.getProperty("java.home"));

    public static final Path CURRENT_JAVA_EXEC_DIR = CURRENT_JAVA_HOME.resolve("bin");

    private static final List<String> PROXY_PROPS = listOf("http.proxyHost", "http.proxyPort",
            "https.proxyHost", "https.proxyPort", "http.nonProxyHosts", "java.net.useSystemProxies");

    private boolean inheritSystemProperties = false;

    protected JkJavaProcess() {
        super();
        this.addParams(CURRENT_JAVA_EXEC_DIR.resolve("java").toString());
    }

    protected JkJavaProcess(JkJavaProcess other) {
        super(other);
        this.inheritSystemProperties = other.inheritSystemProperties;
    }

    private JkJavaProcess setJdkHome(Path jdkHome) {
        this.removeParam(CURRENT_JAVA_EXEC_DIR.resolve("java").toString());
        this.addParamsAt(0, jdkHome.resolve("bin").resolve("java").toString());
        return this;
    }

    /**
     * Creates a process launching the current JDK java command on the specified class.
     */
    public static JkJavaProcess ofJava(String className) {
        JkUtilsAssert.argument(className != null, "className cannot be null");
        return new JkJavaProcess().addParams(className);
    }

    /**
     * Creates a process launching the current JDK java command on the specified class.
     */
    public static JkJavaProcess ofJava(Path jdkHome, String className) {
        JkUtilsAssert.argument(className != null, "className cannot be null");
        return new JkJavaProcess()
                .setJdkHome(jdkHome)
                .addParams(className);
    }

    /**
     * Creates a process launching the current JDK java command to execute the specified jar.
     * @param className Can be null.
     */
    public static JkJavaProcess ofJavaJar(Path jar, String className) {
        return new JkJavaProcess().addParams("-jar", jar.toString(), className);
    }

    /**
     * Creates a JkJavaProcess instance to execute a specified JAR file using a Java runtime
     * located at the specified javaHome. A fully qualified main class name can also be provided.
     */
    public static JkJavaProcess ofJavaJar(Path javaHome, Path jar, String className) {
        return new JkJavaProcess()
                .setJdkHome(javaHome)
                .addParams("-jar", jar.toString(), className);
    }

    /**
     * Creates a process launching the current JDK java command to execute the specified jar.
     * This method assumes that a main method is specified in the jar manifest.
     */
    public static JkJavaProcess ofJavaJar(Path jar) {
        return ofJavaJar(jar, (String) null);
    }

    /**
     * Creates a JkJavaProcess instance to execute a specified JAR file using a Java runtime
     * located at the specified JDK home. This method assumes that a main method is specified
     * in the JAR manifest.
     *
     * @param JdkHome the path to the Java Development Kit (JDK) home directory, used to locate
     *                the Java runtime environment
     * @param jar     the path to the JAR file to be executed
     * @return a JkJavaProcess instance configured to execute the specified JAR file using
     *         the Java runtime located at the specified JDK home
     */
    public static JkJavaProcess ofJavaJar(Path JdkHome, Path jar) {
        return ofJavaJar(JdkHome, jar, null);
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
        return addParamsAt(1, arg);
    }

    /**
     * Adds the specified java options to the command line.
     * Options are command line parameters prepending the Java class parameter.
     */
    public JkJavaProcess addJavaOptions(Collection<String> options) {
        return addParamsAt(1, options);
    }

    /**
     * Adds the specified Java options to the command line if the given condition is true.
     * @see #addJavaOptions(Collection)
     */
    public JkJavaProcess addJavaOptionsIf(boolean condition, String... options) {
        if (condition) {
            return addJavaOptions(options);
        }
        return this;
    }

    /**
     * Adds the specified java options to the command line.
     * @see #addJavaOptions(Collection)
     */
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

    /**
     * Adds a system property to the command line parameters.
     */
    public JkJavaProcess addSystemProperty(String key, String value) {
        addParamsAt(1, "-D" + key + "=" + value);
        return this;
    }

    /**
     * Sets whether the process should inherit system properties from the parent process.
     */
    public JkJavaProcess setInheritSystemProperties(boolean inheritSystemProperties) {
        this.inheritSystemProperties = inheritSystemProperties;
        return this;
    }

    /**
     * Creates a copy of the current JkJavaProcess instance with the same properties.
     */
    public JkJavaProcess copy() {
        return new JkJavaProcess(this)
                .setInheritSystemProperties(this.inheritSystemProperties);
    }

    @Override
    protected void customizeCommand() {
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            String name = (String) key;
            String prefix = "-D" + name + "=";
            if (getParams().stream().anyMatch(parameter -> parameter.startsWith(prefix))) {
                continue;
            }
            if (inheritSystemProperties || PROXY_PROPS.contains(name)) {
                this.addSystemProperty(name, props.getProperty(name));
            }
        }
    }

}
