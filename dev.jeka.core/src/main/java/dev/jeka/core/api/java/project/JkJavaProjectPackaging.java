package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkManifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tasks for packaging artifacts created by the holding project.
 */
public class JkJavaProjectPackaging {

    private final JkJavaProject project;

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private final JkManifest manifest;

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.ofEmpty();
    
    /**
     * For Parent chaining
     */
    public JkJavaProject __;

    JkJavaProjectPackaging(JkJavaProject project) {
        this.project = project;
        this.__ = project;
        manifest = JkManifest.ofParent(this);
    }

    public JkJavaProjectPackaging apply(Consumer<JkJavaProjectPackaging> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkManifest<JkJavaProjectPackaging> getManifest() {
        return manifest;
    }

    /**
     * Returns an artifact file name supplier for NOT including version in artifact file names.
     */
    public Supplier<String> getModuleNameFileNameSupplier() {
        return () -> defaultVersionedModule().getModuleId().getDotedName();
    }

    private JkVersionedModule defaultVersionedModule() {
        return JkVersionedModule.of(project.getPublication().getModuleId(), project.getPublication().getVersion());
    }

    public void createBinJar(Path target) {
        project.getCompilation().runIfNecessary();
        project.getTesting().runIfNecessary();
        JkJarPacker.of(project.getCompilation().getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeJar(target);
    }

    public void createFatJar(Path target) {
        project.getCompilation().runIfNecessary();
        project.getTesting().runIfNecessary();
        Iterable<Path> classpath = project.getDependencyManagement()
                .fetchDependencies(JkJavaDepScopes.RUNTIME).getFiles();
        JkJarPacker.of(project.getCompilation().getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath, this.fatJarFilter);
    }

    public void createSourceJar(Path target) {
        project.getCompilation().getLayout().resolveSources().and(project.getCompilation()
                .getLayout().resolveGeneratedSourceDir()).zipTo(target);
    }

    void createJavadocJar(Path target) {
        project.getDocumentation().runIfNecessary();
        Path javadocDir = project.getDocumentation().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return this.extraFilesToIncludeInFatJar;
    }

    /**
     * File trees specified here will be added to the fat jar.
     */
    public JkJavaProjectPackaging setExtraFilesToIncludeInFatJar(JkPathTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

}
