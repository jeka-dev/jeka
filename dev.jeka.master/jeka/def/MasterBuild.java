import dev.jeka.core.CoreBuild;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

class MasterBuild extends JkClass {

    @JkDefImport("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.jacoco")
    JacocoPluginBuild jacocoBuild;

    public void make() {
        coreBuild.cleanPack();
        jacocoBuild.cleanPack();
        getImportedJkClasses().getDirects().forEach(build -> {
            build.clean();
            build.getPlugins().get(JkPluginJava.class).pack();
        }
        );
    }

}
