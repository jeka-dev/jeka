package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
 * A resolver to determine witch {@link JkBean} class to use :
 *  - as Master
 *  - according KBean short name.
 *
 * @author Jerome Angibaud
 */
final class EngineKBeanClassResolver {

    private static final String KBEAN_CLASSES_CACHE_FILE_NAME = "kbean-classes.txt";

    private static final String JAVA_HOME = JkUtilsPath.toUrl(Paths.get(System.getProperty("java.home"))).toString();

    private static final JkInternalClasspathScanner CLASSPATH_SCANNER = JkInternalClasspathScanner.of();

    private final Path baseDir;

    final Path jekaSourceDir;

    final Path jekaSrcClassDir;

    private JkPathSequence classpath;

    private List<String> cachedJekaSrcBeanClassNames;

    private List<String> cachedGlobalBeanClassName;

    private final JkProperties jekaProperties;

    private boolean useStoredCache;

    EngineKBeanClassResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.jekaSourceDir = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        this.jekaSrcClassDir = baseDir.resolve(JkConstants.JEKA_SRC_CLASSES_DIR);
        this.jekaProperties = JkRunbase.localProperties(baseDir);
    }

    List<EngineCommand> resolve(CommandLine commandLine, String masterBeanName, boolean ignoreMasterBeanNotFound) {
        JkLog.startTask("Resolve KBean classes");

        // Resolve involved KBean classes
        Map<String, Class<? extends KBean>> beanClasses = new HashMap<>();
        Set<String> involvedKBeanNames = new HashSet<>(commandLine.involvedBeanNames());
        involvedKBeanNames.addAll(getInvolvedKBeanNamesFromProperties());
        for (String beanName : involvedKBeanNames) {
            List<String> beanClassNames = JkUtilsIterable.concatLists(jekaSrcBeanClassNames(), globalBeanClassNames())
                    .stream().distinct().collect(Collectors.toList());
            List<String> matchingClassNames = findClassesMatchingName(beanClassNames, beanName);
            if (matchingClassNames.isEmpty()) {  // maybe the cache is staled -> rescan classpath
                JkLog.trace("KBean '%s' does not match any class names on %s. Rescan classpath", beanName, beanClassNames);
                reloadGlobalBeanClassNames();
                matchingClassNames = findClassesMatchingName(beanClassNames, beanName);
                if (matchingClassNames.isEmpty()) {
                    JkLog.trace("KBean '%s' does not match any class names on %s. Fail.", beanName, beanClassNames);
                }
            }
            Class<? extends KBean> selected = loadUniqueClass(matchingClassNames, beanName, !ignoreMasterBeanNotFound);
            if (selected != null) {
                beanClasses.put(KBean.name(selected), selected);
            }
        }

        // Resolve master KBean
        Class<? extends KBean> masterBeanClass = masterBeanClass(masterBeanName, !ignoreMasterBeanNotFound);
        List<EngineCommand> result = new LinkedList<>();
        List<CommandLine.JkBeanAction> masterBeanActions = commandLine.getDefaultKBeanActions();
        if (masterBeanClass == null && !masterBeanActions.isEmpty() && !ignoreMasterBeanNotFound) {
            String suggest = "help".equals(Environment.originalCmdLineAsString()) ? " ( You mean '-help' ? )" : "";
            throw new JkException("No Master KBean has bean has been selected. "
                    + "One is necessary to define "
                    + masterBeanActions.get(0).shortDescription() + suggest + "."
                    + "\nUse -kb=[beanName] to precise a "
                    + "bean present in classpath or create a class extending JkBean into jeka-src dir.");
        }
        beanClasses.put(null, masterBeanClass);
        if (masterBeanClass != null) {
            result.add(new EngineCommand(EngineCommand.Action.BEAN_INSTANTIATION, masterBeanClass,
                    null, null));
        }

        // Resolve KBean actions
        List<CommandLine.JkBeanAction> beanActions = new LinkedList<>(getBeanActionFromProperties());
        beanActions.addAll(commandLine.getBeanActions());
        beanActions.stream()
                .map(action -> toEngineCommand(action, beanClasses))
                .forEach(result::add);

        JkLog.endTask("KBean classes resolved in %d millis");
        JkLog.info("Master KBean : " + masterBeanClass);
        return Collections.unmodifiableList(result);
    }

    void setClasspath(JkPathSequence classpath, boolean classpathChanged) {
        this.classpath = classpath;
        this.useStoredCache = !classpathChanged;
    }

    private Class<? extends KBean> masterBeanClass(String masterBeanName, boolean failIfNotFound) {
        if (masterBeanName == null) {
            if (jekaSrcBeanClassNames().isEmpty()) {
                return null;
            }
            return jekaSrcBeanClasses().get(0);
        }
        List<String> matchingclassNames = findClassesMatchingName(jekaSrcBeanClassNames(), masterBeanName);
        if (matchingclassNames.isEmpty()) {
            matchingclassNames = findClassesMatchingName(globalBeanClassNames(), masterBeanName);
        }
        return loadUniqueClass(matchingclassNames, masterBeanName, failIfNotFound);
    }

    private Class<? extends KBean> loadUniqueClass(List<String> matchingBeanClasses, String beanName,
                                                   boolean failedIfNotFound) {
        if (matchingBeanClasses.isEmpty()) {
            if (failedIfNotFound) {
                throw beanClassNotFound(beanName);
            }
            return null;
        } else if (matchingBeanClasses.size() > 1) {
            throw new JkException("Several classes matches bean name '" + beanName + "' : "
                    + matchingBeanClasses + ". Please precise the fully qualified class name of the bean " +
                    "instead of its short name.");
        } else {
            Class<? extends KBean> result = JkClassLoader.ofCurrent().loadIfExist(matchingBeanClasses.get(0));
            if (result == null) {  // can happen if cache is stalled
                reloadGlobalBeanClassNames();
                throw new JkException("No class " + matchingBeanClasses.get(0) + " found in classpath. Execute 'Jeka -h' to see available KBeans.");
            }
            return result;
        }
    }

    private JkException beanClassNotFound(String name) {
        String classloaderMention = JkLog.isVerbose() ?
                "\nCurrent classloader :\n" + JkClassLoader.ofCurrent() : "";
        return new JkException("Can not find a KBean named '" + name
                + "'.\nThe name to identify a KBean can be :"
                + "\n  - The fully qualified class name of the KBean (e.g. org.foo.BarKBean)"
                + "\n  - The simple class name (e.g. BarKBean)"
                + "\n  - The uncapitalized simple class name (e.g. barKBean)"
                + "\n  - The simple class name minus the 'KBean' suffix. (e.g. Bar)"
                + "\n  - The uncapitalized simple class name minus the 'KBean' suffix.(e.g. bar)"
                + "\nAvailable KBeans :\n  " + String.join("\n  ", smartKBeanNames())
                + classloaderMention);
    }

    private List<String> smartKBeanNames() {
        return globalBeanClassNames().stream()
                .map(fqcn -> KBean.name(fqcn) + "    (" + fqcn + ")")
                .collect(Collectors.toList());
    }

    List<String> globalBeanClassNames() {
        if (cachedGlobalBeanClassName == null) {
            if (useStoredCache) {
                List<String> storedClassNames = readKbeanClasses();
                if (!storedClassNames.isEmpty()) {
                    cachedGlobalBeanClassName = storedClassNames;
                    return cachedGlobalBeanClassName;
                }
            }
            reloadGlobalBeanClassNames();
        }
        return cachedGlobalBeanClassName;
    }

    private void reloadGlobalBeanClassNames() {
        long t0 = System.currentTimeMillis();
        final ClassLoader classLoader;
        final boolean ignoreParentClassLoader;
        if (classpath != null) {

            // If classpath is set, then sources has been compiled in work dir
            classLoader = new URLClassLoader(classpath.toUrls());
            ignoreParentClassLoader = true; // everything should be on classpath
        } else {
            classLoader = JkClassLoader.ofCurrent().get();
            ignoreParentClassLoader = false; // fail on project without jeka-src if true
        }
        cachedGlobalBeanClassName = CLASSPATH_SCANNER.findClassesInheritingOrAnnotatesWith(
                classLoader,
                KBean.class,
                path -> true,
                path -> true,
                true,
                ignoreParentClassLoader);
        if (JkLog.isVerbose()) {
            JkLog.trace("Scanned classloader :  " + JkClassLoader.of(classLoader).getClasspath());
            JkLog.trace("All KBean classes scanned in " + (System.currentTimeMillis() - t0) + " ms.");
            cachedGlobalBeanClassName.forEach(className -> JkLog.trace("  " + className));
        }
        storeGlobalKBeanClasses(cachedGlobalBeanClassName);
    }

    List<Class<? extends KBean>> jekaSrcBeanClasses() {
        List result = jekaSrcBeanClassNames().stream()
                .sorted()
                .map(className -> JkClassLoader.ofCurrent().load(className))
                .collect(Collectors.toList());
        return result;
    }

    boolean hasDefSource() {
        if (!Files.exists(jekaSourceDir)) {
            return false;
        }
        return JkPathTree.of(jekaSourceDir).andMatching(true,
                "**.java", "*.java", "**.kt", "*.kt").count(0, false) > 0;
    }

    boolean hasClassesInWorkDir() {
        return JkPathTree.of(jekaSrcClassDir).andMatching(true, "**.class")
                .count(0, false) > 0;
    }

    List<String> readKbeanClasses() {
        Path store = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(KBEAN_CLASSES_CACHE_FILE_NAME);
        if (!Files.exists(store)) {
            return Collections.emptyList();
        }
        return JkUtilsPath.readAllLines(store);
    }

    JkPathTree getSourceTree() {
        return JkPathTree.of(jekaSourceDir)
                .andMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

    private List<String> jekaSrcBeanClassNames() {
        if (cachedJekaSrcBeanClassNames == null) {
            long t0 = System.currentTimeMillis();
            ClassLoader classLoader = JkClassLoader.ofCurrent().get();

            // Should return only classes located in  'jeka-src class dir' folder.
            Predicate<Path> shouldInclude = pathElement ->
                    pathElement.equals(this.jekaSrcClassDir.toAbsolutePath().normalize());

            if (classpath != null) {

                // If classpath is set, then sources has been compiled in work dir
                classLoader = new URLClassLoader(
                        JkPathSequence.of().and(this.jekaSrcClassDir).and(JkLocator.getJekaJarPath()).toUrls(),
                        ClassLoader.getSystemClassLoader());
            }
            cachedJekaSrcBeanClassNames = CLASSPATH_SCANNER.findClassesInheritingOrAnnotatesWith(
                    classLoader,
                    KBean.class,
                    EngineKBeanClassResolver::shouldScan,
                    shouldInclude,
                    true,
                    true,
                    JkDoc.class);
            if (JkLog.isVerbose()) {
                JkLog.trace("Classes from jeka-src scanned in " + (System.currentTimeMillis() - t0) + " ms.");
                cachedJekaSrcBeanClassNames.forEach(className -> JkLog.trace("  " + className ));
            }
        }
        return cachedJekaSrcBeanClassNames;
    }

    private static boolean shouldScan(String pathElement) {
        return !pathElement.startsWith(JAVA_HOME);  // Don't scan jre classes
        //return true;
    }

    private EngineCommand toEngineCommand(CommandLine.JkBeanAction action,
                                                 Map<String, Class<? extends KBean>> beanClasses) {
        Class<? extends KBean> beanClass = (action.beanName == null)
                ? beanClasses.get(null)
                : getKBeanClass(beanClasses.values(), action.beanName);
        JkUtilsAssert.state(beanClass != null, "Can't resolve KBean class for action %s", action);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private  Class<? extends KBean> getKBeanClass(Collection<Class<? extends KBean>> beanClasses, String name) {
        return beanClasses.stream()
                .filter(Objects::nonNull)
                .filter(beanClass -> KBean.nameMatches(beanClass.getName(), name))
                .findFirst()
                .orElseThrow(() -> beanClassNotFound(name));
    }

    private static List<String> findClassesMatchingName(List<String> beanClassNameCandidates, String name) {
        return beanClassNameCandidates.stream()
                .filter(className -> KBean.nameMatches(className, name))
                .collect(Collectors.toList());
    }

    private JkPathTree defSources() {
        return JkPathTree.of(this.jekaSourceDir).withMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

    private void storeGlobalKBeanClasses(List<String> classNames) {
        Path store = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(KBEAN_CLASSES_CACHE_FILE_NAME);
        if (!Files.exists(store.getParent())) {
            return;
        }
        String content = String.join(System.lineSeparator(), classNames);
        JkPathFile.of(store).createIfNotExist().write(content.getBytes(StandardCharsets.UTF_8));
    }

    // get involved KBean names from local properties
    private List<String> getInvolvedKBeanNamesFromProperties() {
        Map<String, String> props = jekaProperties.getAllStartingWith("", true);
        return props.keySet().stream()
                .filter(key -> key.contains(CommandLine.KBEAN_SYMBOL))
                .map(key -> JkUtilsString.substringBeforeFirst(key, CommandLine.KBEAN_SYMBOL))
                .distinct()
                .collect(Collectors.toList());
    }

    // get actionKBean from local properties
    private List<CommandLine.JkBeanAction> getBeanActionFromProperties() {
        Map<String, String> props = jekaProperties.getAllStartingWith("", true);
        return props.entrySet().stream()
                .filter(entry -> entry.getKey().contains(CommandLine.KBEAN_SYMBOL))
                .map(entry -> {

                    // 'someBean#=' should be interpreted as 'someBean#'
                    if (entry.getKey().endsWith(CommandLine.KBEAN_SYMBOL)) {
                        return entry.getKey() + CommandLine.KBEAN_SYMBOL;
                    }
                    return entry.getKey() + "=" + entry.getValue();
                })
                .map(CommandLine.JkBeanAction::new)
                .collect(Collectors.toList());
    }

}
