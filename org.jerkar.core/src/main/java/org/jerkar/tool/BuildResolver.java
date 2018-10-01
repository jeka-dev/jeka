package org.jerkar.tool;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A resolver for the {@link JkRun} to use for a given project.
 *
 * @author Jerome Angibaud
 */
final class BuildResolver {

    private final Path baseDir;

    final Path buildSourceDir;

    final Path buildClassDir;

    final Path defaultJavaSource;

    BuildResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.buildSourceDir = baseDir.resolve(JkConstants.BUILD_DEF_DIR);
        this.buildClassDir = baseDir.resolve(JkConstants.BUILD_DEF_BIN_DIR);
        this.defaultJavaSource = baseDir.resolve(JkConstants.DEFAULT_JAVA_SOURCE);
    }

    /**
     * Resolves the build classes defined in this project
     */
    List<Class<?>> resolveBuildClasses() {
        return resolveBuildClasses(JkRun.class);
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

    boolean hasBuildSource() {
        if (!Files.exists(buildSourceDir)) {
            return false;
        }
        return JkPathTree.of(buildSourceDir).andAccept("**.java", "*.java").count(0, false) > 0;
    }

    boolean needCompile() {
        if (!this.hasBuildSource()) {
            return false;
        }
        final JkPathTree dir = JkPathTree.of(buildSourceDir);
        for (final Path path : dir.relativeFiles()) {
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
                final Class<?> clazz = JkClassLoader.current()
                        .loadGivenClassSourcePathIfExist(pathName);
                if (clazz == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private JkRun resolve(String classNameHint, Class<? extends JkRun> baseClass) {

        final JkClassLoader classLoader = JkClassLoader.current();

        // If class name specified in options.
        if (!JkUtilsString.isBlank(classNameHint)) {
            final Class<? extends JkRun> clazz = classLoader.loadFromNameOrSimpleName(
                    classNameHint, JkRun.class);
            if (clazz == null) {
                return null;
            }
            JkRun.baseDirContext(baseDir);
            final JkRun build;
            try {
                build = JkRun.of(clazz);
            } finally {
                JkRun.baseDirContext(null);
            }
            return build;
        }

        // If there is a build file
        if (this.hasBuildSource()) {
            final JkPathTree dir = JkPathTree.of(buildSourceDir);
            for (final Path path : dir.relativeFiles()) {
                if (path.toString().endsWith(".java")) {
                    final Class<?> clazz = classLoader.loadGivenClassSourcePath(path.toString());
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        JkRun.baseDirContext(baseDir);
                        final JkRun build;
                        try {
                            build = JkRun.of((Class<? extends JkRun>) clazz);
                        } finally {
                            JkRun.baseDirContext(null);
                        }
                        return build;
                    }
                }

            }
        }

        // If nothing yet found use defaults
        JkRun.baseDirContext(baseDir);
        final JkRun result;
        try {
            result = JkRun.of(JkConstants.DEFAULT_BUILD_CLASS);
        } finally {
            JkRun.baseDirContext(null);
        }
        return result;
    }

    private List<Class<?>> resolveBuildClasses(Class<? extends JkRun> baseClass) {

        final JkClassLoader classLoader = JkClassLoader.current();
        final List<Class<?>> result = new LinkedList<>();

        // If there is a build source
        if (this.hasBuildSource()) {
            final JkPathTree dir = JkPathTree.of(buildSourceDir);
            for (final Path path : dir.relativeFiles()) {
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
