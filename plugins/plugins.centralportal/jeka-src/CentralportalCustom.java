import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIntellijIml;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

class CentralportalCustom extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIntellijIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.compilation.dependencies
                .add(JkLocator.getJekaJarPath());
    }

}