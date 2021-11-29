package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.tool.JkBean;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

final class IdeSupport {

    private IdeSupport() {
    }

    static JkIdeSupport getProjectIde(JkBean jkBean) {
        if (jkBean instanceof JkIdeSupport.JkSupplier) {
            JkIdeSupport.JkSupplier supplier = (JkIdeSupport.JkSupplier) jkBean;
            return supplier.getJavaIdeSupport();
        }
        List<JkIdeSupport.JkSupplier> suppliers = jkBean.getRuntime().getBeans().stream()
                .filter(JkIdeSupport.JkSupplier.class::isInstance)
                .map(JkIdeSupport.JkSupplier.class::cast)
                .collect(Collectors.toList());
        return suppliers.stream()
                .filter(supplier -> supplier != null)
                .map(supplier -> supplier.getJavaIdeSupport())
                .filter(projectIde -> projectIde != null)
                .findFirst().orElse(null);
    }

    static JkDependencySet classpathAsDependencySet() {
        JkClassLoader classLoader = JkClassLoader.ofCurrent();
        final JkClasspath classpath;
        if (classLoader.get() instanceof URLClassLoader) {
            classpath = JkUrlClassLoader.ofCurrent().getDirectClasspath();
        } else {
            classpath = JkClasspath.ofCurrentRuntime();
        }
        JkDependencySet dependencySet = JkDependencySet.of();
        for (Path entry : classpath) {
            dependencySet = dependencySet.andFiles(entry);
        }
        return dependencySet;
    }
}
