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
 * A resolver for the {@link JkCommandSet} to use for a given project.
 *
 * @author Jerome Angibaud
 */
final class CommandResolver {

    private final Path baseDir;

    final Path defSourceDir;

    final Path defClassDir;

    CommandResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.defSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.defClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    /**
     * Resolves the {@link JkCommandSet} instance to use on this project.
     */
    JkCommandSet resolve(String classNameHint) {
        return resolve(classNameHint, JkCommandSet.class, true);
    }

    /**
     * Resolves the {@link JkCommandSet} instance to use on this project.
     */
    @SuppressWarnings("unchecked")
    <T extends JkCommandSet> T resolve(Class<T> baseClass, boolean initialised) {
        return (T) resolve(null, baseClass, initialised);
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
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private JkCommandSet resolve(String classNameHint, Class<? extends JkCommandSet> baseClass, boolean initialised) {

        // If class name specified in options.
        if (!JkUtilsString.isBlank(classNameHint)) {
            final Class<? extends JkCommandSet> clazz = JkInternalClasspathScanner.INSTANCE
                    .loadClassesHavingNameOrSimpleName(classNameHint, JkCommandSet.class);
            if (clazz == null) {
                JkLog.warn("No commands class found with name " + classNameHint);
                return null;
            }
            JkCommandSet.baseDirContext(baseDir);
            final JkCommandSet run;
            try {
                run = initialised ? JkCommandSet.of(clazz) : JkCommandSet.ofUninitialised(clazz);
            } finally {
                JkCommandSet.baseDirContext(null);
            }
            return run;
        }

        // If there is a command file
        if (this.hasDefSource()) {
            final JkPathTree dir = JkPathTree.of(defSourceDir);
            for (final Path path : dir.getRelativeFiles()) {
                if (path.toString().endsWith(".java") || path.toString().endsWith(".kt")) {
                    final Class<?> clazz = JkClassLoader.ofCurrent().loadGivenClassSourcePath(path.toString());
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        JkCommandSet.baseDirContext(baseDir);
                        final JkCommandSet run;
                        try {
                            run = JkCommandSet.of((Class<? extends JkCommandSet>) clazz);
                        } finally {
                            JkCommandSet.baseDirContext(null);
                        }
                        return run;
                    }
                }

            }
        }

        // If nothing yet found use defaults
        JkCommandSet.baseDirContext(baseDir);
        final JkCommandSet result;
        try {
            result = JkCommandSet.of(JkConstants.DEFAULT_COMMAND_CLASS);
        } finally {
            JkCommandSet.baseDirContext(null);
        }
        return result;
    }

}
