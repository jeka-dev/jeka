package org.jerkar.api.ide.eclipse;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLocator;

import java.nio.file.Path;


class Lib {

    private static final String CONTAINERS_PATH = "eclipse/containers";

    static final Path CONTAINER_DIR = JkLocator.jerkarHomeDir().resolve(CONTAINERS_PATH);

    static final Path CONTAINER_USER_DIR = JkLocator.jerkarUserHomeDir().resolve(CONTAINERS_PATH);

    public static Lib file(Path file, JkScope scope, boolean exported) {
        return new Lib(file, null, scope, exported);
    }

    public static Lib project(String project, JkScope scope, boolean exported) {
        return new Lib(null, project, scope, exported);
    }

    public final Path file;

    public final String projectRelativePath;

    public final JkScope scope;

    public final boolean exported;

    private Lib(Path file, String projectRelativePath, JkScope scope, boolean exported) {
        super();
        this.file = file;
        this.scope = scope;
        this.projectRelativePath = projectRelativePath;
        this.exported = exported;
    }

    @Override
    public String toString() {
        return scope + ":" + file == null ? projectRelativePath : file.toString();
    }

    public static JkDependencies toDependencies(Path parentDir, Iterable<Lib> libs,
            ScopeResolver scopeSegregator, JkEclipseClasspathApplier applier) {
        final JkDependencies.Builder builder = JkDependencies.builder();
        for (final Lib lib : libs) {
            if (lib.projectRelativePath == null) {
                builder.on(lib.file.toFile()).scope(lib.scope);

            } else { // This is a dependency on an eclipse project
                Path projectDir = parentDir.resolve(lib.projectRelativePath);
                final JkJavaProject project = new JkJavaProject(projectDir.toFile());
                applier.apply(project);
                builder.on(project).scope(lib.scope);
            }
        }
        return builder.build();
    }

}
