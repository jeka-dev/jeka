package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * A resolver to determine witch {@link JkBean} class to use as default or according KBean short name.
 *
 * @author Jerome Angibaud
 */
final class EngineBeanClassResolver {

    private final Path baseDir;

    final Path defSourceDir;

    final Path defClassDir;

    EngineBeanClassResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.defSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.defClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    Class<? extends JkBean> resolveDefaultJkBeanClass(Optional<String> nickName) {
        List<Class<? extends JkBean>> candidates = getDefBeanClasses();
        if (nickName.isPresent()) {
            if (candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            StringBuilder message = new StringBuilder()
                    .append("Found more than 1 KBean in def sources : " + candidates + ".\n")
                    .append("Specify default bean using -JKB=beanName or qualified method/field " +
                            "as 'beanName#doSomething'.");
            throw new JkException(message.toString());
        }
        candidates = candidates.stream()
                .filter(beanClass -> JkBean.nickNameMatches(beanClass, nickName.get()))
                .collect(Collectors.toList());
        return findBeanClassInClassloader(nickName.get());
    }

    private List<Class<? extends JkBean>> getDefBeanClasses() {
        JkClassLoader classLoader = JkClassLoader.ofCurrent();
        List classes = defSources().getRelativeFiles()
                .stream()
                .map(path -> classLoader.loadGivenClassSourcePath(path.toString()))
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .filter(clazz -> JkBean.class.isAssignableFrom(clazz))
                .map(clazz -> clazz.cast(JkBean.class))
                .collect(Collectors.toList());
        return classes;
    }

    private static Class<? extends JkBean> findBeanClassInClassloader(String nickName) {
        JkClassLoader classLoader = JkClassLoader.ofCurrent();
        Class<? extends JkBean> beanClass = classLoader.loadIfExist(nickName);
        if (beanClass != null && !JkBean.class.isAssignableFrom(beanClass)) {
            throw new JkException("Class name '" + nickName + "' does not refers to a JkBean class.");
        }
        if (beanClass != null) {
            return beanClass;
        }
        if (nickName.contains(".")) {  // nickname was a fully qualified class name and no class found
            return null;
        }
        String capitalizedNickName = JkUtilsString.capitalize(nickName);
        String simpleClassName = nickName.equals(capitalizedNickName) ?
                nickName : capitalizedNickName + JkBean.class.getSimpleName();

        // Note : There is may be more than 1 class in classloader matching this nick name.
        // However, we take the first class found to avoid full classpath scanning
        JkLog.startTask("Finding KBean class having nick name " + nickName);
        beanClass = JkInternalClasspathScanner.INSTANCE.loadFirstFoundClassHavingNameOrSimpleName(nickName, JkBean.class);
        JkLog.endTask();
        return beanClass;
    }

    private JkPathTree defSources() {
        return JkPathTree.of(this.defSourceDir).withMatcher(Engine.JAVA_OR_KOTLIN_SOURCE_MATCHER);
    }

    boolean hasDefSource() {
        if (!Files.exists(defSourceDir)) {
            return false;
        }
        return JkPathTree.of(defSourceDir).andMatching(true,
                "**.java", "*.java", "**.kt", "*.kt").count(0, false) > 0;
    }

}
