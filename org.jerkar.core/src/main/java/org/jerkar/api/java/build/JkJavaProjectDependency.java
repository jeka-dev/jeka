package org.jerkar.api.java.build;

import org.jerkar.api.depmanagement.JkComputedDependency;

import java.io.Serializable;


/**
 * Created by angibaudj on 02-08-17.
 */
@Deprecated // Experimental !!!!
public class JkJavaProjectDependency extends JkComputedDependency  {

    private JkJavaProject project;

    protected JkJavaProjectDependency(JkJavaProject project) {
        super(new Invoker(project), project.structure().baseDir(), project.asDependencyJars().entries());
        this.project = project;
    }

    public JkJavaProject project() {
        return project;
    }

    private static class Invoker implements Runnable, Serializable {

        private static final long serialVersionUID = 1L;

        private final JkJavaProject project;


        Invoker(JkJavaProject project) {
            super();
            this.project = project;
        }

        @Override
        public void run() {
            project.doPack();
        }

    }
}
