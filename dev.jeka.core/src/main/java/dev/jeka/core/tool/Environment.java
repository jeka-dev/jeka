package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static dev.jeka.core.tool.JkConstants.*;

class Environment {

    static final String KB_KEYWORD = "kb";

    private Environment() {
        // Can't be instantiated
    }

    static CommandLine commandLine = CommandLine.parse(new String[0]);

    static StandardOptions standardOptions = new StandardOptions(Collections.emptyMap());

    static String[] originalArgs;

    static void initialize(String[] commandLineArgs) {
        originalArgs = commandLineArgs;
        List<String> effectiveCommandLineArgs = new LinkedList<>(Arrays.asList(commandLineArgs));

        // Add arguments contained in local.properties 'jeka.cmd._appendXXXX'
        JkProperties props = JkRunbase.readProjectPropertiesRecursively(Paths.get(""));
        List<String> appendedArgs = props.getAllStartingWith(CMD_APPEND_PROP, true).keySet().stream()
                .sorted()
                .map(props::get)
                .flatMap(value -> Arrays.stream(JkUtilsString.parseCommandline(value)))
                .collect(Collectors.toList());
        effectiveCommandLineArgs.addAll(appendedArgs);

        // Interpolate arguments passed as $key to respective value
        for (ListIterator<String> it = effectiveCommandLineArgs.listIterator(); it.hasNext(); ) {
            String word = it.next();
            if (word.startsWith(CMD_SUBSTITUTE_SYMBOL)) {
                String token = word.substring(CMD_SUBSTITUTE_SYMBOL.length());
                String propName = CMD_PREFIX_PROP + token;
                String presetValue = props.get(propName);
                if (presetValue != null) {
                    String[] replacingItems = JkUtilsString.parseCommandline(presetValue);
                    it.remove();
                    Arrays.stream(replacingItems).forEach(item -> it.add(item));
                }
            }
        }
        JkLog.trace("Effective command line : " + effectiveCommandLineArgs);

        // Parse command line
        final CommandLine commandLine = CommandLine.parse(effectiveCommandLineArgs.toArray(new String[0]));

        final Map<String, String> optionProps = commandLine.getStandardOptions();

        // Set defaultKBean from properties if it has not been defined in cmd line
        if (!StandardOptions.isDefaultKBeanDefined(optionProps)) {
            optionProps.put(KB_KEYWORD, JkUtilsString.blankToNull(props.get(DEFAULT_KBEAN_PROP)));
        }

        final StandardOptions standardOptions = new StandardOptions(optionProps);
        if (standardOptions.logVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }
        if (standardOptions.logIvyVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.QUITE_VERBOSE);
        }
        Environment.commandLine = commandLine;
        Environment.standardOptions = standardOptions;
    }

    static String originalCmdLineAsString() {
        return String.join(" ", originalArgs);
    }

    static boolean isPureHelpCmd() {
        return Environment.originalArgs.length == 1 &&
                (Environment.originalArgs[0].equals("--help") || Environment.originalArgs[0].equals("-h"));
    }

    static boolean isPureVersionCmd() {
        return Environment.originalArgs.length == 1 &&
                (Environment.originalArgs[0].equals("--version") || Environment.originalArgs[0].equals("-v"));
    }

}
