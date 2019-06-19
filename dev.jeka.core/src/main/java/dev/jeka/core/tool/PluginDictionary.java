package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains description for all concrete plugins in current classpath.
 * <p>
 * Jeka offers a very simple, yet powerful, plugin mechanism.<br/>
 * Basically it offers to discover every classes in the classpath that inherit
 * to a given class and that respect a certain naming convention.<br/>
 * <p>
 * The naming convention is as follow : The class simple name should be prefixed
 * with 'JkPlugin'.<br/>
 * For example, 'my.package.JkPluginXxxxx' will be discovered as a plugin named Xxxxx.
 *
 * @author Jerome Angibaud
 * 
 * @see {@link PluginDescription}
 */
final class PluginDictionary {

    private static final Map<String, PluginDescription> SHORTNAME_CACHE = new LinkedHashMap<>();

    private Set<PluginDescription> plugins;

    /**
     * Returns all the plugins present in classpath for this template class.
     */
    Set<PluginDescription> getAll() {
        if (plugins == null) {
            synchronized (this) {
                final Set<PluginDescription> result = loadAllPlugins();
                this.plugins = Collections.unmodifiableSet(result);
            }
        }
        return this.plugins;
    }

    /**
     * Returns the plugin having the specified name.
     * If the specified name can be a short name (like 'myPlugin') or a full class name
     */
    static PluginDescription loadByName(String name) {
        if (isShortName(name)) {
            final PluginDescription result = loadPluginHavingShortName(name);
            if (result != null) {
                return result;
            }
        }
        return loadPluginsHavingLongName(name);
    }

    private static String simpleClassName(String pluginName) {
        return JkPlugin.class.getSimpleName() + JkUtilsString.capitalize(pluginName);
    }

    private static boolean isShortName(String name) {
        return !name.contains(".") && !name.startsWith(JkPlugin.class.getSimpleName());
    }

    @Override
    public String toString() {
        if (this.plugins == null) {
            return "Not loaded.";
        }
        return this.plugins.toString();
    }

    private static <T> Set<PluginDescription> loadAllPlugins() {
        final String nameSuffix = JkPlugin.class.getSimpleName();
        Set<PluginDescription> result = toPluginDescriptions(JkInternalClasspathScanner.INSTANCE
                .loadClassesHavingSimpleNameMatching(name -> name.startsWith(nameSuffix)));
        for(PluginDescription pluginDescription : result) {
            SHORTNAME_CACHE.put(pluginDescription.shortName, pluginDescription);
        }
        return result;
    }

    private static PluginDescription loadPluginHavingShortName(String shortName) {
        PluginDescription result = SHORTNAME_CACHE.get(shortName);
        if (result != null) {
            return result;
        }
        final String simpleName = simpleClassName(shortName);
        Set<Class<?>> classes = JkInternalClasspathScanner.INSTANCE.loadClassesHavingSimpleName(simpleName );
        final Set<PluginDescription> set = toPluginDescriptions(classes);
        if (set.size() > 1) {
            throw new JkException("Several plugin have the same short name : '" + shortName
                    + "'. Please disambiguate with using plugin long name (full class value)."
                    + " Following plugins have same shortName : " + set);
        }
        if (set.isEmpty()) {
            return null;
        }
        result = set.iterator().next();
        SHORTNAME_CACHE.put(shortName, result);
        return result;
    }

    private static PluginDescription loadPluginsHavingLongName(String longName) {
        final Class<? extends JkPlugin> pluginClass = JkClassLoader.ofCurrent().loadIfExist(longName);
        if (pluginClass == null) {
            return null;
        }
        return new PluginDescription(pluginClass);
    }

    private static Set<PluginDescription> toPluginDescriptions(Set<Class<?>> matchingClasses) {
        return toPluginSet(matchingClasses.stream()
                .filter(clazz -> JkPlugin.class.isAssignableFrom(clazz))
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet()));
    }

    @SuppressWarnings("unchecked")
    private static Set<PluginDescription> toPluginSet(Iterable<Class<?>> classes) {
        final Set<PluginDescription> result = new TreeSet<>();
        for (final Class<?> clazz : classes) {
            result.add(new PluginDescription((Class<? extends JkPlugin>) clazz));
        }
        return result;
    }

    /**
     * Give the description of a plugin class as its name, its purpose and its
     * base class.
     * 
     * @author Jerome Angibaud
     */
    static class PluginDescription implements Comparable<PluginDescription> {

        private static String shortName(Class<?> clazz) {
            return JkUtilsString.uncapitalize(JkUtilsString.substringAfterFirst(clazz.getSimpleName(),
                    JkPlugin.class.getSimpleName()));
        }

        private static String longName(Class<?> clazz) {
            return clazz.getName();
        }

        private final String shortName;

        private final String fullName;

        private final Class<? extends JkPlugin> clazz;

        public PluginDescription(Class<? extends JkPlugin> clazz) {
            super();
            this.shortName = shortName(clazz);
            this.fullName = longName(clazz);
            this.clazz = clazz;
        }

        public List<String> pluginDependencies() {
            List<String> result = new LinkedList<>();
            JkDocPluginDeps pluginDeps = clazz.getAnnotation(JkDocPluginDeps.class);
            if (pluginDeps == null) {
                return Collections.emptyList();
            }
            for (Class<?> depClass : pluginDeps.value()) {
                result.add(depClass.getName());
            }
            return result;
        }

        public String shortName() {
            return this.shortName;
        }

        public String fullName() {
            return this.fullName;
        }

        public Class<? extends JkPlugin> pluginClass() {
            return clazz;
        }

        public List<String> explanation() {
            if (this.clazz.getAnnotation(JkDoc.class) == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(this.clazz.getAnnotation(JkDoc.class).value());
        }

        public List<String> activationEffect() {
            JkDoc doc = JkUtilsReflect.getInheritedAnnotation(clazz,  JkDoc.class, "activate");
            return doc == null ? Collections.emptyList() : Arrays.asList(doc.value());
        }

        boolean isDecorateRunDefined() {
            Method decorateRun = JkUtilsReflect.findMethodMethodDeclaration(clazz, "activate");
            return  decorateRun != null && !decorateRun.getDeclaringClass().equals(JkPlugin.class);
        }

        @Override
        public String toString() {
            return "name=" + this.shortName + "(" + this.fullName + ")";
        }

        @Override
        public int compareTo(PluginDescription o) {
            return this.shortName.compareTo(o.shortName);
        }
    }

}
