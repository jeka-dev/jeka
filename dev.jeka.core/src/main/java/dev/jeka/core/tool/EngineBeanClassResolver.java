package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
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
        for (String beanName : commandLine.involvedBeanNames()) {
            List<String> matchingClassNames = findClassesMatchingName(globalBeanClassNames(), beanName);
            Class<? extends JkBean> selected = loadUniqueClassOrFail(matchingClassNames, beanName);
            beanClasses.put(JkBean.name(selected), selected);
        }
        Class<? extends JkBean> defaultBeanClass = defaultBeanClass(defaultBeanName);
        List<EngineCommand> result = new LinkedList<>();
        if (defaultBeanClass == null && commandLine.containsDefaultBean()) {
            throw new JkException("Cannot find any KBean in jeka/def dir. Use -kb=[beanName] to precise a " +
                    "bean present in classpath or create a class extending JkBean into jeka/def dir.");
        }
        beanClasses.put(null, defaultBeanClass);
        if (defaultBeanClass != null) {
            result.add(new EngineCommand(EngineCommand.Action.BEAN_REGISTRATION, defaultBeanClass,
                    null, null));
        }
        commandLine.getBeanActions().stream()
                .map(action -> toEngineCommand(action, beanClasses)).forEach(result::add);
        JkLog.endTask();
        return Collections.unmodifiableList(result);
    }

    void setClasspath(JkPathSequence classpath) {
        this.classpath = classpath;
    }

    private Class<? extends JkBean> defaultBeanClass(String defaultBeanName) {
        if (defaultBeanName == null) {
            if (defBeanClassNames().isEmpty()) {
                return null;
            }
            return defBeanClasses().get(0);
        }
        List<String> matchingclassNames = findClassesMatchingName(defBeanClassNames(), defaultBeanName);
        if (matchingclassNames.isEmpty()) {
            matchingclassNames = findClassesMatchingName(globalBeanClassNames(), defaultBeanName);
        }
        return loadUniqueClassOrFail(matchingclassNames, defaultBeanName);
    }

    private static Class<? extends JkBean> loadUniqueClassOrFail(List<String> matchingBeanClasses, String beanName) {
        if (matchingBeanClasses.isEmpty()) {
            throw beanClassNotFound(beanName);
        } else if (matchingBeanClasses.size() > 1) {
            throw new JkException("Several classes matches default bean name '" + beanName + "' : "
                    + matchingBeanClasses + ". Please precise the fully qualified class name of the default bean " +
                    "instead of its short name.");
        } else {
            return JkClassLoader.ofCurrent().load(matchingBeanClasses.get(0));
        }
    }

    private static JkException beanClassNotFound(String name) {
        return new JkException("Can not find a KBean named '" + name
                + "'.\nThe name can be the fully qualified class name of the KBean, its uncapitalized "
                + "simple class name or its uncapitalized simple class name without the 'JkBean' suffix.\n"
                + "Execute jeka -help to display available beans.");
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
            cachedGlobalBeanClassName = JkInternalClasspathScanner.of()
                    .findClassedExtending(classLoader, JkBean.class, path -> true, true, false);
            if (JkLog.isVerbose()) {
                JkLog.trace("All JkBean classes scanned in " + (System.currentTimeMillis() - t0) + " ms.");
                cachedGlobalBeanClassName.forEach(className -> JkLog.trace("  " + className));
            }
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
            cachedDefBeanClassNames = JkInternalClasspathScanner.of().findClassedExtending(classLoader,
                    JkBean.class, EngineBeanClassResolver::scan, true, ignoreParent);
            if (JkLog.isVerbose()) {
                JkLog.trace("Def JkBean classes scanned in " + (System.currentTimeMillis() - t0) + " ms.");
                cachedDefBeanClassNames.forEach(className -> JkLog.trace("  " + className ));
            }
        }
        return cachedDefBeanClassNames;
    }

    private static boolean scan(String pathElement) {
        return !pathElement.startsWith(JAVA_HOME);  // Don't scan jre classes
        //return true;
    }

    private static EngineCommand toEngineCommand(CommandLine.JkBeanAction action,
                                                 Map<String, Class<? extends JkBean>> beanClasses) {
        Class<? extends JkBean> beanClass = (action.beanName == null)
                ? beanClasses.get(null)
                : getJkBeanClass(beanClasses.values(), action.beanName);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private static Class<? extends JkBean> getJkBeanClass(Collection<Class<? extends JkBean>> beanClasses, String name) {
        return beanClasses.stream()
                .filter(Objects::nonNull)
                .filter(beanClass -> JkBean.nameMatches(beanClass.getName(), name))
                .findFirst()
                .orElseThrow(() -> beanClassNotFound(name));
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
