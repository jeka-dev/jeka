package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.junit.JkUnit;

import java.io.Serializable;
import java.util.Map;


/**
 * Created by angibaudj on 02-08-17.
 */
@Deprecated // Experimental !!!!
public class JkJavaProjectDependency extends JkComputedDependency  {

    private JkJavaProject project;

    protected JkJavaProjectDependency(JkJavaProject project, final JkDependencyResolver dependencyResolver,
                                      final JkJavaCompiler compiler , final JkUnit uniter, Map<String, String> options) {
        super(new Invoker(project, dependencyResolver, compiler, uniter, options),
                project.getSourceLayout().baseDir(), jarAndRuntimeDeps(project, dependencyResolver, options).entries());
        this.project = project;
    }

    public JkJavaProject project() {
        return project;
    }

    private static class Invoker implements Runnable, Serializable {

        private static final long serialVersionUID = 1L;

        private final JkJavaProject project;

        private final JkDependencyResolver dependencyResolver;

        private final JkJavaCompiler compiler;

        private final JkUnit juniter;

        private final Map<String, String> options;

        Invoker(JkJavaProject project, JkDependencyResolver dependencyResolver,
                JkJavaCompiler compiler, JkUnit juniter, Map<String, String> options) {
            this.project = project;
            this.dependencyResolver = dependencyResolver;
            this.compiler = compiler;
            this.juniter = juniter;
            this.options = options;
        }

        @Override
        public void run() {
            project.buildMainJar(this.dependencyResolver, compiler, juniter, options);
        }

        @Override
        public String toString() {
            return this.project.toString();
        }
    }

    private static JkPath jarAndRuntimeDeps(JkJavaProject project, JkDependencyResolver dependencyResolver, Map<String, String> options) {
        return JkPath.of(dependencyResolver.get(project.dependencies(options))).andHead(project.getMainJar());
    }
}
