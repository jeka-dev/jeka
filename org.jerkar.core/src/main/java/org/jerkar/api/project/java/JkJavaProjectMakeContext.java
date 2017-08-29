package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.junit.JkUnit;

public class JkJavaProjectMakeContext {

    public static JkJavaProjectMakeContext of() {
        return new JkJavaProjectMakeContext(JkDependencyResolver.of(JkRepo.mavenCentral()), JkJavaCompiler.base(),
                JkUnit.of(), null);
    }

    private final JkDependencyResolver dependencyResolver;

    private final JkJavaCompiler baseCompiler;

    private final JkUnit juniter;

    private final Class<?> javadocDoclet;

    private JkJavaProjectMakeContext(JkDependencyResolver dependencyResolver, JkJavaCompiler baseCompiler,
                                     JkUnit juniter, Class<?> javadocDoclet) {
        this.dependencyResolver = dependencyResolver;
        this.baseCompiler = baseCompiler;
        this.juniter = juniter;
        this.javadocDoclet = javadocDoclet;
    }

    public JkDependencyResolver dependencyResolver() {
        return dependencyResolver;
    }

    public JkJavaCompiler baseCompiler() {
        return baseCompiler;
    }

    public Class javadocDoclet() {
        return javadocDoclet;
    }

    public JkUnit juniter() {
        return juniter;
    }

    public JkJavaProjectMakeContext with(JkDependencyResolver resolver) {
        return new JkJavaProjectMakeContext(resolver, this.baseCompiler(), this.juniter, this.javadocDoclet);
    }

    public JkJavaProjectMakeContext with(JkUnit juniter) {
        return new JkJavaProjectMakeContext(this.dependencyResolver, this.baseCompiler(), juniter, this.javadocDoclet);
    }

    public JkJavaProjectMakeContext with(JkJavaCompiler baseCompiler) {
        return new JkJavaProjectMakeContext(this.dependencyResolver, baseCompiler, this.juniter, this.javadocDoclet);
    }

    public JkJavaProjectMakeContext with(Class<?> javadocDoclet) {
        return new JkJavaProjectMakeContext(this.dependencyResolver, this.baseCompiler, this.juniter, javadocDoclet);
    }


}
