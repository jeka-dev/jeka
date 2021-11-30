package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/*
 * A resolver to determine witch {@link JkBean} class to use as default or according KBean short name.
 *
 * @author Jerome Angibaud
 */
final class EngineBeanClassResolver {

    private static final String JAVA_HOME = JkUtilsPath.toUrl(Paths.get(System.getProperty("java.home"))).toString();

    private final Path baseDir;

    final Path defSourceDir;

    final Path defClassDir;

    private JkPathSequence classpath;

    private List<String> cachedDefBeanClassNames;

    private List<String> cachedGlobalBeanClassName;

    private static final Comparator<Class> CLASS_NAME_COMPARATOR = Comparator.comparing(Class::getName);

    EngineBeanClassResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.defSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.defClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    List<EngineCommand> resolve(CommandLine commandLine, String defaultBeanName) {
        JkLog.startTask("Resolve KBean classes");
        Map<String, Class<? extends JkBean>> beanClasses = new HashMap<>();
        if (commandLine.containsDefaultBean()) {
            if (defaultBeanName == null) {
                if (defBeanClassNames().isEmpty()) {
                    throw new JkException("Cannot find any KBean in jeka/def dir. Use -KB=[beanName] to precise a " +
                            "bean present in classpath or create a class extending JkBean into jeka/def dir.");
                }
                Class<? extends JkBean> defaultBeanClass = defBeanClasses().get(0);
                beanClasses.put(null, defaultBeanClass);
            } else {
                List<String> matchingclassNames = findClassesMatchingName(defBeanClassNames(), defaultBeanName);
                if (matchingclassNames.isEmpty()) {
                    matchingclassNames = findClassesMatchingName(globalBeanClassNames(), defaultBeanName);
                }
                Class<? extends JkBean> selected = loadUniqueClassOrFail(matchingclassNames, defaultBeanName);
                beanClasses.put(null, selected);
            }
        } else {
            List<Class<? extends JkBean>> defaultBeanClasses = defBeanClasses();
            if (!defaultBeanClasses.isEmpty()) {
                beanClasses.put(null, defaultBeanClasses.get(0));
            }
        }
        for (String beanName : commandLine.involvedBeanNames()) {
            List<String> matchingClassNames = findClassesMatchingName(globalBeanClassNames(), beanName);
            Class<? extends JkBean> selected = loadUniqueClassOrFail(matchingClassNames, beanName);
            beanClasses.put(JkBean.name(selected), selected);
        }
        JkLog.endTask();
        return commandLine.getBeanActions().stream()
                .map(action -> toEngineCommand(action, beanClasses)).collect(Collectors.toList());
    }

    void setClasspath(JkPathSequence classpath) {
        this.classpath = classpath;
    }

    private static Class<? extends JkBean> loadUniqueClassOrFail(List<String> matchingBeanClasses, String beanName) {
        if (matchingBeanClasses.isEmpty()) {
            throw new JkException("Can not find a KBean named '" + beanName
                    + "'.\nThe name can be the fully qualified class name of the KBean, its uncapitalized "
                    + "simple class name or its uncapitalized simple class name without the 'JkBean' suffix.\n"
                    + "Current class loader is : " + JkClassLoader.ofCurrent().toString());
        } else if (matchingBeanClasses.size() > 1) {
            throw new JkException("Several classes matches default bean name '" + beanName + "' : "
                    + matchingBeanClasses + ". Please precise the fully qualified class name of the default bean " +
                    "instead of its short name.");
        } else {
            return JkClassLoader.ofCurrent().load(matchingBeanClasses.get(0));
        }
    }

    List<String> globalBeanClassNames() {
        if (cachedGlobalBeanClassName == null) {
            long t0 = System.currentTimeMillis();
            ClassLoader classLoader = JkClassLoader.ofCurrent().get();
            boolean ignoreParent = false;
            if (classpath != null) {

                // If classpath is set, then sources has been compiled in work dir
                classLoader = new URLClassLoader(classpath.toUrls());
                ignoreParent = true;
            }
            cachedGlobalBeanClassName = JkInternalClasspathScanner.INSTANCE
                    .findClassedExtending(classLoader, JkBean.class, path -> true, true, false);
            JkLog.trace("All JkBean classes scanned in " + (System.currentTimeMillis() - t0) + " ms.");
        }
        return cachedGlobalBeanClassName;
    }

    List<Class<? extends JkBean>> defBeanClasses() {
        List result = defBeanClassNames().stream()
                .sorted()
                .map(className -> JkClassLoader.ofCurrent().load(className))
                .collect(Collectors.toList());
        return result;
    }

    boolean hasDefSource() {
        if (!Files.exists(defSourceDir)) {
            return false;
        }
        return JkPathTree.of(defSourceDir).andMatching(true,
                "**.java", "*.java", "**.kt", "*.kt").count(0, false) > 0;
    }

    private List<String> defBeanClassNames() {
        if (cachedDefBeanClassNames == null) {
            long t0 = System.currentTimeMillis();
            ClassLoader classLoader = JkClassLoader.ofCurrent().get();
            boolean ignoreParent = false;
            if (classpath != null) {

                // If classpath is set, then sources has been compiled in work dir
                classLoader = new URLClassLoader(JkPathSequence.of().and(this.defClassDir).toUrls());
                ignoreParent = true;
            }
            cachedDefBeanClassNames = JkInternalClasspathScanner.INSTANCE.findClassedExtending(classLoader,
                    JkBean.class, EngineBeanClassResolver::scan, true, ignoreParent);
            JkLog.trace("Def JkBean classes scanned in " + (System.currentTimeMillis() - t0) + " ms.");
        }
        return cachedDefBeanClassNames;
    }

    private static boolean scan(String pathElement) {
        return !pathElement.startsWith(JAVA_HOME);  // Don't scan jre classes
        //return true;
    }

    private static EngineCommand toEngineCommand(CommandLine.JkBeanAction action,
                                                 Map<String, Class<? extends JkBean>> beanClasses) {
        Class<? extends JkBean> beanClass = beanClasses.get(action.beanName);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private static List<String> findClassesMatchingName(List<String> beanClassNameCandidates, String name) {
        return beanClassNameCandidates.stream()
                .filter(className -> JkBean.nameMatches(className, name))
                .collect(Collectors.toList());
    }

    private JkPathTree defSources() {
        return JkPathTree.of(this.defSourceDir).withMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

}
