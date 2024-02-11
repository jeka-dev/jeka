package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsObject;
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

    static ParsedCmdLine parsedCmdLine = ParsedCmdLine.parse(new String[0]);

    static EnvLogSettings logs = new EnvLogSettings(false, false, false, false, false, false, JkLog.Style.INDENT, false, false);

    static EnvBehaviorSettings behavior = new EnvBehaviorSettings(null, false, false, false);

    // TODO remove after picocli migration
    static String[] originalArgs;

    static void initialize(String[] commandLineArgs) {
        originalArgs = commandLineArgs;

        JkProperties props = JkRunbase.readProjectPropertiesRecursively(Paths.get(""));
        String[] effectiveCommandLine = interpolatedCommandLine(commandLineArgs, props);


        // Parse command line
        final ParsedCmdLine cmdLine = ParsedCmdLine.parse(effectiveCommandLine);

        final Map<String, String> optionProps = cmdLine.getStandardOptions();

        // Set defaultKBean from properties if it has not been defined in cmd line
        if (!CmdLineOptions.isDefaultKBeanDefined(optionProps)) {
            optionProps.put(KB_KEYWORD, JkUtilsString.blankToNull(props.get(DEFAULT_KBEAN_PROP)));
        }

        final CmdLineOptions cmdLineOptions = new CmdLineOptions(optionProps, cmdLine.rawArgs());

        logs = createLogSettings(cmdLineOptions);
        behavior = createBehaviorSettings(cmdLineOptions);
        parsedCmdLine = cmdLine;

        if (logs.verbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }
        if (logs.ivyVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.DEBUG);
        }
    }

    private static EnvLogSettings createLogSettings(CmdLineOptions cmdLineOptions) {
        return new EnvLogSettings(
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

    private static EnvBehaviorSettings createBehaviorSettings(CmdLineOptions cmdLineOptions) {
        return new EnvBehaviorSettings(
                cmdLineOptions.kbeanName(),
                cmdLineOptions.cleanWork.isPresent(),
                cmdLineOptions.cleanOutput.isPresent(),
                cmdLineOptions.ignoreCompileFail.isPresent()
        );
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

    static String[] interpolatedCommandLine(String[] original, JkProperties props) {

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
        JkLog.verbose("Effective command line : " + effectiveCommandLineArgs);

        return effectiveCommandLineArgs.toArray(new String[0]);
    }


    /*
     * TODO remove after picoCli migration
     * Options accepted by command-line interface
     */
    private static class CmdLineOptions {

        private static final List<Option<?>> ALL_OPTIONS = new LinkedList<>();

        Set<String> acceptedOptions = new HashSet<>();

        final Option<Void> logIvyVerbose = ofVoid("Log Ivy 'trace' level", "--log-ivy-verbose", "-liv");

        final Option<Void> logVerbose = ofVoid("Log verbose messages", "--verbose", "-v");

        final Option<Void> logStackTrace = ofVoid("log the stacktrace when Jeka fail",
                "--log-stacktrace", "-lst");

        final Option<Void> logBanner = ofVoid("log intro and outro banners", "--log-banner", "-lb");

        final Option<Void> logDuration = ofVoid("Log intro and outro banners", "--log-duration", "-ld");

        final Option<Void> logStartUp = ofVoid("Log start-up information happening prior command executions",
                                                      "--log-startup", "-lsu");

        final Option<Void> logRuntimeInformation = ofVoid("log Jeka runbase information as Jeka version, JDK version, working dir, classpath ...",
                "--log-runtime-info", "-lri");

        JkLog.Style logStyle;

        Boolean logAnimation;

        private final String kbeanName;

        // behavioral option

        final Option<Void> cleanWork = ofVoid("Clean 'jeka-work' directory prior running.", "--clean-work", "-cw");

        final Option<Void> cleanOutput = ofVoid("Clean '.jeka-output' directory prior running.", "--clean-output", "-co");

        final Option<Void> ignoreCompileFail = ofVoid("Ignore when 'jeka-src compile fails", "--ignore-compile-fail", "-dci");


        private final Set<String> names = new HashSet<>();

        CmdLineOptions(Map<String, String> map, String[] rawArgs) {
            populateOptions(Arrays.asList(rawArgs));

            this.logAnimation = valueOf(boolean.class, map, null, "log.animation", "la");
            this.logStyle = valueOf(JkLog.Style.class, map, JkLog.Style.INDENT, "log.style", "ls");
            this.kbeanName = valueOf(String.class, map, null, "kbean", KB_KEYWORD);

        }

        static boolean isDefaultKBeanDefined(Map<String, String> map) {
            return map.containsKey(KB_KEYWORD) || map.containsKey("kbean");
        }

        String kbeanName() {
            return kbeanName;
        }

        @Override
        public String toString() {
            return "JkBean" + JkUtilsObject.toString(kbeanName) + ", LogVerbose=" + logVerbose
                    + ", LogHeaders=" + logBanner;
        }

        private <T> T valueOf(Class<T> type, Map<String, String> map, T defaultValue, String... optionNames) {
            for (String name : optionNames) {
                acceptedOptions.add(name);
                this.names.add(name);
                if (map.containsKey(name)) {
                    String stringValue = map.get(name);
                    if (type.equals(boolean.class) && stringValue == null) {
                        return (T) Boolean.TRUE;
                    }
                    try {
                        return (T) FieldInjector.parse(type, stringValue);
                    } catch (IllegalArgumentException e) {
                        throw new JkException("Property " + name + " has been set with improper value '"
                                + stringValue + "' : " + e.getMessage());
                    }
                }
            }
            return defaultValue;
        }

        private <T> Option<T> of(Class<T> type, T initialValue, String description, String ...names) {
            Option<T> option = Option.of(type, initialValue, description, names);
            ALL_OPTIONS.add(option);
            return option;
        }

        private Option<Void> ofVoid(String description, String ...names) {
            Option<Void> option = Option.of(Void.class, null, description, names);
            ALL_OPTIONS.add(option);
            return option;
        }

        private <T extends Enum<?>> Option<T> ofEnum(String description, T value, String ...names) {
            Option<T> option = Option.of((Class<T>) value.getClass(), value, description, names);
            ALL_OPTIONS.add(option);
            return option;
        }

        private static void populateOptions(List<String> args) {
            ALL_OPTIONS.forEach(option -> option.populateFrom(args));
        }

        static class Option<T> {

            private static final List<Option> ALL = new LinkedList<>();

            private T value;

            private Class<T> type;

            private boolean present;

            public final List<String> names;

            private final String description;


            private Option(Class<T> type, T value, List<String> names, String description) {
                this.value = value;
                this.names = names;
                this.description = description;
            }

            public static <T> Option<T> of(Class<T> type, T initialValue, String description, String ...names) {
                return new Option<>(type, initialValue, Collections.unmodifiableList(Arrays.asList(names)), description);
            }

            public boolean isPresent() {
                return present;
            }

            public T getValue() {
                return value;
            }

            private void populateFrom(List<String> args) {
                for (ListIterator<String> it = args.listIterator(); it.hasNext();) {
                    String arg = it.next();
                    if (names.contains(arg)) {
                        this.present = true;
                    }
                    if (Void.class.equals(type)) {
                        return;
                    }
                }
            }

        }

    }
}
