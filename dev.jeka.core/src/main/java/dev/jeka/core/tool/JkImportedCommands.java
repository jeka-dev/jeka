package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A command class can import one or several command classes. It is an important mechanism to reuse runs across projects.
 * This class holds imported runs within a command class.
 *
 * @author Jerome Angibaud
 */
public final class JkImportedCommands {

    private static final ThreadLocal<Map<ImportedCommandsRef, JkCommands>> IMPORTED_RUN_CONTEXT = new ThreadLocal<>();

    static JkImportedCommands of(JkCommands masterCommands) {
        return new JkImportedCommands(getDirectImportedCommands(masterCommands));
    }

    private final List<JkCommands> directImportedRuns;

    private List<JkCommands> transitiveImportedRuns;

    // The declared @JkImportProject values, read at pre-compile time
    private List<Path> importedRunRoots = Collections.emptyList();

    private JkImportedCommands( List<JkCommands> runDeps) {
        super();
        this.directImportedRuns = Collections.unmodifiableList(runDeps);
    }

    /**
     * Returns only the direct slave of this master run.
     */
    public List<JkCommands> getDirects() {
        return Collections.unmodifiableList(directImportedRuns);
    }

    /**
     * Returns direct and transitive importedRuns.
     */
    public List<JkCommands> getAll() {
        if (transitiveImportedRuns == null) {
            transitiveImportedRuns = resolveTransitiveRuns(new HashSet<>());
        }
        return transitiveImportedRuns;
    }

    /**
     * Same as {@link #getAll()} but only returns run instance of the specified class or its subclasses.
     */
    public <T extends JkCommands> List<T> getAllOf(Class<T> ofClass) {
        final List<T> result = new LinkedList<>();
        for (final JkCommands run : getAll()) {
            if (ofClass.isAssignableFrom(run.getClass())) {
                result.add((T) run);
            }
        }
        return result;
    }

    public List<Path> getImportedRunRoots() {
        return importedRunRoots;
    }

    void setImportedRunRoots(List<Path> roots) {
        this.importedRunRoots = Collections.unmodifiableList(roots);
    }

    private List<JkCommands> resolveTransitiveRuns(Set<Path> files) {
        final List<JkCommands> result = new LinkedList<>();
        for (final JkCommands run : directImportedRuns) {
            final Path dir = run.getBaseDir();
            if (!files.contains(dir)) {
                result.addAll(run.getImportedCommands().resolveTransitiveRuns(files));
                result.add(run);
                files.add(dir);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<JkCommands> getDirectImportedCommands(JkCommands masterCommands) {
        final List<JkCommands> result = new LinkedList<>();
        final List<Field> fields = JkUtilsReflect.getAllDeclaredFields(masterCommands.getClass(), JkImportProject.class);
        for (final Field field : fields) {
            final JkImportProject jkProject = field.getAnnotation(JkImportProject.class);
            final JkCommands importedRun = createImportedCommands(
                    (Class<? extends JkCommands>) field.getType(), jkProject.value(), masterCommands.getBaseDir());
            try {
                JkUtilsReflect.setFieldValue(masterCommands, field, importedRun);
            } catch (final RuntimeException e) {
                Path currentClassBaseDir = Paths.get(masterCommands.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
                while (!Files.exists(currentClassBaseDir.resolve(JkConstants.DEF_DIR)) && currentClassBaseDir != null) {
                    currentClassBaseDir = currentClassBaseDir.getParent();
                }
                if (currentClassBaseDir == null) {
                    throw new IllegalStateException("Can't inject imported run instance of type " + importedRun.getClass().getSimpleName()
                            + " into field " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " from directory " + masterCommands.getBaseDir()
                            + " while working dir is " + Paths.get("").toAbsolutePath());
                }
                throw new IllegalStateException("Can't inject imported run instance of type " + importedRun.getClass().getSimpleName()
                        + " into field " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " from directory " + masterCommands.getBaseDir()
                        + "\nCommand class is located in " + currentClassBaseDir
                        + " while working dir is " + Paths.get("").toAbsolutePath()
                        + ".\nPlease set working dir to " + currentClassBaseDir, e);
            }
            result.add(importedRun);
        }
        return result;
    }

    /*
     * Creates an instance of <code>JkCommands</code> for the given project and
     * command class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private static <T extends JkCommands> T createImportedCommands(Class<T> importedCommandsClass, String relativePath, Path masterRunPath) {
        final Path projectDir = masterRunPath.resolve(relativePath).normalize();
        final ImportedCommandsRef commandsRef = new ImportedCommandsRef(projectDir, importedCommandsClass);
        Map<ImportedCommandsRef, JkCommands> map = IMPORTED_RUN_CONTEXT.get();
        if (map == null) {
            map = new HashMap<>();
            IMPORTED_RUN_CONTEXT.set(map);
        }
        final T cachedResult = (T) IMPORTED_RUN_CONTEXT.get().get(commandsRef);
        if (cachedResult != null) {
            return cachedResult;
        }
        final Engine engine = new Engine(projectDir);
        final T result = engine.getCommands(importedCommandsClass, false);
        IMPORTED_RUN_CONTEXT.get().put(commandsRef, result);
        return result;
    }

    private static class ImportedCommandsRef {

        final String canonicalFileName;

        final Class<?> clazz;

        ImportedCommandsRef(Path projectDir, Class<?> clazz) {
            super();
            this.canonicalFileName = projectDir.normalize().toAbsolutePath().toString();
            this.clazz = clazz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ImportedCommandsRef that = (ImportedCommandsRef) o;

            if (!canonicalFileName.equals(that.canonicalFileName)) {
                return false;
            }
            return clazz.equals(that.clazz);
        }

        @Override
        public int hashCode() {
            int result = canonicalFileName.hashCode();
            result = 31 * result + clazz.hashCode();
            return result;
        }
    }

}
