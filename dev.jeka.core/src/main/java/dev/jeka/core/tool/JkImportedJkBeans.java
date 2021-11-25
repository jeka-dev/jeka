package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A KBean can import one or several KBeans from external projects.
 * This class holds imported KBean by a given KBean instance.
 *
 * @author Jerome Angibaud
 */
public final class JkImportedJkBeans {

    private final JkBean holder;

    private List<JkBean> directs;

    private List<JkBean> transitives;

    // The declared @JkInjectProject values, read at pre-compile time
    private List<Path> importedBeanRoots = Collections.emptyList();

    JkImportedJkBeans(JkBean holder) {
        this.holder = holder;
        this.directs = computeDirects(holder);
    }

    JkImportedJkBeans() {
        this.holder = null;
        this.directs = Collections.emptyList();
    }

    /**
     * Returns imported KBeans.
     */
    public List<JkBean> get(boolean includeTransitives) {
        return includeTransitives
                ? Optional.ofNullable(transitives).orElseGet(() -> (transitives = computeTransitives(new HashSet<>())))
                : Optional.ofNullable(directs).orElseGet(() -> (directs = computeDirects(holder)));
    }

    /**
     * Returns KBeans found in imported projects having the specified type.
     */
    public <T extends JkBean> List<T> get(Class<T> jkBeanClass, boolean includeTransitives) {
        return get(includeTransitives).stream()
                .map(JkBean::getRuntime)
                .map(runtime -> runtime.getBeanRegistry().getOptional(jkBeanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    void setImportedBeanRoots(Set<Path> roots) {
        this.importedBeanRoots = new LinkedList<>(roots);
    }

    private List<JkBean> computeTransitives(Set<Path> files) {
        final List<JkBean> result = new LinkedList<>();
        for (final JkBean jkBean : directs) {
            final Path dir = jkBean.getBaseDir();
            if (!files.contains(dir)) {
                result.addAll(jkBean.getImportedJkBeans().computeTransitives(files));
                result.add(jkBean);
                files.add(dir);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<JkBean> computeDirects(JkBean masterBean) {
        final List<JkBean> result = new LinkedList<>();
        final List<Field> fields = JkUtilsReflect.getAllDeclaredFields(masterBean.getClass(), JkInjectProject.class);
        JkLog.trace("Projects imported by " + masterBean + " : " + fields);
        for (final Field field : fields) {
            final JkInjectProject jkProject = field.getAnnotation(JkInjectProject.class);
            final JkBean importedJkClass = createImportedJkBean(
                    (Class<? extends JkBean>) field.getType(), jkProject.value(), masterBean.getBaseDir());
            try {
                JkUtilsReflect.setFieldValue(masterBean, field, importedJkClass);
            } catch (final RuntimeException e) {
                Path currentClassBaseDir = Paths.get(masterBean.getClass().getProtectionDomain()
                        .getCodeSource().getLocation().getPath());
                while (!Files.exists(currentClassBaseDir.resolve(JkConstants.DEF_DIR)) && currentClassBaseDir != null) {
                    currentClassBaseDir = currentClassBaseDir.getParent();
                }
                if (currentClassBaseDir == null) {
                    throw new IllegalStateException("Can't inject imported run instance of type "
                            + importedJkClass.getClass().getSimpleName()
                            + " into field " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " from directory " + masterBean.getBaseDir()
                            + " while working dir is " + Paths.get("").toAbsolutePath());
                }
                throw new IllegalStateException("Can't inject imported run instance of type "
                        + importedJkClass.getClass().getSimpleName()
                        + " into field " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " from directory " + masterBean.getBaseDir()
                        + "\nJeka class is located in " + currentClassBaseDir
                        + " while working dir is " + Paths.get("").toAbsolutePath()
                        + ".\nPlease set working dir to " + currentClassBaseDir, e);
            }
            result.add(importedJkClass);
        }
        return result;
    }

    /*
     * Creates an instance of <code>JkBean</code> for the given project and
     * Jeka class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private static <T extends JkBean> T createImportedJkBean(Class<T> importedBeanClass, String relativePath, Path holderBaseDir) {
        final Path importedProjectDir = holderBaseDir.resolve(relativePath).normalize();
        JkRuntime runtime = JkRuntime.get(importedProjectDir);
        JkRuntime.setBaseDirContext(importedProjectDir);
        final T result = JkRuntime.get(importedProjectDir).getBeanRegistry().get(importedBeanClass);
        JkRuntime.setBaseDirContext(Paths.get(""));
        return result;
    }

}
