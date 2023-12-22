package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.KBean;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class IdeSupport {

    private IdeSupport() {
    }

    static JkIdeSupport getProjectIde(KBean kBean) {
        List<JkIdeSupportSupplier> suppliers = kBean.getRuntime().getBeans().stream()
                .filter(JkIdeSupportSupplier.class::isInstance)
                .map(JkIdeSupportSupplier.class::cast)
                .collect(Collectors.toList());
        return suppliers.stream()
                .map(supplier -> {
                    JkIdeSupport ideSupport = supplier.getJavaIdeSupport();
                    if (ideSupport != null) {
                        JkLog.info("Use %s class as IDE support supplier.", supplier.getClass().getName());
                    }
                    return ideSupport;
                })
                .filter(Objects::nonNull)
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
