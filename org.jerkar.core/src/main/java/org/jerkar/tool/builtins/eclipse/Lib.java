package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkLocator;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

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

    public static JkDependencies toDependencies(JkBuild masterBuild, Iterable<Lib> libs,
                                                ScopeResolver scopeSegregator) {
        final JkDependencies.Builder builder = JkDependencies.builder();
        for (final Lib lib : libs) {
            if (lib.projectRelativePath == null) {
                builder.on(lib.file).scope(lib.scope);

            } else { // This is project dependency
                final JkJavaBuild slaveBuild = (JkJavaBuild) masterBuild
                        .createImportedBuild(lib.projectRelativePath);

                // if the slave build does not have build class, we apply eclipse# plugin
                JkFileTree def = slaveBuild.baseTree().go("build/def");
                if (!def.exists() || def.include("**/*.java").fileCount(false) == 0) {
                    JkBuildPluginEclipse eclipsePlugin = new JkBuildPluginEclipse();
                    slaveBuild.plugins.activate(eclipsePlugin);
                }

                final JkComputedDependency projectDependency = slaveBuild.asDependency(slaveBuild
                        .packer().jarFile());
                builder.on(projectDependency).scope(lib.scope);

                final File dotClasspathFile = slaveBuild.file(".classpath");
                if (dotClasspathFile.exists()) {
                    final DotClasspathModel dotClasspathModel = DotClasspathModel.from(dotClasspathFile);
                    final List<Lib> sublibs = new ArrayList<>();
                    for (final Lib sublib : dotClasspathModel.libs(slaveBuild.baseTree().root(),
                            scopeSegregator)) {
                        if (sublib.exported) {
                            sublibs.add(sublib);
                        }
                    }
                    builder.on(Lib.toDependencies(slaveBuild, sublibs, scopeSegregator));
                }
            }
        }
        return builder.build();
    }

}
