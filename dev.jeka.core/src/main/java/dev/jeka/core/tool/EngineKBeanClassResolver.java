package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
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
 * A resolver to determine witch {@link JkBean} class to use as default or according KBean short name.
 *
 * @author Jerome Angibaud
 */
final class EngineKBeanClassResolver {

    private static final String JAVA_HOME = JkUtilsPath.toUrl(Paths.get(System.getProperty("java.home"))).toString();

    private static final JkInternalClasspathScanner CLASSPATH_SCANNER = JkInternalClasspathScanner.of();

    private final Path baseDir;

    final Path defSourceDir;

    final Path defClassDir;

    private JkPathSequence classpath;

    private List<String> cachedDefBeanClassNames;

    private List<String> cachedGlobalBeanClassName;

    private final JkProperties localProperties;

    private boolean useStoredCache;


    EngineKBeanClassResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.defSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.defClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
        this.localProperties = JkRunbase.localProperties(baseDir);
    }

    List<EngineCommand> resolve(CommandLine commandLine, String defaultBeanName, boolean ignoreDefaultBeanNotFound) {
        JkLog.startTask("Resolve KBean classes");

        // Resolve involved KBean classes
        Map<String, Class<? extends KBean>> beanClasses = new HashMap<>();
        Set<String> involvedKBeanNames = new HashSet<>(commandLine.involvedBeanNames());
        involvedKBeanNames.addAll(getInvolvedKBeanNamesFromProperties());
        for (String beanName : involvedKBeanNames) {
            List<String> beanClassNames = JkUtilsIterable.concatLists(defBeanClassNames(), globalBeanClassNames())
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
            Class<? extends KBean> selected = loadUniqueClass(matchingClassNames, beanName,
                    !ignoreDefaultBeanNotFound);
            beanClasses.put(KBean.name(selected), selected);
        }

        // Resolve default KBean
        Class<? extends KBean> defaultBeanClass = defaultBeanClass(defaultBeanName, !ignoreDefaultBeanNotFound);
        List<EngineCommand> result = new LinkedList<>();
        List<CommandLine.JkBeanAction> defaultBeanActions = commandLine.getDefaultBeanActions();
        if (defaultBeanClass == null && !defaultBeanActions.isEmpty() && !ignoreDefaultBeanNotFound) {
            String suggest = "help".equals(Environment.originalCmdLineAsString()) ? " ( You mean '-help' ? )" : "";
            throw new JkException("No default KBean has bean has been selected. "
                    + "One is necessary to define "
                    + defaultBeanActions.get(0).shortDescription() + suggest + "."
                    + "\nUse -kb=[beanName] to precise a "
                    + "bean present in classpath or create a class extending JkBean into jeka/def dir.");
        }
        beanClasses.put(null, defaultBeanClass);
        if (defaultBeanClass != null) {
            result.add(new EngineCommand(EngineCommand.Action.BEAN_INSTANTIATION, defaultBeanClass,
                    null, null));
        }

        // Resolve KBean actions
        List<CommandLine.JkBeanAction> beanActions = new LinkedList<>(getBeanActionFromProperties());
        beanActions.addAll(commandLine.getBeanActions());
        beanActions.stream()
                .map(action -> toEngineCommand(action, beanClasses))
                .forEach(result::add);

        JkLog.endTask();
        JkLog.info("Default KBean : " + defaultBeanClass);
        return Collections.unmodifiableList(result);
    }

    void setClasspath(JkPathSequence classpath, boolean classpathChanged) {
        this.classpath = classpath;
        this.useStoredCache = !classpathChanged;
    }

    private Class<? extends KBean> defaultBeanClass(String defaultBeanName, boolean failIfNotFound) {
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
        return loadUniqueClass(matchingclassNames, defaultBeanName, failIfNotFound);
    }

    private Class<? extends KBean> loadUniqueClass(List<String> matchingBeanClasses, String beanName,
                                                   boolean failedIfNotFound) {
        if (matchingBeanClasses.isEmpty()) {
            if (failedIfNotFound) {
                throw beanClassNotFound(beanName);
            }
            return null;
        } else if (matchingBeanClasses.size() > 1) {
            throw new JkException("Several classes matches default bean name '" + beanName + "' : "
                    + matchingBeanClasses + ". Please precise the fully qualified class name of the default bean " +
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
        return new JkException("Can not find a KBean named '" + name
                + "'.\nThe name to identify a KBean can be :"
                + "\n  - The fully qualified class name of the KBean (e.g. org.foo.BarKBean)"
                + "\n  - The simple class name (e.g. BarKBean)"
                + "\n  - The uncapitalized simple class name (e.g. barKBean)"
                + "\n  - The simple class name minus the 'KBean' suffix. (e.g. Bar)"
                + "\n  - The uncapitalized simple class name minus the 'KBean' suffix.(e.g. bar)"
                + "\nAvailable KBeans :\n  " + String.join("\n  ", globalBeanClassNames())
                + "\nCurrent classloader :\n"
                + JkClassLoader.ofCurrent());
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
            ignoreParentClassLoader = false; // fail on project without jeka/def if true
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

    List<Class<? extends KBean>> defBeanClasses() {
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

    boolean hasClassesInWorkDir() {
        return JkPathTree.of(defClassDir).andMatching(true, "**.class")
                .count(0, false) > 0;
    }

    List<String> readKbeanClasses() {
        Path store = baseDir.resolve(JkConstants.WORK_PATH).resolve(JkConstants.KBEAN_CLASSES_CACHE_FILE_NAME);
        if (!Files.exists(store)) {
            return Collections.emptyList();
        }
        return JkUtilsPath.readAllLines(store);
    }

    JkPathTree getSourceTree() {
        return JkPathTree.of(defSourceDir)
                .andMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

    private List<String> defBeanClassNames() {
        if (cachedDefBeanClassNames == null) {
            long t0 = System.currentTimeMillis();
            ClassLoader classLoader = JkClassLoader.ofCurrent().get();

            // Should return only classes located in  'def class dir' folder.
            Predicate<Path> shouldInclude = pathElement ->
                    pathElement.equals(this.defClassDir.toAbsolutePath().normalize());

            if (classpath != null) {

                // If classpath is set, then sources has been compiled in work dir
                classLoader = new URLClassLoader(
                        JkPathSequence.of().and(this.defClassDir).and(JkLocator.getJekaJarPath()).toUrls(),
                        ClassLoader.getSystemClassLoader());
            }
            cachedDefBeanClassNames = CLASSPATH_SCANNER.findClassesInheritingOrAnnotatesWith(
                    classLoader,
                    KBean.class,
                    EngineKBeanClassResolver::shouldScan,
                    shouldInclude,
                    true,
                    true,
                    JkDoc.class);
            if (JkLog.isVerbose()) {
                JkLog.trace("Def KBean classes scanned in " + (System.currentTimeMillis() - t0) + " ms.");
                cachedDefBeanClassNames.forEach(className -> JkLog.trace("  " + className ));
            }
        }
        return cachedDefBeanClassNames;
    }

    private static boolean shouldScan(String pathElement) {
        return !pathElement.startsWith(JAVA_HOME);  // Don't scan jre classes
        //return true;
    }

    private EngineCommand toEngineCommand(CommandLine.JkBeanAction action,
                                                 Map<String, Class<? extends KBean>> beanClasses) {
        Class<? extends KBean> beanClass = (action.beanName == null)
                ? beanClasses.get(null)
                : getJkBeanClass(beanClasses.values(), action.beanName);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private  Class<? extends KBean> getJkBeanClass(Collection<Class<? extends KBean>> beanClasses, String name) {
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
        return JkPathTree.of(this.defSourceDir).withMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

    private void storeGlobalKBeanClasses(List<String> classNames) {
        Path store = baseDir.resolve(JkConstants.WORK_PATH).resolve(JkConstants.KBEAN_CLASSES_CACHE_FILE_NAME);
        if (!Files.exists(store.getParent().getParent())) {
            return;
        }
        String content = String.join(System.lineSeparator(), classNames);
        JkPathFile.of(store).createIfNotExist().write(content.getBytes(StandardCharsets.UTF_8));
    }

    // get involved KBean names from local properties
    private List<String> getInvolvedKBeanNamesFromProperties() {
        Map<String, String> props = localProperties.getAllStartingWith("", true);
        return props.keySet().stream()
                .filter(key -> key.contains(CommandLine.KBEAN_SYMBOL))
                .map(key -> JkUtilsString.substringBeforeFirst(key, CommandLine.KBEAN_SYMBOL))
                .distinct()
                .collect(Collectors.toList());
    }

    // get actionKBean from local properties
    private List<CommandLine.JkBeanAction> getBeanActionFromProperties() {
        Map<String, String> props = localProperties.getAllStartingWith("", true);
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