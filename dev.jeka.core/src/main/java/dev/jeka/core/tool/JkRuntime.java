package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;

import java.nio.file.Path;
import java.util.Set;

public final class JkRuntime {

    private static JkClass instance;

    private static JkDependencySet dependencies;

    private static JkDependencyResolver dependencyResolver;

    private static Set<Path> importedProjectDirs;

    private JkRuntime() {}

    static void setInstance(JkClass instanceArg) {
        instance = instanceArg;
    }

    public JkBeanRegistry getJkBeanRegistry() {
        return instance.getJkBeanRegistry();
    }

    public <T extends JkBean> JkBean getJkBean(Class<T> jkBeanClass) {
        return instance.getJkBeanRegistry().get(jkBeanClass);
    }

    public JkRepoSet getDownloadRepos() {
        return instance.getDefDependencyResolver().getRepos();
    }

    static void setDependenciesAndResolver(JkDependencySet dependenciesArg, JkDependencyResolver resolverArg) {
        dependencies = dependenciesArg;
        dependencyResolver = resolverArg;
    }

    public static void setImportedProjectDirs(Set<Path> importedProjectDirs) {
        JkRuntime.importedProjectDirs = importedProjectDirs;
    }
}
