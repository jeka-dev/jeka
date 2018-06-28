package org.jerkar.tool;

import org.jerkar.api.system.JkException;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class Environment {

    static Map<String, String> systemProps = new HashMap<>();

    static CommandLine commandLine = CommandLine.parse(new String[0]);

    static StandardOptions standardOptions = new StandardOptions(Collections.emptyMap());

    static void initialize(String[] commandLineArgs) {

        // Parse command line
        final CommandLine commandLine = CommandLine.parse(commandLineArgs);

        // Take all defined system properties (command line, system.properties files) and
        // inject them in the system.
        final Map<String, String> sysProps = getSpecifiedSystemProps();
        sysProps.putAll(commandLine.getSystemProperties());
        setSystemProperties(sysProps);

        final Map<String, String> optionMap = new HashMap<>();
        optionMap.putAll(JkOptions.readSystemAndUserOptions());
        optionMap.putAll(commandLine.getOptions());
        JkOptions.init(optionMap);

        final StandardOptions standardOptions = new StandardOptions(optionMap);
        if (standardOptions.logVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }
        LogHandler.MAX_LENGTH = standardOptions.logMaxLength;

        Environment.systemProps = sysProps;
        Environment.commandLine = commandLine;
        Environment.standardOptions = standardOptions;

    }

    private static Map<String, String> userSystemProperties() {
        final Map<String, String> result = new HashMap<>();
        final Path userPropFile = JkLocator.jerkarUserHomeDir().resolve("system.properties");
        if (Files.exists(userPropFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }


    private static Map<String, String> getSpecifiedSystemProps() {
        final Map<String, String> result = new TreeMap<>();
        final Path propFile = JkLocator.jerkarHomeDir().resolve("system.properties");
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

        boolean logVerbose;

        boolean logHeaders;

        int logMaxLength = -1;

        String buildClass;

        StandardOptions (Map<String, String> map) {
            this.logVerbose = valueOf(Boolean.class, map, false, "Log.verbose", "LV");
            this.logHeaders = valueOf(Boolean.class, map, false,"Log.headers", "LH");
            this.logMaxLength = valueOf(Integer.class, map, -1,"Log.maxLength", "LML");
            this.buildClass = valueOf(String.class, map, null, "BuildClass", "BC");
        }

        @Override
        public String toString() {
            return "BuildClass=" + JkUtilsObject.toString(buildClass) + ", Log.Verbose=" + logVerbose
                    + ", Log.headers=" + logHeaders + ", Log.maxLength=" + logMaxLength;
        }

        private static <T> T valueOf(Class<T> type, Map<String, String> map, T defaultValue, String ... names) {
            for (String name : names) {
                if (map.containsKey(name)) {
                    String stringValue = map.get(name);
                    try {
                        return (T) OptionInjector.parse(type, stringValue);
                    } catch (IllegalArgumentException e) {
                        throw new JkException("Option " + name + " has been set with improper value '" + stringValue + "'");
                    }
                }
            }
            return defaultValue;
        }

    }

}
