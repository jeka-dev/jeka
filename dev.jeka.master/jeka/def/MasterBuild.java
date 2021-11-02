import dev.jeka.core.CoreBuild;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefImport;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

class MasterBuild extends JkClass {

    @JkDefImport("../dev.jeka.core")
    CoreBuild coreBuild;

    @JkDefImport("../plugins/dev.jeka.plugins.jacoco")
    JacocoPluginBuild jacocoBuild;

    public void make() {
        getImportedJkClasses().getDirects().forEach(build -> {
            JkLog.startTask("Building " + build);
            build.clean();
            build.getPlugins().get(JkPluginJava.class).pack();
            JkLog.endTask();
        });
        runSamples();
    }

    public void buildPlugins() {
        jacocoBuild.cleanPack();
    }

    public void runSamples()  {
        new SamplesRunner().run();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(MasterBuild.class, args).make();
    }

}
