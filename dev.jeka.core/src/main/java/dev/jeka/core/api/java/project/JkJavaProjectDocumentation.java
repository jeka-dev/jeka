package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class JkJavaProjectDocumentation {

    private final JkJavaProject project;

    private final JkJavadocProcessor<JkJavaProjectDocumentation> javadocMaker;

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
        javadocMaker = JkJavadocProcessor.ofParent(this);
    }

    public JkJavaProjectDocumentation apply(Consumer<JkJavaProjectDocumentation> consumer) {
         consumer.accept(this);
         return this;
    }

    public JkJavadocProcessor<JkJavaProjectDocumentation> getJavadocProcessor() {
        return javadocMaker;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void run() {
        Iterable<Path> classpath = project.getDependencyManagement()
                .fetchDependencies(JkJavaDepScopes.SCOPES_FOR_COMPILATION).getFiles();
        Path dir = project.getOutputDir().resolve(javadocDir);
        JkPathTreeSet sources = project.getCompilation().getLayout().resolveSources();javadocMaker.make(classpath, sources, dir);
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

    void reset() {
        done = false;
    }

}
