package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.tool.JkClass;

import java.util.List;

final class IdeSupport {

    private IdeSupport() {
    }

    static JkIdeSupport getProjectIde(JkClass jkClass) {
        if (jkClass instanceof JkIdeSupport.JkSupplier) {
            JkIdeSupport.JkSupplier supplier = (JkIdeSupport.JkSupplier) jkClass;
            return supplier.getJavaIdeSupport();
        }
        List<JkIdeSupport.JkSupplier> suppliers = jkClass.getJkBeanRegistry().getLoadedPluginInstanceOf(
                JkIdeSupport.JkSupplier.class);
        return suppliers.stream()
                .filter(supplier -> supplier != null)
                .map(supplier -> supplier.getJavaIdeSupport())
                .filter(projectIde -> projectIde != null)
                .findFirst().orElse(null);
    }
}
