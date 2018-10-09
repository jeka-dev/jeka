package org.jerkar.tool;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsReflect;

/**
 * A run class can import one or several run classes. It is an important mechanism to reuse runs across projects.
 * This class holds imported runs within a run class.
 *
 * @author Jerome Angibaud
 */
public final class JkImportedRuns {

    private static final ThreadLocal<Map<ImportedRunRef, JkRun>> IMPORTED_RUN_CONTEXT = new ThreadLocal<>();

    static JkImportedRuns of(Path masterRootDir, JkRun masterRun) {
        return new JkImportedRuns(masterRootDir, getDirectImportedRuns(masterRun));
    }

    private final List<JkRun> directImportedRuns;

    private List<JkRun> transitiveImportedRuns;

    private final Path masterRunBaseDir;

    private JkImportedRuns(Path masterDir, List<JkRun> runDeps) {
        super();
        this.masterRunBaseDir = masterDir;
        this.directImportedRuns = Collections.unmodifiableList(runDeps);
    }

    /**
     * Returns only the direct slave of this master run.
     */
    public List<JkRun> directs() {
        return Collections.unmodifiableList(directImportedRuns);
    }

    /**
     * Returns direct and transitive importedRuns.
     */
    public List<JkRun> all() {
        if (transitiveImportedRuns == null) {
            transitiveImportedRuns = resolveTransitiveRuns(new HashSet<>());
        }
        return transitiveImportedRuns;
    }

    /**
     * Same as {@link #all()} but only returns run instance of the specified class or its subclasses.
     */
    public <T extends JkRun> List<T> allOf(Class<T> ofClass) {
        final List<T> result = new LinkedList<>();
        for (final JkRun run : all()) {
            if (ofClass.isAssignableFrom(run.getClass())) {
                result.add((T) run);
            }
        }
        return result;
    }

    private List<JkRun> resolveTransitiveRuns(Set<Path> files) {
        final List<JkRun> result = new LinkedList<>();
        for (final JkRun run : directImportedRuns) {
            final Path dir = run.baseDir();
            if (!files.contains(dir)) {
                result.addAll(run.importedRuns().resolveTransitiveRuns(files));
                result.add(run);
                files.add(dir);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<JkRun> getDirectImportedRuns(JkRun masterRun) {
        final List<JkRun> result = new LinkedList<>();
        final List<Field> fields = JkUtilsReflect.getAllDeclaredFields(masterRun.getClass(), JkImportRun.class);

        for (final Field field : fields) {
            final JkImportRun jkProject = field.getAnnotation(JkImportRun.class);
            final JkRun importedRun = createImportedRun(
                    (Class<? extends JkRun>) field.getType(), jkProject.value(), masterRun.baseDir());
            try {
                JkUtilsReflect.setFieldValue(masterRun, field, importedRun);
            } catch (final RuntimeException e) {
                Path currentClassBaseDir = Paths.get(masterRun.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
                while (!Files.exists(currentClassBaseDir.resolve(JkConstants.DEF_DIR)) && currentClassBaseDir != null) {
                    currentClassBaseDir = currentClassBaseDir.getParent();
                }
                if (currentClassBaseDir == null) {
                    throw new IllegalStateException("Can't inject imported run instance of type " + importedRun.getClass().getSimpleName()
                            + " into field " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " from directory " + masterRun.baseDir()
                            + " while working dir is " + Paths.get("").toAbsolutePath());
                }
                throw new IllegalStateException("Can't inject imported run instance of type " + importedRun.getClass().getSimpleName()
                        + " into field " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " from directory " + masterRun.baseDir()
                        + "\nRun class is located in " + currentClassBaseDir
                        + " while working dir is " + Paths.get("").toAbsolutePath()
                        + ".\nPlease set working dir to " + currentClassBaseDir, e);
            }
            result.add(importedRun);
        }
        return result;
    }

    /*
     * Creates an instance of <code>JkRun</code> for the given project and
     * run class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private static <T extends JkRun> T createImportedRun(Class<T> importedRunClass, String relativePath, Path masterRunPath) {
        final Path projectDir = masterRunPath.resolve(relativePath).normalize();
        final ImportedRunRef projectRef = new ImportedRunRef(projectDir, importedRunClass);
        Map<ImportedRunRef, JkRun> map = IMPORTED_RUN_CONTEXT.get();
        if (map == null) {
            map = new HashMap<>();
            IMPORTED_RUN_CONTEXT.set(map);
        }
        final T cachedResult = (T) IMPORTED_RUN_CONTEXT.get().get(projectRef);
        if (cachedResult != null) {
            return cachedResult;
        }
        final Engine engine = new Engine(projectDir);
        final T result = engine.getRun(importedRunClass);
        IMPORTED_RUN_CONTEXT.get().put(projectRef, result);
        return result;
    }

    private static class ImportedRunRef {

        final String canonicalFileName;

        final Class<?> clazz;

        ImportedRunRef(Path projectDir, Class<?> clazz) {
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

            final ImportedRunRef that = (ImportedRunRef) o;

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
