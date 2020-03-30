package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.java.JkJavadocMaker;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class JkJavaProjectMakerDocumentationStep {

    private final JkJavaProjectMaker maker;

    private List<String> javadocOptions = new LinkedList<>();

    private boolean done;

    JkJavaProjectMakerDocumentationStep(JkJavaProjectMaker maker) {
        this.maker = maker;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void run() {
        final JkJavaProject project = maker.project;
        JkJavadocMaker.of(project.getSourceLayout().getSources(), maker.getOutLayout().getJavadocDir())
        .withClasspath(maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
        .andOptions(javadocOptions).process();
    }

    public void runIfNecessary() {
        if (done && !Files.exists(maker.getOutLayout().getJavadocDir())) {
            JkLog.info("Javadoc already generated. Won't perfom again");
        } else {
            run();
            done = true;
        }
    }

    public List<String> getJavadocOptions() {
        return this.javadocOptions;
    }

    public JkJavaProjectMakerDocumentationStep setJavadocOptions(List<String> options) {
        this.javadocOptions = options;
        return this;
    }

    public JkJavaProjectMakerDocumentationStep setJavadocOptions(String ... options) {
        return this.setJavadocOptions(JkUtilsIterable.listOf(options));
    }

    void reset() {
        done = false;
    }




}
