package org.jerkar.tool;

import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsString;

import java.util.*;

/*
 * Holds information carried by the command line.
 */
final class CommandLine {

    private static final String ALL_RUN_SYMBOL = "*";

    private static final String PLUGIN_SYMBOL = "#";

    private static final char PLUGIN_SYMBOL_CHAR = '#';

    private static final String MODULE_SYMBOL_CHAR = "@";

    private static CommandLine INSTANCE = null;

    static CommandLine parse(String[] words) {
        final CommandLine result = new CommandLine();
        result.runOptions = extractOptions(words);
        result.systemProperties = extractSystemProperties(words);
        result.masterMethods = extractMethods(words, true);
        result.subProjectMethods = extractMethods(words, false);
        result.pluginOptions = extractPluginOptions(words);
        result.runDependencies = dependencies(words);
        result.rawArgs = words;
        return result;
    }

    private Map<String, String> runOptions;

    private Map<String, String> systemProperties;

    private List<MethodInvocation> masterMethods;

    private List<MethodInvocation> subProjectMethods;

    private List<PluginOptions> pluginOptions;

    private List<JkModuleDependency> runDependencies;

    private String[] rawArgs;

    private CommandLine() {
        super();
    }

    private static List<JkModuleDependency> dependencies(String[] words) {
        final List<JkModuleDependency> result = new LinkedList<>();
        for (final String word : words) {
            if (word.startsWith(MODULE_SYMBOL_CHAR)) {
                final String dependencyDef = word.substring(1);
                result.add(JkModuleDependency.of(dependencyDef));
            }
        }
        return result;
    }

    private static List<MethodInvocation> extractMethods(String[] words, boolean master) {
        final List<MethodInvocation> result = new LinkedList<>();
        for (final String word : words) {
            if (!word.startsWith("-") && !word.startsWith("@") && !word.endsWith(PLUGIN_SYMBOL)
                    && !word.endsWith(PLUGIN_SYMBOL + ALL_RUN_SYMBOL)) {
                if (word.endsWith(ALL_RUN_SYMBOL)) {
                    final String trunc = JkUtilsString.substringBeforeLast(word, ALL_RUN_SYMBOL);
                    result.add(MethodInvocation.parse(trunc));
                } else if (master) {
                    result.add(MethodInvocation.parse(word));
                }
            }
        }
        if (result.isEmpty() && master) {
            result.add(MethodInvocation.normal(JkConstants.DEFAULT_METHOD));
        }
        return result;
    }

    private static Map<String, String> extractOptions(String[] words) {
        final Map<String, String> result = new HashMap<>();
        for (final String word : words) {
            if (word.startsWith("-") && !word.startsWith("-D")) {
                final int equalIndex = word.indexOf("=");
                if (equalIndex <= -1) { // no '=' so we just associate the key with a null value
                    final String key = word.substring(1);
                    if (!key.contains(PLUGIN_SYMBOL)) { // if '#' is present
                        result.put(key, null);
                    }
                } else {
                    final String key = word.substring(1, equalIndex);
                    if (!key.contains(PLUGIN_SYMBOL)) {
                        final String value = word.substring(equalIndex + 1);
                        result.put(key, value);
                    }
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> extractSystemProperties(String[] args) {
        final Map<String, String> result = new HashMap<>();
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
        return Collections.unmodifiableMap(result);
    }

    private static List<PluginOptions> extractPluginOptions(String words[]) {
        final Map<String, PluginOptions> setups = new LinkedHashMap<>();
        for (String word : words) {
            if (MethodInvocation.isPluginMethodInvokation(word)) {
                final String pluginName = JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL);
                if (!setups.containsKey(pluginName)) {
                    setups.put(pluginName, PluginOptions.of(pluginName));
                }
            } else if (MethodInvocation.isPluginActivation(word)) {
                final String pluginName = JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL);
                final PluginOptions setup = setups.get(pluginName);
                if (setup == null) {
                    setups.put(pluginName, PluginOptions.of(pluginName));
                } else {
                    setups.put(pluginName, setup);
                }
            } else if (isPluginOption(word)) {
                final String pluginName = JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL)
                        .substring(1);
                PluginOptions setup = setups.computeIfAbsent(pluginName, n -> PluginOptions.of(n));
                final int equalIndex = word.indexOf("=");
                if (equalIndex <= -1) {
                    final String key = JkUtilsString.substringAfterFirst(word, PLUGIN_SYMBOL);
                    setups.put(pluginName, setup.with(key, null));
                } else {
                    final String key = JkUtilsString.substringBeforeFirst(
                            JkUtilsString.substringAfterFirst(word, PLUGIN_SYMBOL), "=");
                    final String value = word.substring(equalIndex + 1);
                    setups.put(pluginName, setup.with(key, value));
                }
            }
        }
        return new LinkedList<>(setups.values());
    }

    private static boolean isPluginOption(String word) {
        return word.startsWith("-") && word.indexOf(PLUGIN_SYMBOL) > 2;
    }

    static final class MethodInvocation {

        public static MethodInvocation parse(String word) {
            if (isPluginMethodInvokation(word)) {
                return pluginMethod(JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL),
                        JkUtilsString.substringAfterLast(word, PLUGIN_SYMBOL));
            }
            return normal(word);
        }

        public static MethodInvocation normal(String name) {
            return new MethodInvocation(name, null);
        }

        public static MethodInvocation pluginMethod(String pluginName, String methodName) {
            JkUtilsAssert.isTrue(pluginName != null && !pluginName.isEmpty(),
                    "PluginName can't be null or empty");
            return new MethodInvocation(methodName, pluginName);
        }

        public final String methodName;

        public final String pluginName;

        private MethodInvocation(String methodName, String pluginName) {
            super();
            JkUtilsAssert.isTrue(methodName != null && !methodName.isEmpty(),
                    "PluginName can' t be null or empty");
            this.methodName = methodName;
            this.pluginName = pluginName;
        }

        private static boolean isPluginMethodInvokation(String word) {
            if (word.startsWith("-")) {
                return false;
            }
            return JkUtilsString.countOccurence(word, PLUGIN_SYMBOL_CHAR) == 1
                    && !word.startsWith(PLUGIN_SYMBOL) && !word.endsWith(PLUGIN_SYMBOL);
        }

        private static boolean isPluginActivation(String word) {
            return JkUtilsString.countOccurence(word, PLUGIN_SYMBOL_CHAR) == 1
                    && !word.startsWith(PLUGIN_SYMBOL) && word.endsWith(PLUGIN_SYMBOL);
        }

        public boolean isMethodPlugin() {
            return pluginName != null;
        }

        @Override
        public String toString() {
            if (pluginName == null) {
                return methodName;
            }
            return pluginName + "#" + methodName;
        }

    }

    Map<String, String> getOptions() {
        return runOptions;
    }

    Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    List<MethodInvocation> getMasterMethods() {
        return masterMethods;
    }

    List<MethodInvocation> getSubProjectMethods() {
        return subProjectMethods;
    }

    List<PluginOptions> getPluginOptions() {
        return pluginOptions;
    }

    List<JkModuleDependency> dependencies() {
        return this.runDependencies;
    }

    String[] rawArgs() {
        return rawArgs;
    }

}
