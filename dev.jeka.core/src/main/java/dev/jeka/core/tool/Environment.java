package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsFile;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

class Environment {

    private Environment() {
        // Can't be instantiated
    }

    static Map<String, String> systemProps = new HashMap<>();

    static CommandLine commandLine = CommandLine.parse(new String[0]);

    static StandardOptions standardOptions = new StandardOptions(Collections.emptyMap());

    static void initialize(String[] commandLineArgs) {


        List<String> effectiveCommandLineArgs = new LinkedList<>(Arrays.asList(commandLineArgs));

        // Add arguments contained in cmd.properties '_append'
        Map<String, String> presets = projectCmdProperties();
        List<String> appendedArgs = Arrays.asList(JkUtilsString.translateCommandline(presets.get("_append")));
        effectiveCommandLineArgs.addAll(appendedArgs);

        // Interpolate arguments passed as $key to respective value
        for (ListIterator<String> it = effectiveCommandLineArgs.listIterator(); it.hasNext(); ) {
            String word = it.next();
            if (word.startsWith("$")) {
                String presetValue = presets.get(word.substring(1));
                if (presetValue != null) {
                    String[] replacingItems = JkUtilsString.translateCommandline(presetValue);
                    it.remove();
                    Arrays.stream(replacingItems).forEach(item -> it.add(item));
                }
            }
        }
        JkLog.trace("Effective command line : " + effectiveCommandLineArgs);

        // Parse command line
        final CommandLine commandLine = CommandLine.parse(effectiveCommandLineArgs.toArray(new String[0]));


        // Take all defined system properties (command line, ofSystem.properties files) and
        // inject them in the system.
        final Map<String, String> sysProps = getSpecifiedSystemProps();
        sysProps.putAll(commandLine.getSystemProperties());
        setSystemProperties(sysProps);

        final Map<String, String> optionMap = new HashMap<>();
        optionMap.putAll(JkOptions.readSystemAndUserOptions());
        optionMap.putAll(JkOptions.readFromProjectOptionsProperties(Paths.get("")));
        optionMap.putAll(commandLine.getCommandOptions());
        JkOptions.init(optionMap);

        final StandardOptions standardOptions = new StandardOptions(optionMap);
        if (standardOptions.logVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        }
        if (standardOptions.logQuiteVerbose) {
            JkLog.setVerbosity(JkLog.Verbosity.QUITE_VERBOSE);
        }
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

        boolean logBanner;

        boolean logSetup;

        JkLog.Style logStyle;

        String logRuntimeInformation;

        private String jkClassName;

        private boolean forceCompile;

        private final Set<String> names = new HashSet<>();

        StandardOptions (Map<String, String> map) {
            this.logVerbose = valueOf(Boolean.class, map, false, "LogVerbose", "LV");
            this.logQuiteVerbose = valueOf(Boolean.class, map, false, "LogQuiteVerbose", "LQV");
            this.logBanner = valueOf(Boolean.class, map, false,"LogBanner", "LB");
            this.logSetup = valueOf(Boolean.class, map, false,"LogSetup", "LSU");
            this.logRuntimeInformation = valueOf(String.class, map, null, "LogRuntimeInformation", "LRI");
            this.logStyle = valueOf(JkLog.Style.class, map, JkLog.Style.INDENT, "LogStyle", "LS");
            this.jkClassName = valueOf(String.class, map, null, "JekaClass", "JKC");
            this.forceCompile = valueOf(Boolean.class, map, false, "ForceCompile", "FC");
        }

        Set<String> names() {
            return names;
        }

        String jkClassName() {
            if ("".equals(jkClassName)) {
                return JkConstants.DEFAULT_JEKA_CLASS.getName();
            }
            return jkClassName;
        }

        boolean forceCompile() {
            return forceCompile;
        }

        @Override
        public String toString() {
            return "JkClass=" + JkUtilsObject.toString(jkClassName) + ", LogVerbose=" + logVerbose
                    + ", LogHeaders=" + logBanner;
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

    private static Map<String, String> projectCmdProperties() {
        Path presetCommandsFile = Paths.get("jeka/cmd.properties");
        if (Files.exists(presetCommandsFile)) {
            return JkUtilsFile.readPropertyFileAsMap(presetCommandsFile);
        }
        return Collections.emptyMap();
    }



}
