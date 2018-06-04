package org.jerkar.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsString;

/*
 * Master and imported build concepts are relevant only in a multi-project build.
 * When doing a multi-project build there is always 1 master and 1 or many imported builds (1 imported build per project).
 *
 * Settings for the master project only are distinct from overall settings (master + imported builds).
 */
final class CommandLine {

    private static final String ALL_BUILD_SYMBOL = "*";

    private static final String PLUGIN_SYMBOL = "#";

    private static final char PLUGIN_SYMBOL_CHAR = '#';

    private static final String MODULE_SYMBOL_CHAR = "@";

    static CommandLine INSTANCE = of(new String[0]);

    public static CommandLine of(String[] words) {
        final CommandLine result = new CommandLine();
        result.masterBuildOptions = extractOptions(words, true);
        result.subProjectBuildOptions = extractOptions(words, false);
        result.masterMethods = extractMethods(words, true);
        result.subProjectMethods = extractMethods(words, false);
        result.masterPluginSetups = extractPluginSetup(words, true);
        result.subProjectPluginSetups = extractPluginSetup(words, false);
        result.buildDependencies = dependencies(words);
        return result;
    }

    private Map<String, String> masterBuildOptions;

    private Map<String, String> subProjectBuildOptions;

    private List<MethodInvocation> masterMethods;

    private List<MethodInvocation> subProjectMethods;

    private Collection<JkPluginSetup> masterPluginSetups;

    private Collection<JkPluginSetup> subProjectPluginSetups;

    private List<JkModuleDependency> buildDependencies;

    private CommandLine() {
        super();
    }

    private static List<JkModuleDependency> dependencies(String[] words) {
        final List<JkModuleDependency> result = new LinkedList<>();
        for (final String word : words) {
            if (word.startsWith(MODULE_SYMBOL_CHAR)) {
                final String depdef = word.substring(1);
                result.add(JkModuleDependency.of(depdef));
            }
        }
        return result;
    }

    private static List<MethodInvocation> extractMethods(String[] words, boolean master) {
        final List<MethodInvocation> result = new LinkedList<>();
        for (final String word : words) {
            if (!word.startsWith("-") && !word.startsWith("@") && !word.endsWith(PLUGIN_SYMBOL)
                    && !word.endsWith(PLUGIN_SYMBOL + ALL_BUILD_SYMBOL)) {
                if (word.endsWith(ALL_BUILD_SYMBOL)) {
                    final String trunc = JkUtilsString.substringBeforeLast(word, ALL_BUILD_SYMBOL);
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

    private static Map<String, String> extractOptions(String[] words, boolean master) {
        final Map<String, String> result = new HashMap<>();
        for (final String word : words) {
            if (word.startsWith("-") && !word.startsWith("-D")) {
                final int equalIndex = word.indexOf("=");
                if (equalIndex <= -1) { // no '=' so we just associate the key
                    // with a null value
                    final String key = word.substring(1);
                    if (!key.contains(PLUGIN_SYMBOL)) { // if '#' is present
                        // this means that it
                        // concerns plugin
                        // option, not build
                        // options
                        if (key.endsWith(ALL_BUILD_SYMBOL)) {
                            result.put(JkUtilsString.substringBeforeLast(key, ALL_BUILD_SYMBOL),
                                    null);
                        } else if (master) {
                            result.put(key, null);
                        }
                    }
                } else {
                    final String key = word.substring(1, equalIndex);
                    if (!key.contains(PLUGIN_SYMBOL)) {
                        final String value = word.substring(equalIndex + 1);
                        if (value.endsWith(ALL_BUILD_SYMBOL)) {
                            result.put(key,
                                    JkUtilsString.substringBeforeLast(value, ALL_BUILD_SYMBOL));
                        } else if (master) {
                            result.put(key, value);
                        }

                    }
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Collection<JkPluginSetup> extractPluginSetup(String words[], boolean master) {
        final Map<String, JkPluginSetup> setups = new HashMap<>();
        for (String word : words) {
            if (!word.endsWith(ALL_BUILD_SYMBOL) && !master) {
                continue;
            }
            if (word.endsWith(ALL_BUILD_SYMBOL)) {
                word = JkUtilsString.substringBeforeLast(word, ALL_BUILD_SYMBOL);
            }
            if (MethodInvocation.isPluginMethodInvokation(word)) {
                final String pluginName = JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL);
                if (!setups.containsKey(pluginName)) {
                    setups.put(pluginName, JkPluginSetup.of(pluginName));
                }
            } else if (MethodInvocation.isPluginActivation(word)) {
                final String pluginName = JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL);
                final JkPluginSetup setup = setups.get(pluginName);
                if (setup == null) {
                    setups.put(pluginName, JkPluginSetup.of(pluginName));
                } else {
                    setups.put(pluginName, setup.activated());
                }
            } else if (isPluginOption(word)) {
                final String pluginName = JkUtilsString.substringBeforeFirst(word, PLUGIN_SYMBOL)
                        .substring(1);
                JkPluginSetup setup = setups.computeIfAbsent(pluginName, n -> JkPluginSetup.of(n));
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
        return setups.values();
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
                    "PluginName can' t ne null or empty");
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

    public static class JkPluginSetup {



        @SuppressWarnings("unchecked")
        public static JkPluginSetup of(String name) {
            return new JkPluginSetup(name, Collections.EMPTY_MAP);
        }

        public final String pluginName;

        public final Map<String, String> options;

        private JkPluginSetup(String pluginName, Map<String, String> options) {
            super();
            this.pluginName = pluginName;
            this.options = Collections.unmodifiableMap(options);
        }

        public JkPluginSetup with(String key, String value) {
            final Map<String, String> map = new HashMap<>(options);
            map.put(key, value);
            return new JkPluginSetup(pluginName, map);
        }

        public JkPluginSetup activated() {
            return new JkPluginSetup(pluginName, options);
        }

        @Override
        public String toString() {
            return pluginName + " : " + options;
        }
    }

    public Map<String, String> getMasterBuildOptions() {
        return masterBuildOptions;
    }

    public Map<String, String> getSubProjectBuildOptions() {
        return subProjectBuildOptions;
    }

    public List<MethodInvocation> getMasterMethods() {
        return masterMethods;
    }

    public List<MethodInvocation> getSubProjectMethods() {
        return subProjectMethods;
    }

    public Collection<JkPluginSetup> getMasterPluginSetups() {
        return masterPluginSetups;
    }

    public Collection<JkPluginSetup> getSubProjectPluginSetups() {
        return subProjectPluginSetups;
    }

    public List<JkModuleDependency> dependencies() {
        return this.buildDependencies;
    }

}
