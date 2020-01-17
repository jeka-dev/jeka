package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLocator;

import java.nio.file.Path;


class Lib {

    private static final String CONTAINERS_PATH = "eclipse/containers";

    static final Path CONTAINER_DIR = JkLocator.getJekaHomeDir().resolve(CONTAINERS_PATH);

    static final Path CONTAINER_USER_DIR = JkLocator.getJekaUserHomeDir().resolve(CONTAINERS_PATH);

    public static Lib file(Path file, JkScope scope, boolean exported) {
        return new Lib(file, null, scope, exported);
    }

    public static Lib project(String project, JkScope scope, boolean exported) {
        return new Lib(null, project, scope, exported);
    }

    private final Path file;

    private final String projectRelativePath;

    private final JkScope scope;

    private final boolean exported;

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

    public static JkDependencySet toDependencies(Path parentDir, Iterable<Lib> libs, JkEclipseClasspathApplier applier) {
        JkDependencySet result = JkDependencySet.of();
        for (final Lib lib : libs) {
            if (lib.projectRelativePath == null) {
                result = result.andFile(lib.file, lib.scope);

            } else { // This is a dependency on an eclipse project
                final Path projectDir = parentDir.resolve(lib.projectRelativePath);
                final JkJavaProject project = JkJavaProject.ofMavenLayout(projectDir);
                applier.apply(project);
                result = result.and(project.getMaker(), lib.scope);
            }
        }
        return result;
    }

}
