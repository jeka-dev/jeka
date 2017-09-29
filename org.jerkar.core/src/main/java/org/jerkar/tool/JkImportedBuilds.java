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

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * Defines importedBuilds of a given master build.
 *
 * @author Jerome Angibaud
 */
public final class JkImportedBuilds {

    private static final ThreadLocal<Map<ImportedBuildRef, JkBuild>> IMPORTED_BUILD_CONTEXT = new ThreadLocal<>();

    static JkImportedBuilds of(Path masterRootDir, JkBuild build) {
        return new JkImportedBuilds(masterRootDir, getDirectImportedBuilds(build));
    }

    private final List<JkBuild> directImports;

    private List<JkBuild> transitiveImports;

    private final Path masterBuildRoot;

    private JkImportedBuilds(Path masterDir, List<JkBuild> buildDeps) {
        super();
        this.masterBuildRoot = masterDir;
        this.directImports = Collections.unmodifiableList(buildDeps);
    }

    /**
     * Returns a {@link JkImportedBuilds} identical to this one but augmented with
     * specified slave builds.
     */
    @SuppressWarnings("unchecked")
    public JkImportedBuilds and(List<JkBuild> slaves) {
        return new JkImportedBuilds(this.masterBuildRoot, JkUtilsIterable.concatLists(
                this.directImports, slaves));
    }

    /**
     * Returns only the direct slave of this master build.
     */
    public List<JkBuild> directs() {
        return Collections.unmodifiableList(directImports);
    }

    /**
     * Returns direct and transitive importedBuilds. Transitive importedBuilds are resolved by
     * invoking recursively <code>JkBuildDependencySupport#importedBuilds()</code> on
     * direct importedBuilds.
     *
     */
    public List<JkBuild> all() {
        if (transitiveImports == null) {
            transitiveImports = resolveTransitiveBuilds(new HashSet<>());
        }
        return transitiveImports;
    }

    /**
     * Same as {@link #all()} but only returns builds instance of the specified class or its subclasses.
     */
    public <T extends JkBuild> List<T> allOf(Class<T> ofClass) {
        final List<T> result = new LinkedList<>();
        for (final JkBuild build : all()) {
            if (ofClass.isAssignableFrom(build.getClass())) {
                result.add((T) build);
            }
        }
        return result;
    }

    private List<JkBuild> resolveTransitiveBuilds(Set<Path> files) {
        final List<JkBuild> result = new LinkedList<>();
        for (final JkBuild build : directImports) {
            final Path dir = build.baseDir();
            if (!files.contains(dir)) {
                result.addAll(build.importedBuilds().resolveTransitiveBuilds(files));
                result.add(build);
                files.add(dir);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<JkBuild> getDirectImportedBuilds(JkBuild build) {
        final List<JkBuild> result = new LinkedList<>();
        final List<Field> fields = JkUtilsReflect.getAllDeclaredField(build.getClass(), JkImportBuild.class);

        for (final Field field : fields) {
            final JkImportBuild jkProject = field.getAnnotation(JkImportBuild.class);
            final JkBuild subBuild = createImportedBuild(
                    (Class<? extends JkBuild>) field.getType(), jkProject.value(), build);
            try {
                JkUtilsReflect.setFieldValue(build, field, subBuild);
            } catch (final RuntimeException e) {
                Path currentClassBaseDir = Paths.get(build.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
                while (!Files.exists(currentClassBaseDir.resolve("build/def")) && currentClassBaseDir != null) {
                    currentClassBaseDir = currentClassBaseDir.getParent();
                }
                if (currentClassBaseDir == null) {
                    throw new IllegalStateException("Can't inject slave build instance of type " + subBuild.getClass().getSimpleName()
                            + " into field " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " from directory " + build.baseTree().rootDir()
                            + " while working dir is " + JkUtilsFile.workingDir());
                }
                throw new IllegalStateException("Can't inject slave build instance of type " + subBuild.getClass().getSimpleName()
                        + " into field " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " from directory " + build.baseDir()
                        + "\nBuild class is located in " + currentClassBaseDir
                        + " while working dir is " + JkUtilsFile.workingDir()
                        + ".\nPlease set working dir to " + currentClassBaseDir, e);
            }
            result.add(subBuild);
        }
        return result;
    }

    /**
     * Creates an instance of <code>JkBuild</code> for the given project and
     * build class. The instance field annotated with <code>JkOption</code> are
     * populated as usual.
     */
    @SuppressWarnings("unchecked")
    private static <T extends JkBuild> T createImportedBuild(Class<T> clazz, String relativePath, JkBuild build) {
        final Path projectDir = build.baseDir().resolve(relativePath);
        final ImportedBuildRef projectRef = new ImportedBuildRef(projectDir, clazz);
        Map<ImportedBuildRef, JkBuild> map = IMPORTED_BUILD_CONTEXT.get();
        if (map == null) {
            map = new HashMap<>();
            IMPORTED_BUILD_CONTEXT.set(map);
        }
        final T cachedResult = (T) IMPORTED_BUILD_CONTEXT.get().get(projectRef);
        if (cachedResult != null) {
            return cachedResult;
        }
        final Engine engine = new Engine(projectDir);
        final T result = engine.getBuild(clazz);
        JkOptions.populateFields(result);
        IMPORTED_BUILD_CONTEXT.get().put(projectRef, result);
        return result;
    }

    private static class ImportedBuildRef {

        final String canonicalFileName;

        final Class<?> clazz;

        ImportedBuildRef(Path projectDir, Class<?> clazz) {
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

            final ImportedBuildRef that = (ImportedBuildRef) o;

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
