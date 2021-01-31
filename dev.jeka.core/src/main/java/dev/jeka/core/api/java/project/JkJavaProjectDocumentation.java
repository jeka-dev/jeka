package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Responsible to create Javadoc and Source jar.
 */
public class JkJavaProjectDocumentation {

    private final JkJavaProject project;

    private final JkJavadocProcessor<JkJavaProjectDocumentation> javadocProcessor;

    private boolean done;

    // relative to output path
    private String javadocDir = "javadoc";

    /**
     * For parent chaining
     */
    public final JkJavaProject __;

     JkJavaProjectDocumentation(JkJavaProject project) {
        this.project = project;
        this.__ = project;
        javadocProcessor = JkJavadocProcessor.ofParent(this);
    }

    public JkJavaProjectDocumentation apply(Consumer<JkJavaProjectDocumentation> consumer) {
         consumer.accept(this);
         return this;
    }

    public JkJavadocProcessor<JkJavaProjectDocumentation> getJavadocProcessor() {
        return javadocProcessor;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void run() {
        JkScope[] scopes = new JkScope[] {JkScope.COMPILE};
        Iterable<Path> classpath = project.getConstruction().getDependencyManagement()
                .fetchDependencies(scopes).getFiles();
        Path dir = project.getOutputDir().resolve(javadocDir);
        JkPathTreeSet sources = project.getConstruction().getCompilation().getLayout().resolveSources();
        javadocProcessor.make(classpath, sources, dir);
    }

    public void runIfNecessary() {
        if (done && !Files.exists(project.getOutputDir().resolve(javadocDir))) {
            JkLog.info("Javadoc already generated. Won't perfom again");
        } else {
            run();
            done = true;
        }
    }

    public Path getJavadocDir() {
        return project.getOutputDir().resolve(javadocDir);
    }

    public JkJavaProjectDocumentation setJavadocDir(String javadocDir) {
        this.javadocDir = javadocDir;
        return this;
    }

    public void createJavadocJar(Path target) {
        runIfNecessary();
        Path javadocDir = getJavadocDir();
        /*
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }*/
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public void createJavadocJar() {
        createJavadocJar(project.getArtifactPath(JkJavaProjectPublication.JAVADOC_ARTIFACT_ID));
    }

    public void createSourceJar(Path target) {
        JkJavaProjectCompilation compilation = project.getConstruction().getCompilation();
        compilation.getLayout().resolveSources().and(compilation
                .getLayout().resolveGeneratedSourceDir()).zipTo(target);
    }

    public void createSourceJar() {
        createSourceJar(project.getArtifactPath(JkJavaProjectPublication.SOURCES_ARTIFACT_ID));
    }

    void reset() {
        done = false;
    }

}
