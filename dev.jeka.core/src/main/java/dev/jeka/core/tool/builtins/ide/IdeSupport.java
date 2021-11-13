package dev.jeka.core.tool.builtins.ide;

import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkRuntime;

import java.util.List;

final class IdeSupport {

    private IdeSupport() {
    }

    static JkIdeSupport getProjectIde(JkBean jkBean) {
        if (jkBean instanceof JkIdeSupport.JkSupplier) {
            JkIdeSupport.JkSupplier supplier = (JkIdeSupport.JkSupplier) jkBean;
            return supplier.getJavaIdeSupport();
        }
        List<JkIdeSupport.JkSupplier> suppliers = jkBean.getRuntime().getBeanRegistry().getLoadedPluginInstanceOf(
                JkIdeSupport.JkSupplier.class);
        return suppliers.stream()
                .filter(supplier -> supplier != null)
                .map(supplier -> supplier.getJavaIdeSupport())
                .filter(projectIde -> projectIde != null)
                .findFirst().orElse(null);
    }
}
