package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Contains description for all concrete plugins in current classpath.
 * <p>
 * Jeka offers a very simple, yet powerful, plugin mechanism.<br/>
 * Basically it offers to discover every classes in the classpath that inherit
 * to a given class and that respect a certain naming convention.<br/>
 * <p>
 * The naming convention is as follow : The class simple name should be prefixed
 * with 'JkBean'.<br/>
 * For example, 'my.package.JkPluginXxxxx' will be discovered as a plugin named Xxxxx.
 *
 * @author Jerome Angibaud
 * 
 * @see {@link KBeanDescription}
 */
final class KBeanDictionary {

    private static final Map<String, KBeanDescription> SHORTNAME_CACHE = new LinkedHashMap<>();

    private Set<KBeanDescription> jkBeans;

    /**
     * Returns all the plugins present in classpath for this template class.
     */
    Set<KBeanDescription> getAll() {
        if (jkBeans == null) {
            synchronized (this) {
                final Set<KBeanDescription> result = loadAllPlugins();
                this.jkBeans = Collections.unmodifiableSet(result);
            }
        }
        return this.jkBeans;
    }

    /**
     * Returns the plugin having the specified name.
     * If the specified name can be a short name (like 'myPlugin') or a full class name
     */
    static KBeanDescription loadByName(String name) {
        if (isShortName(name)) {
            final KBeanDescription result = loadPluginHavingShortName(name);
            if (result != null) {
                return result;
            }
        }
        return loadPluginsHavingLongName(name);
    }

    private static String simpleClassName(String pluginName) {
        return JkUtilsString.capitalize(pluginName) + JkBean.class.getSimpleName();
    }

    private static boolean isShortName(String name) {
        return !name.contains(".") && !name.endsWith(JkBean.class.getSimpleName());
    }

    @Override
    public String toString() {
        if (this.jkBeans == null) {
            return "Not loaded.";
        }
        return this.jkBeans.toString();
    }

    private static <T> Set<KBeanDescription> loadAllPlugins() {
        final String nameSuffix = JkBean.class.getSimpleName();
        Set<Class<?>> candidates = JkInternalClasspathScanner.INSTANCE
                .loadClassesHavingSimpleNameMatching(name -> name.endsWith(nameSuffix));
        Set<KBeanDescription> result = toJkBeanDescriptions(candidates);
        for(KBeanDescription KBeanDescription : result) {
            SHORTNAME_CACHE.put(KBeanDescription.shortName, KBeanDescription);
        }
        return result;
    }

    private static KBeanDescription loadPluginHavingShortName(String shortName) {
        KBeanDescription result = SHORTNAME_CACHE.get(shortName);
        if (result != null) {
            return result;
        }
        final String simpleName = simpleClassName(shortName);
        Set<Class<?>> classes = JkInternalClasspathScanner.INSTANCE.loadClassesHavingSimpleName(simpleName );
        final Set<KBeanDescription> set = toJkBeanDescriptions(classes);
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

    private static KBeanDescription loadPluginsHavingLongName(String longName) {
        final Class<? extends JkBean> pluginClass = JkClassLoader.ofCurrent().loadIfExist(longName);
        if (pluginClass == null) {
            return null;
        }
        return new KBeanDescription(pluginClass);
    }

    private static Set<KBeanDescription> toJkBeanDescriptions(Set<Class<?>> matchingClasses) {
        Set<Class<?>> jkBeanClasses = matchingClasses.stream()
                .filter(clazz -> JkBean.class.isAssignableFrom(clazz))
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet());
        return toPluginSet(jkBeanClasses);
    }

    @SuppressWarnings("unchecked")
    private static Set<KBeanDescription> toPluginSet(Iterable<Class<?>> classes) {
        final Set<KBeanDescription> result = new TreeSet<>();
        for (final Class<?> clazz : classes) {
            result.add(new KBeanDescription((Class<? extends JkBean>) clazz));
        }
        return result;
    }

    /**
     * Gives the description of a KBean : its name, its purpose and its base class.
     * 
     * @author Jerome Angibaud
     */
    static class KBeanDescription implements Comparable<KBeanDescription> {

        private static String longName(Class<?> clazz) {
            return clazz.getName();
        }

        private final String shortName;

        private final String fullName;

        private final Class<? extends JkBean> clazz;

        KBeanDescription(Class<? extends JkBean> clazz) {
            super();
            this.shortName = JkBean.shortName(clazz);
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

        public Class<? extends JkBean> pluginClass() {
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
            return  decorateRun != null && !decorateRun.getDeclaringClass().equals(JkBean.class);
        }

        @Override
        public String toString() {
            return "name=" + this.shortName + "(" + this.fullName + ")";
        }

        @Override
        public int compareTo(KBeanDescription o) {
            return this.shortName.compareTo(o.shortName);
        }
    }

}
