package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A resolver for the {@link JkClass} to use for a given project.
 *
 * @author Jerome Angibaud
 */
final class JkClassResolver {

    private final Path baseDir;

    final Path defSourceDir;

    final Path defClassDir;

    JkClassResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.defSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.defClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    /**
     * Resolves the {@link JkClass} instance to use on this project.
     */
    JkClass resolve(String classNameHint) {
        return resolve(classNameHint, JkClass.class, true);
    }

    JkClass resolveQuietly(String classNameHint) {
        try {
            return resolve(classNameHint);
        } catch (RuntimeException e) {
            JkLog.warn("Error reading class hint " + classNameHint + " : " + e.getMessage());
            if (JkLog.isVerbose()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Resolves the {@link JkClass} instance to use on this project.
     */
    @SuppressWarnings("unchecked")
    <T extends JkClass> T resolve(Class<T> baseClass, boolean initialise) {
        return (T) resolve(null, baseClass, initialise);
    }

    boolean hasDefSource() {
        if (!Files.exists(defSourceDir)) {
            return false;
        }
        return JkPathTree.of(defSourceDir).andMatching(true,
                "**.java", "*.java").count(0, false) > 0;
    }

    boolean needCompile() {
        if (!this.hasDefSource()) {
            JkLog.trace("No def sources found. Skip compile.");
            return false;
        }
        final JkPathTree dir = JkPathTree.of(defSourceDir);
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
                } else {
                    JkLog.trace("Def class " + clazz + " already present in classpath.");
                }
            }
        }
        JkLog.trace("All def classes are already present in classpath. Skip compile.");
        return false;
    }

    @SuppressWarnings("unchecked")
    private JkClass resolve(String classNameHint, Class<? extends JkClass> baseClass, boolean initialize) {

        // If class name specified in options.
        if (!JkUtilsString.isBlank(classNameHint)) {
            final Class<? extends JkClass> clazz = JkInternalClasspathScanner.INSTANCE
                    .loadClassesHavingNameOrSimpleName(classNameHint, JkClass.class);
            if (clazz == null) {
                JkLog.trace("No Jeka class found with name " + classNameHint);
                return null;
            }
            JkClass.baseDirContext(baseDir);
            final JkClass run;
            try {
                run = initialize ? JkClass.of(clazz) : JkClass.ofUninitialized(clazz);
            } finally {
                JkClass.baseDirContext(null);
            }
            return run;
        }

        // If there is a command file
        if (this.hasDefSource()) {
            final JkPathTree dir = JkPathTree.of(defSourceDir);
            for (final Path path : dir.getRelativeFiles()) {
                if (path.toString().endsWith(".java") || path.toString().endsWith(".kt")) {
                    final Class<? extends JkClass> clazz =
                            JkClassLoader.ofCurrent().loadGivenClassSourcePath(path.toString());
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        JkClass.baseDirContext(baseDir);
                        final JkClass run;
                        try {
                            run = initialize ? JkClass.of(clazz) : JkClass.ofUninitialized(clazz);
                        } finally {
                            JkClass.baseDirContext(null);
                        }
                        return run;
                    }
                }
            }
        }

        // If nothing yet found use defaults
        JkClass.baseDirContext(baseDir);
        final JkClass result;
        try {
            result = JkClass.of(JkConstants.DEFAULT_JEKA_CLASS);
        } finally {
            JkClass.baseDirContext(null);
        }
        return result;
    }

}
