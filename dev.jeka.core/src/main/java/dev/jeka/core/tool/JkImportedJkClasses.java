package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A Jeka class can import one or several Jeka classes. It is an important mechanism to reuse Jeka across projects.
 * This class holds imported Jeka classes within a Jeka class instance.
 *
 * @author Jerome Angibaud
 */
public final class JkImportedJkClasses {

    private static final ThreadLocal<Map<ImportedJkClassRef, JkClass>> IMPORTED_RUN_CONTEXT = new ThreadLocal<>();

    static JkImportedJkClasses of(JkClass masterCommandSet) {
        return new JkImportedJkClasses(getDirectImportedJkClasses(masterCommandSet));
    }

    private final List<JkClass> directImportedJkClasses;

    private List<JkClass> transitiveImportedJkClasses;

    // The declared @JkDefImport values, read at pre-compile time
    private List<Path> importedRunRoots = Collections.emptyList();

    private JkImportedJkClasses(List<JkClass> runDeps) {
        super();
        this.directImportedJkClasses = Collections.unmodifiableList(runDeps);
    }

    /**
     * Returns only the direct slave of this master run.
     */
    public List<JkClass> getDirects() {
        return Collections.unmodifiableList(directImportedJkClasses);
    }

    /**
     * Returns direct and transitive importedRuns.
     */
    public List<JkClass> getAll() {
        if (transitiveImportedJkClasses == null) {
            transitiveImportedJkClasses = resolveTransitiveRuns(new HashSet<>());
        }
        return transitiveImportedJkClasses;
    }

    /**
     * Same as {@link #getAll()} but only returns run instance of the specified class or its subclasses.
     */
    public <T extends JkClass> List<T> getAllOf(Class<T> ofClass) {
        final List<T> result = new LinkedList<>();
        for (final JkClass run : getAll()) {
            if (ofClass.isAssignableFrom(run.getClass())) {
                result.add((T) run);
            }
        }
        return result;
    }

    public List<Path> getImportedJkClassRoots() {
        return importedRunRoots;
    }

    void setImportedRunRoots(List<Path> roots) {
        this.importedRunRoots = Collections.unmodifiableList(roots);
    }

    private List<JkClass> resolveTransitiveRuns(Set<Path> files) {
        final List<JkClass> result = new LinkedList<>();
        for (final JkClass run : directImportedJkClasses) {
            final Path dir = run.getBaseDir();
            if (!files.contains(dir)) {
                result.addAll(run.getImportedJkClasses().resolveTransitiveRuns(files));
                result.add(run);
                files.add(dir);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<JkClass> getDirectImportedJkClasses(JkClass masterJkClass) {
        final List<JkClass> result = new LinkedList<>();
        final List<Field> fields = JkUtilsReflect.getAllDeclaredFields(masterJkClass.getClass(), JkDefImport.class);
        for (final Field field : fields) {
            final JkDefImport jkProject = field.getAnnotation(JkDefImport.class);
            final JkClass importedJkClass = createImportedJkClass(
                    (Class<? extends JkClass>) field.getType(), jkProject.value(), masterJkClass.getBaseDir());
            try {
                JkUtilsReflect.setFieldValue(masterJkClass, field, importedJkClass);
            } catch (final RuntimeException e) {
                Path currentClassBaseDir = Paths.get(masterJkClass.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
                while (!Files.exists(currentClassBaseDir.resolve(JkConstants.DEF_DIR)) && currentClassBaseDir != null) {
                    currentClassBaseDir = currentClassBaseDir.getParent();
                }
                if (currentClassBaseDir == null) {
                    throw new IllegalStateException("Can't inject imported run instance of type " + importedJkClass.getClass().getSimpleName()
                            + " into field " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " from directory " + masterJkClass.getBaseDir()
                            + " while working dir is " + Paths.get("").toAbsolutePath());
                }
                throw new IllegalStateException("Can't inject imported run instance of type " + importedJkClass.getClass().getSimpleName()
                        + " into field " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " from directory " + masterJkClass.getBaseDir()
                        + "\nJeka class is located in " + currentClassBaseDir
                        + " while working dir is " + Paths.get("").toAbsolutePath()
                        + ".\nPlease set working dir to " + currentClassBaseDir, e);
            }
            result.add(importedJkClass);
        }
        return result;
    }

    /*
     * Creates an instance of <code>JkClass</code> for the given project and
     * Jeka class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private static <T extends JkClass> T createImportedJkClass(Class<T> importedJekaClass, String relativePath, Path masterRunPath) {
        final Path projectDir = masterRunPath.resolve(relativePath).normalize();
        final ImportedJkClassRef jkClassRef = new ImportedJkClassRef(projectDir, importedJekaClass);
        Map<ImportedJkClassRef, JkClass> map = IMPORTED_RUN_CONTEXT.get();
        if (map == null) {
            map = new HashMap<>();
            IMPORTED_RUN_CONTEXT.set(map);
        }
        final T cachedResult = (T) IMPORTED_RUN_CONTEXT.get().get(jkClassRef);
        if (cachedResult != null) {
            return cachedResult;
        }
        final Engine engine = new Engine(projectDir);
        final T result = engine.getJkClass(importedJekaClass, false);
        IMPORTED_RUN_CONTEXT.get().put(jkClassRef, result);
        return result;
    }

    private static class ImportedJkClassRef {

        final String canonicalFileName;

        final Class<?> clazz;

        ImportedJkClassRef(Path projectDir, Class<?> clazz) {
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

            final ImportedJkClassRef that = (ImportedJkClassRef) o;

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
