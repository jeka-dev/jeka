package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkHierarchicalConsoleLogHandler;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsFile;
import dev.jeka.core.api.utils.JkUtilsObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

class Environment {

    private Environment() {
        // Can't be instantiated
    }

    static Map<String, String> systemProps = new HashMap<>();

    static CommandLine commandLine = CommandLine.parse(new String[0]);

    static StandardOptions standardOptions = new StandardOptions(Collections.emptyMap());

    static void initialize(String[] commandLineArgs) {

        // Parse command line
        final CommandLine commandLine = CommandLine.parse(commandLineArgs);

        // Take all defined system properties (command line, ofSystem.properties files) and
        // inject them in the system.
        final Map<String, String> sysProps = getSpecifiedSystemProps();
        sysProps.putAll(commandLine.getSystemProperties());
        setSystemProperties(sysProps);

        final Map<String, String> optionMap = new HashMap<>();
        optionMap.putAll(JkOptions.readSystemAndUserOptions());
        optionMap.putAll(commandLine.getCommandOptions());
        JkOptions.init(optionMap);

        final StandardOptions standardOptions = new StandardOptions(optionMap);
        if (standardOptions.logVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }
        if (standardOptions.logQuiteVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.QUITE_VERBOSE);
        }
        JkHierarchicalConsoleLogHandler.setMaxLength(standardOptions.logMaxLength);

        Environment.systemProps = sysProps;
        Environment.commandLine = commandLine;
        Environment.standardOptions = standardOptions;
    }

    private static Map<String, String> userSystemProperties() {
        final Map<String, String> result = new HashMap<>();
        final Path userPropFile = JkLocator.getJekaUserHomeDir().resolve("ofSystem.properties");
        if (Files.exists(userPropFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }


    private static Map<String, String> getSpecifiedSystemProps() {
        final Map<String, String> result = new TreeMap<>();
        final Path propFile = JkLocator.getJekaHomeDir().resolve("ofSystem.properties");
        if (Files.exists(propFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        result.putAll(userSystemProperties());
        return result;
    }

    private static void setSystemProperties(Map<String, String> props) {
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * By convention, standard options start with upper case.
     */
    static class StandardOptions {

        boolean logQuiteVerbose;

        boolean logVerbose;

        boolean logHeaders;

        int logMaxLength = -1;

        String commandClass;

        private final Set<String> names = new HashSet<>();

        StandardOptions (Map<String, String> map) {
            this.logVerbose = valueOf(Boolean.class, map, false, "LogVerbose", "LV");
            this.logQuiteVerbose = valueOf(Boolean.class, map, false, "LogQuiteVerbose", "LQV");
            this.logHeaders = valueOf(Boolean.class, map, false,"LogHeaders", "LH");
            this.logMaxLength = valueOf(Integer.class, map, -1,"LogMaxLength", "LML");
            this.commandClass = valueOf(String.class, map, null, "CommandClass", "CC");
        }

        Set<String> names() {
            return names;
        }

        @Override
        public String toString() {
            return "CommandClass=" + JkUtilsObject.toString(commandClass) + ", LogVerbose=" + logVerbose
                    + ", LogHeaders=" + logHeaders + ", LogMaxLength=" + logMaxLength;
        }

        private <T> T valueOf(Class<T> type, Map<String, String> map, T defaultValue, String ... optionNames) {
            for (String name : optionNames) {
                this.names.add(name);
                if (map.containsKey(name)) {
                    String stringValue = map.get(name);
                    try {
                        return (T) FieldInjector.parse(type, stringValue);
                    } catch (IllegalArgumentException e) {
                        throw new JkException("Option " + name + " has been set with improper value '" + stringValue + "'");
                    }
                }
            }
            return defaultValue;
        }

    }

}
