package dev.jeka.core.tool;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * A resolver for the {@link JkCommands} to use for a given project.
 *
 * @author Jerome Angibaud
 */
final class CommandResolver {

    private final Path baseDir;

    final Path runSourceDir;

    final Path runClassDir;

    CommandResolver(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.runSourceDir = baseDir.resolve(JkConstants.DEF_DIR);
        this.runClassDir = baseDir.resolve(JkConstants.DEF_BIN_DIR);
    }

    /**
     * Resolves the {@link JkCommands} instance to use on this project.
     */
    JkCommands resolve(String classNameHint) {
        return resolve(classNameHint, JkCommands.class);
    }

    /**
     * Resolves the {@link JkCommands} instance to use on this project.
     */
    @SuppressWarnings("unchecked")
    <T extends JkCommands> T resolve(Class<T> baseClass) {
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

    @SuppressWarnings("unchecked")
    private JkCommands resolve(String classNameHint, Class<? extends JkCommands> baseClass) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();

        // If class name specified in options.
        if (!JkUtilsString.isBlank(classNameHint)) {
            final Class<? extends JkCommands> clazz = classLoader.loadFromNameOrSimpleName(
                    classNameHint, JkCommands.class);
            if (clazz == null) {
                return null;
            }
            JkCommands.baseDirContext(baseDir);
            final JkCommands run;
            try {
                run = JkCommands.of(clazz);
            } finally {
                JkCommands.baseDirContext(null);
            }
            return run;
        }

        // If there is a command file
        if (this.hasDefSource()) {
            final JkPathTree dir = JkPathTree.of(runSourceDir);
            for (final Path path : dir.getRelativeFiles()) {
                if (path.toString().endsWith(".java")) {
                    final Class<?> clazz = classLoader.toJkClassLoader().loadGivenClassSourcePath(path.toString());
                    if (baseClass.isAssignableFrom(clazz)
                            && !Modifier.isAbstract(clazz.getModifiers())) {
                        JkCommands.baseDirContext(baseDir);
                        final JkCommands run;
                        try {
                            run = JkCommands.of((Class<? extends JkCommands>) clazz);
                        } finally {
                            JkCommands.baseDirContext(null);
                        }
                        return run;
                    }
                }

            }
        }

        // If nothing yet found use defaults
        JkCommands.baseDirContext(baseDir);
        final JkCommands result;
        try {
            result = JkCommands.of(JkConstants.DEFAULT_COMMAND_CLASS);
        } finally {
            JkCommands.baseDirContext(null);
        }
        return result;
    }

}
