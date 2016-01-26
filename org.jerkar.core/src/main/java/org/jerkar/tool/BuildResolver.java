package org.jerkar.tool;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A resolver for the {@link JkBuild} to use for a given project.
 * 
 * @author Jerome Angibaud
 */
final class BuildResolver {

    private final File baseDir;

    final File buildSourceDir;

    final File buildClassDir;

    final File defaultJavaSource;

    BuildResolver(File baseDir) {
        super();
        this.baseDir = baseDir;
        this.buildSourceDir = new File(baseDir, JkConstants.BUILD_DEF_DIR);
        this.buildClassDir = new File(baseDir, JkConstants.BUILD_DEF_BIN_DIR);
        this.defaultJavaSource = new File(baseDir, JkConstants.DEFAULT_JAVA_SOURCE);
    }

    /**
     * Resolves the build classes defined in this project
     */
    List<Class<?>> resolveBuildClasses() {
        return resolveBuildClasses(JkBuild.class);
    }

    /**
     * Resolves the {@link JkBuild} instance to use on this project.
     */
    JkBuild resolve(String classNameHint) {
        return resolve(classNameHint, JkBuild.class);
    }

    /**
     * Resolves the {@link JkBuild} instance to use on this project.
     */
    @SuppressWarnings("unchecked")
    <T extends JkBuild> T resolve(Class<T> baseClass) {
        return (T) resolve(null, baseClass);
    }

    boolean hasBuildSource() {
        if (!buildSourceDir.exists()) {
            return false;
        }
        return JkFileTree.of(buildSourceDir).include("**/*.java").fileCount(false) > 0;
    }

    boolean needCompile() {
        if (!this.hasBuildSource()) {
            return false;
        }
        final JkFileTree dir = JkFileTree.of(buildSourceDir);
        for (final String path : dir.relativePathes()) {
            if (path.endsWith(".java")) {
                final String simpleName;
                if (path.contains(File.pathSeparator)) {
                    simpleName = JkUtilsString.substringAfterLast(path, File.separator);
                } else {
                    simpleName = path;
                }
                if (simpleName.startsWith("_")) {
                    continue;
                }
                final Class<?> clazz = JkClassLoader.current()
                        .loadGivenClassSourcePathIfExist(path);
                if (clazz == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private JkBuild resolve(String classNameHint, Class<? extends JkBuild> baseClass) {

        final JkClassLoader classLoader = JkClassLoader.current();

        // If class name specified in options.
        if (!JkUtilsString.isBlank(classNameHint)) {
            final Class<? extends JkBuild> clazz = classLoader.loadFromNameOrSimpleName(
                    classNameHint, JkBuild.class);
            if (clazz == null) {
                throw new JkException("No build class named " + classNameHint + " found.");
            }
            JkBuild.baseDirContext(baseDir);
            final JkBuild build;
            try {
                build = JkUtilsReflect.newInstance(clazz);
            } finally {
                JkBuild.baseDirContext(null);
            }
            return build;
        }

        // If there is a build file
        if (this.hasBuildSource()) {
            final JkFileTree dir = JkFileTree.of(buildSourceDir);
            for (final String path : dir.relativePathes()) {
                if (path.endsWith(".java")) {
                    final Class<?> clazz = classLoader.loadGivenClassSourcePath(path);
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        JkBuild.baseDirContext(baseDir);
                        final JkBuild build;
                        try {
                            build = (JkBuild) JkUtilsReflect.newInstance(clazz);
                        } finally {
                            JkBuild.baseDirContext(null);
                        }
                        return build;
                    }
                }

            }
        }

        // If nothing yet found use defaults
        JkBuild.baseDirContext(baseDir);
        final JkBuild result;
        try {
            result = (JkBuild) JkUtilsReflect
                    .newInstance(JkConstants.DEFAULT_BUILD_CLASS);
        } finally {
            JkBuild.baseDirContext(null);
        }
        return result;
    }

    private List<Class<?>> resolveBuildClasses(Class<? extends JkBuild> baseClass) {

        final JkClassLoader classLoader = JkClassLoader.current();
        final List<Class<?>> result = new LinkedList<Class<?>>();

        // If there is a build source
        if (this.hasBuildSource()) {
            final JkFileTree dir = JkFileTree.of(buildSourceDir);
            for (final String path : dir.relativePathes()) {
                if (path.endsWith(".java")) {
                    final Class<?> clazz = classLoader.loadGivenClassSourcePath(path);
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
