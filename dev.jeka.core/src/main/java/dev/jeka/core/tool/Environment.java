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

    static CmdLine cmdLine = CmdLine.parse(new String[0]);

    static LogSettings logs = new LogSettings(false, false, false, false, false, false, JkLog.Style.FLAT, false, false);

    static BehaviorSettings behavior = new BehaviorSettings(null, false, false, false);

    static String[] originalArgs;

    static void initialize(String[] commandLineArgs) {
        originalArgs = commandLineArgs;

        JkProperties props = JkRunbase.readProjectPropertiesRecursively(Paths.get(""));
        String[] effectiveCommandLine = interpolatedCommandLine(commandLineArgs, props);


        // Parse command line
        final CmdLine parsedCmdLine = CmdLine.parse(effectiveCommandLine);

        final Map<String, String> optionProps = parsedCmdLine.getStandardOptions();

        // Set defaultKBean from properties if it has not been defined in cmd line
        if (!CmdLineOptions.isDefaultKBeanDefined(optionProps)) {
            optionProps.put(KB_KEYWORD, JkUtilsString.blankToNull(props.get(DEFAULT_KBEAN_PROP)));
        }

        final CmdLineOptions cmdLineOptions = new CmdLineOptions(optionProps, parsedCmdLine.rawArgs());

        logs = LogSettings.from(cmdLineOptions);
        behavior = BehaviorSettings.from(cmdLineOptions);
        cmdLine = parsedCmdLine;

        if (logs.verbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }
        if (logs.ivyVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.QUITE_VERBOSE);
        }
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

    static class BehaviorSettings {

        final String kbeanName;

        final boolean cleanWork;

        final boolean cleanOutput;

        final boolean ignoreCompileFailure;


        BehaviorSettings(String kbeanName, boolean cleanWork, boolean cleanOutput, boolean ignoreCompileFailure) {
            this.kbeanName = kbeanName;
            this.cleanWork = cleanWork;
            this.cleanOutput = cleanOutput;
            this.ignoreCompileFailure = ignoreCompileFailure;
        }

        static BehaviorSettings from(CmdLineOptions cmdLineOptions) {
            return new BehaviorSettings(
                    cmdLineOptions.kbeanName(),
                    cmdLineOptions.cleanWork.isPresent(),
                    cmdLineOptions.cleanOutput.isPresent(),
                    cmdLineOptions.ignoreCompileFail.isPresent()
            );
        }

        static boolean isDefaultKBeanDefined(Map<String, String> map) {
            return map.containsKey(Environment.KB_KEYWORD) || map.containsKey("kbean");
        }

    }

    // Log settings
    static class LogSettings {

        final boolean verbose;

        final boolean ivyVerbose;

        final boolean startUp;

        final boolean stackTrace;

        final boolean runtimeInformation;

        final boolean totalDuration;

        final JkLog.Style style;

        final Boolean animation;

        final boolean banner;

        public LogSettings(boolean verbose, boolean ivyVerbose, boolean startUp,
                           boolean stackTrace, boolean runtimeInformation, boolean totalDuration,
                           JkLog.Style style, Boolean animation, boolean banner) {
            this.verbose = verbose;
            this.ivyVerbose = ivyVerbose;
            this.startUp = startUp;
            this.stackTrace = stackTrace;
            this.runtimeInformation = runtimeInformation;
            this.totalDuration = totalDuration;
            this.style = style;
            this.animation = animation;
            this.banner = banner;
        }

        static LogSettings from(CmdLineOptions cmdLineOptions) {
            return new LogSettings(
                    cmdLineOptions.logVerbose.isPresent(),
                    cmdLineOptions.logIvyVerbose.isPresent(),
                    cmdLineOptions.logStartUp.isPresent(),
                    cmdLineOptions.logStackTrace.isPresent(),
                    cmdLineOptions.logRuntimeInformation.isPresent(),
                    cmdLineOptions.logDuration.isPresent(),
                    cmdLineOptions.logStyle,
                    cmdLineOptions.logAnimation,
                    cmdLineOptions.logBanner.isPresent()
            );
        }


    }

   private static String[] interpolatedCommandLine(String[] original, JkProperties props) {

        List<String> effectiveCommandLineArgs = new LinkedList<>(Arrays.asList(original));

        // Add arguments contained in local.properties 'jeka.cmd._appendXXXX'
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

        return effectiveCommandLineArgs.toArray(new String[0]);
    }



}
