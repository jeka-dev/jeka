package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;

public class JkJavaProjectMakerDocumentationStep {

    private final JkJavaProject project;

    private JkJavadocProcessor<JkJavaProjectMakerDocumentationStep> javadocMaker;

    private boolean done;

    /**
     * For parent chaining
     */
    public final JkJavaProject.JkSteps __;

    private JkJavaProjectMakerDocumentationStep(JkJavaProject project) {
        this.project = project;
        this.__ = project.getSteps();
    }

    static JkJavaProjectMakerDocumentationStep of(JkJavaProject project) {
        JkJavaProjectMakerDocumentationStep result = new JkJavaProjectMakerDocumentationStep(project);
        result.javadocMaker = JkJavadocProcessor.of(result);
        return result;
    }

    public JkJavadocProcessor<JkJavaProjectMakerDocumentationStep> getJavadocProcessor() {
        return javadocMaker;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void run() {
        Iterable<Path> classpath = project.getDependencyManagement()
                .fetchDependencies(JkJavaDepScopes.SCOPES_FOR_COMPILATION).getFiles();
        Path dir = project.getOutLayout().getJavadocDir();
        javadocMaker.make(classpath, project.getSourceLayout().getSources(), dir);
    }

    public void runIfNecessary() {
        if (done && !Files.exists(project.getOutLayout().getJavadocDir())) {
            JkLog.info("Javadoc already generated. Won't perfom again");
        } else {
            run();
            done = true;
        }
    }

    void reset() {
        done = false;
    }

}
