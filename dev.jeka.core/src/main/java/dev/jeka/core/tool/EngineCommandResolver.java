package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/*
 * A resolver to determine witch {@link JkBean} class to use as default or according KBean short name.
 *
 * @author Jerome Angibaud
 */
final class EngineCommandResolver {

    private final Path baseDir;

    final Path defSourceDir;

    final Path defClassDir;

    EngineCommandResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.defSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.defClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    List<EngineCommand> resolve(CommandLine commandLine, String defaultBeanNickname) {
        JkLog.startTask("Resolve KBean classes");
        Map<String, Class<? extends JkBean>> beanClasses = new HashMap<>();
        if (commandLine.containsDefaultBean()) {
            Class<? extends JkBean> clazz = resolveDefaultJkBeanClass(defaultBeanNickname);
            if (clazz == null) {
                if (defaultBeanNickname == null) {
                    throw new JkException("Cannot find any KBean in jeka/def dir. Use -KB=[beanName] to precise a " +
                            "bean present in classpath or add a class extending JkBean into jeka/def dir.");
                } else {
                    throw new JkException("Can not resolve KBean for name '" + defaultBeanNickname
                            + "'. The name can be the fully qualified class name of the KBean, its uncapitalized simple class name " +
                            "or its uncapitalized simple class name without the 'JkBean' suffix.");
                }
            }
            beanClasses.put(null, clazz);
        }
        for (String beanName : commandLine.involvedBeanNames()) {
            Class<? extends JkBean> clazz = findBeanClassInClassloader(beanName);
            if (clazz == null) {
                throw new JkException("Cannot find KBean for name '" + beanName + "'. Available KBeans are :"
                        + String.join("\n", this.getDefBeanClasses().stream()
                            .map(Class::getName).collect(Collectors.toList()))
                        + "\nClassloader : " + JkClassLoader.ofCurrent().toString()
                );
            }
            if (beanClasses.containsKey(beanName) && !clazz.equals(beanClasses.get(beanName))) {
                throw new JkException("KBean name '" + beanName
                        + "' is ambiguous to reference KBEAN as it stands both for " + clazz
                        + " and " + beanClasses.get(beanName));
            } else {
                beanClasses.put(JkBean.computeShortName(clazz), clazz);
            }
        }
        JkLog.endTask();
        return commandLine.getBeanActions().stream()
                .map(action -> toEngineCommand(action, beanClasses)).collect(Collectors.toList());
    }

    private static EngineCommand toEngineCommand(CommandLine.JkBeanAction action,
                                                 Map<String, Class<? extends JkBean>> beanClasses) {
        Class<? extends JkBean> beanClass = beanClasses.get(action.beanName);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private Class<? extends JkBean> resolveDefaultJkBeanClass(String nickName) {
        List<Class<? extends JkBean>> candidates = getDefBeanClasses();
        if (nickName == null) {
            if (candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            StringBuilder message = new StringBuilder()
                    .append("Found more than 1 KBean in def sources : " + candidates + ".\n")
                    .append("Specify default bean using -KB=beanName or qualified method/field " +
                            "as 'beanName#doSomething'.");
            throw new JkException(message.toString());
        }
        candidates = candidates.stream()
                .filter(beanClass -> JkBean.nameMatches(beanClass.getName(), nickName))
                .collect(Collectors.toList());
        return findBeanClassInClassloader(nickName);
    }

    private List<Class<? extends JkBean>> getDefBeanClasses() {
        JkClassLoader classLoader = JkClassLoader.ofCurrent();
        List classes = defSources().getRelativeFiles()
                .stream()
                .map(path -> classLoader.loadGivenClassSourcePath(path.toString()))
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .filter(clazz -> JkBean.class.isAssignableFrom(clazz))
                .collect(Collectors.toList());
        return classes;
    }

    private static Class<? extends JkBean> findBeanClassInClassloader(String beanBame) {
        JkClassLoader classLoader = JkClassLoader.ofCurrent();
        Class<? extends JkBean> beanClass = classLoader.loadIfExist(beanBame);
        if (beanClass != null && !JkBean.class.isAssignableFrom(beanClass)) {
            throw new JkException("Class name '" + beanBame + "' does not refers to a JkBean class.");
        }
        if (beanClass != null) {
            return beanClass;
        }
        if (beanBame.contains(".")) {  // nickname was a fully qualified class name and no class found
            return null;
        }
        String capitalizedName = JkUtilsString.capitalize(beanBame);
        String simpleClassName = beanBame.equals(capitalizedName) ?
                beanBame : capitalizedName + JkBean.class.getSimpleName();

        // Note : There is may be more than 1 class in classloader matching this nick name.
        // However, we take the first class found to avoid full classpath scanning
        JkLog.startTask("Finding KBean class for " + beanBame + " (" + simpleClassName + ")");
        beanClass = JkInternalClasspathScanner.INSTANCE.loadFirstFoundClassHavingNameOrSimpleName(beanBame, JkBean.class);
        JkLog.endTask();
        return beanClass;
    }

    private JkPathTree defSources() {
        return JkPathTree.of(this.defSourceDir).withMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

    private boolean hasDefSource() {
        if (!Files.exists(defSourceDir)) {
            return false;
        }
        return JkPathTree.of(defSourceDir).andMatching(true,
                "**.java", "*.java", "**.kt", "*.kt").count(0, false) > 0;
    }

    private List<Class<? extends JkBean>> loadBeanClassesAnnotatedWithJkDoc(Set<String> beanNicknames) {
        JkClassLoader classLoader = JkClassLoader.ofCurrent();
        List<String> candidteClassNames = classLoader.findClassesMatchingAnnotations(
                annotations -> annotations.contains(JkDoc.class.getSimpleName()));
        List result = candidteClassNames.stream()
                .filter(className -> matchAnyBeanNickname(className, beanNicknames))
                .map(classname -> classLoader.load(classname))
                .collect(Collectors.toList());
        return result;
    }

    private static boolean matchAnyBeanNickname(String className, Set<String> nicknames) {
        return nicknames.stream().anyMatch(nickname -> JkBean.nameMatches(className, nickname));
    }

}
