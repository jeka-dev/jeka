package org.jerkar.api.ide.eclipse;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.system.JkLocator;


class Lib {

    private static final String CONTAINERS_PATH = "eclipse/containers";

    static final File CONTAINER_DIR = new File(JkLocator.jerkarHome(), CONTAINERS_PATH);

    static final File CONTAINER_USER_DIR = new File(JkLocator.jerkarUserHome(), CONTAINERS_PATH);

    public static Lib file(File file, JkScope scope, boolean exported) {
        return new Lib(file, null, scope, exported);
    }

    public static Lib project(String project, JkScope scope, boolean exported) {
        return new Lib(null, project, scope, exported);
    }

    public final File file;

    public final String projectRelativePath;

    public final JkScope scope;

    public final boolean exported;

    private Lib(File file, String projectRelativePath, JkScope scope, boolean exported) {
        super();
        this.file = file;
        this.scope = scope;
        this.projectRelativePath = projectRelativePath;
        this.exported = exported;
    }

    @Override
    public String toString() {
        return scope + ":" + file.getPath();
    }

    public static JkDependencies toDependencies(/*JkBuild*/ Object masterBuild, Iterable<Lib> libs,
            ScopeResolver scopeSegregator) {
        final JkDependencies.Builder builder = JkDependencies.builder();
        for (final Lib lib : libs) {
            if (lib.projectRelativePath == null) {
                builder.on(lib.file).scope(lib.scope);

            } else { // This is build import
                /*    final JkJavaBuild importedBuild = (JkJavaBuild) masterBuild
                        .createImportedBuild(lib.projectRelativePath);


                final JkComputedDependency projectDependency = importedBuild.asDependency(importedBuild
                        .packer().jarFile());
                builder.on(projectDependency).scope(lib.scope);

                final File dotClasspathFile = importedBuild.file(".classpath");
                if (dotClasspathFile.exists()) {
                    final DotClasspathModel dotClasspathModel = DotClasspathModel.from(dotClasspathFile);
                    final List<Lib> sublibs = new ArrayList<>();
                    for (final Lib sublib : dotClasspathModel.libs(importedBuild.baseTree().root(),
                            scopeSegregator)) {
                        if (sublib.exported) {
                            sublibs.add(sublib);
                        }
                    }
                    builder.on(Lib.toDependencies(importedBuild, sublibs, scopeSegregator));

                }  */
            }
        }
        return builder.build();
    }

}
