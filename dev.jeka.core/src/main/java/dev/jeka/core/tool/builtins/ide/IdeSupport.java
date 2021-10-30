package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.java.project.JkJavaIdeSupport;
import dev.jeka.core.tool.JkClass;

import java.util.List;

final class IdeSupport {

    private IdeSupport() {
    }

    static JkJavaIdeSupport getProjectIde(JkClass jkClass) {
        if (jkClass instanceof JkJavaIdeSupport.JkSupplier) {
            JkJavaIdeSupport.JkSupplier supplier = (JkJavaIdeSupport.JkSupplier) jkClass;
            return supplier.getJavaIdeSupport();
        }
        List<JkJavaIdeSupport.JkSupplier> suppliers = jkClass.getPlugins().getLoadedPluginInstanceOf(
                JkJavaIdeSupport.JkSupplier.class);
        return suppliers.stream()
                .filter(supplier -> supplier != null)
                .map(supplier -> supplier.getJavaIdeSupport())
                .filter(projectIde -> projectIde != null)
                .findFirst().orElse(null);
    }
}
