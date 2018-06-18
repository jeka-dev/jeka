package org.jerkar.tool;

import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class OptionsAndCommandLine {

    Map<String, String> sysprops;

    CommandLine commandLine;

    StandardOptions standardOptions;

    static OptionsAndCommandLine loadOptionsAndSystemProps(String[] args) {
        final Map<String, String> sysProps = getSpecifiedSystemProps(args);
        setSystemProperties(sysProps);
        final Map<String, String> optionMap = new HashMap<>();
        optionMap.putAll(JkOptions.readSystemAndUserOptions());
        CommandLine.init(args);
        final CommandLine commandLine = CommandLine.instance();
        optionMap.putAll(commandLine.getBuildOptions());
        if (!JkOptions.isPopulated()) {
            JkOptions.init(optionMap);
        }
        final StandardOptions standardOptions = new StandardOptions();
        JkOptions.populateFields(standardOptions, optionMap);
        if (standardOptions.silent) {
            JkLog.setVerbosity(JkLog.Verbosity.MUTE);
        } else if (standardOptions.verbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }

        JkOptions.populateFields(standardOptions);
        final OptionsAndCommandLine loadResult = new OptionsAndCommandLine();
        loadResult.sysprops = sysProps;
        loadResult.commandLine = commandLine;
        loadResult.standardOptions = standardOptions;
        return loadResult;
    }

    private static Map<String, String> userSystemProperties() {
        final Map<String, String> result = new HashMap<>();
        final Path userPropFile = JkLocator.jerkarUserHomeDir().resolve("system.properties");
        if (Files.exists(userPropFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }


    private static Map<String, String> getSpecifiedSystemProps(String[] args) {
        final Map<String, String> result = new TreeMap<>();
        final Path propFile = JkLocator.jerkarHomeDir().resolve("system.properties");
        if (Files.exists(propFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        result.putAll(userSystemProperties());
        for (final String arg : args) {
            if (arg.startsWith("-D")) {
                final int equalIndex = arg.indexOf("=");
                if (equalIndex <= -1) {
                    result.put(arg.substring(2), "");
                } else {
                    final String name = arg.substring(2, equalIndex);
                    final String value = arg.substring(equalIndex + 1);
                    result.put(name, value);
                }
            }
        }
        return result;
    }

    private static void setSystemProperties(Map<String, String> props) {
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    static class StandardOptions {

        boolean verbose;

        boolean silent;

        String buildClass;

        @Override
        public String toString() {
            return "buildClass=" + JkUtilsObject.toString(buildClass) + ", verbose=" + verbose + ", silent=" + silent;
        }
    }

}
