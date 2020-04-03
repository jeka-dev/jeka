package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;

public class JkJavaProjectDocumentationStep {

    private final JkJavaProject project;

    private final JkJavadocProcessor<JkJavaProjectDocumentationStep> javadocMaker;

    private final JkJavaProjectCompilationStep compilationStep;

    private boolean done;

    // relative to output path
    private String javadocDir;

    /**
     * For parent chaining
     */
    public final JkJavaProject.JkSteps __;

     JkJavaProjectDocumentationStep(JkJavaProject project, JkJavaProject.JkSteps parent) {
        this.project = project;
        this.__ = parent;
        javadocMaker = JkJavadocProcessor.ofParent(this);
        compilationStep = parent.getCompilation();

    }

    public JkJavadocProcessor<JkJavaProjectDocumentationStep> getJavadocProcessor() {
        return javadocMaker;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void run() {
        Iterable<Path> classpath = project.getDependencyManagement()
                .fetchDependencies(JkJavaDepScopes.SCOPES_FOR_COMPILATION).getFiles();
        Path dir = project.getOutputDir().resolve(javadocDir);
        javadocMaker.make(classpath, compilationStep.getLayout().getSources(), dir);
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

    public JkJavaProjectDocumentationStep setJavadocDir(String javadocDir) {
        this.javadocDir = javadocDir;
        return this;
    }

    void reset() {
        done = false;
    }

}
