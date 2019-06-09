package dev.jeka.core.tool;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * A resolver for the {@link JkRun} to use for a given project.
 *
 * @author Jerome Angibaud
 */
final class RunResolver {

    private final Path baseDir;

    final Path runSourceDir;

    final Path runClassDir;

    RunResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.runSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.runClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    /**
     * Resolves run classes defined in this project
     */
    List<Class<?>> resolveRunClasses() {
        return resolveRunClasses(JkRun.class);
    }

    /**
     * Resolves the {@link JkRun} instance to use on this project.
     */
    JkRun resolve(String classNameHint) {
        return resolve(classNameHint, JkRun.class);
    }

    /**
     * Resolves the {@link JkRun} instance to use on this project.
     */
    @SuppressWarnings("unchecked")
    <T extends JkRun> T resolve(Class<T> baseClass) {
        return (T) resolve(null, baseClass);
    }

    boolean hasDefSource() {
        if (!Files.exists(runSourceDir)) {
            return false;
        }
        return JkPathTree.of(runSourceDir).andMatching(true,
                "**.java", "*.java").count(0, false) > 0;
    }

    boolean needCompile() {
        if (!this.hasDefSource()) {
            return false;
        }
        final JkPathTree dir = JkPathTree.of(runSourceDir);
        for (final Path path : dir.getRelativeFiles()) {
            final String pathName = path.toString();
            if (pathName.endsWith(".java")) {
                final String simpleName;
                if (pathName.contains(File.pathSeparator)) {
                    simpleName = JkUtilsString.substringAfterLast(pathName, File.separator);
                } else {
                    simpleName = pathName;
                }
                if (simpleName.startsWith("_")) {
                    continue;
                }
                final Class<?> clazz = JkClassLoader.ofCurrent()
                        .loadGivenClassSourcePathIfExist(pathName);
                if (clazz == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private JkRun resolve(String classNameHint, Class<? extends JkRun> baseClass) {

        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();

        // If class name specified in options.
        if (!JkUtilsString.isBlank(classNameHint)) {
            final Class<? extends JkRun> clazz = classLoader.loadFromNameOrSimpleName(
                    classNameHint, JkRun.class);
            if (clazz == null) {
                return null;
            }
            JkRun.baseDirContext(baseDir);
            final JkRun run;
            try {
                run = JkRun.of(clazz);
            } finally {
                JkRun.baseDirContext(null);
            }
            return run;
        }

        // If there is a run file
        if (this.hasDefSource()) {
            final JkPathTree dir = JkPathTree.of(runSourceDir);
            for (final Path path : dir.getRelativeFiles()) {
                if (path.toString().endsWith(".java")) {
                    final Class<?> clazz = classLoader.toJkClassLoader().loadGivenClassSourcePath(path.toString());
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        JkRun.baseDirContext(baseDir);
                        final JkRun run;
                        try {
                            run = JkRun.of((Class<? extends JkRun>) clazz);
                        } finally {
                            JkRun.baseDirContext(null);
                        }
                        return run;
                    }
                }

            }
        }

        // If nothing yet found use defaults
        JkRun.baseDirContext(baseDir);
        final JkRun result;
        try {
            result = JkRun.of(JkConstants.DEFAULT_RUN_CLASS);
        } finally {
            JkRun.baseDirContext(null);
        }
        return result;
    }

    private List<Class<?>> resolveRunClasses(Class<? extends JkRun> baseClass) {

        final JkClassLoader classLoader = JkClassLoader.ofCurrent();
        final List<Class<?>> result = new LinkedList<>();

        // If there is a def sources
        if (this.hasDefSource()) {
            final JkPathTree dir = JkPathTree.of(runSourceDir);
            for (final Path path : dir.getRelativeFiles()) {
                if (path.toString().endsWith(".java")) {
                    final Class<?> clazz = classLoader.loadGivenClassSourcePath(path.toString());
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        result.add(clazz);
                    }
                }
            }
        }
        return result;
    }

}
