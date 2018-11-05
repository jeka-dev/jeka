package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.system.JkLog;

import java.util.LinkedList;
import java.util.List;

public class JkJavaProjectJavadocTasks {

    private final JkJavaProjectMaker maker;

    private List<String> javadocOptions = new LinkedList<>();

    private boolean done;

    JkJavaProjectJavadocTasks(JkJavaProjectMaker maker) {
        this.maker = maker;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void run() {
        JkJavaProject project = maker.project;
        JkJavadocMaker.of(project.getSourceLayout().getSources(), maker.getOutLayout().getJavadocDir())
                .withClasspath(maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
                .andOptions(javadocOptions).process();
    }

    public void runIfNecessary() {
        if (done) {
            JkLog.info("Javadoc already generated. Won't perfom again");
        } else {
            run();
            done = true;
        }
    }

    public List<String> getJavadocOptions() {
        return this.javadocOptions;
    }

    public JkJavaProjectJavadocTasks setJavadocOptions(List<String> options) {
        this.javadocOptions = options;
        return this;
    }

    void reset() {
        done = false;
    }




}
