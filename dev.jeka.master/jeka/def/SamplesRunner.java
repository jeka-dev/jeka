import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
class SamplesRunner extends JekaCommandLineExecutor {

    SamplesRunner() {
        super("..");
    }

    void run() {
        runjekaw("dev.jeka.samples.basic", "-JKC=JavaPluginBuild cleanPackPublish");
        runjekaw("dev.jeka.samples.basic", "-JKC=SignedArtifactsBuild cleanPackPublish");
        runjekaw("dev.jeka.samples.basic", "-JKC=ThirdPartyPoweredBuild cleanPack");
        runjekaw("dev.jeka.samples.basic", "-JKC=AntStyleBuild cleanPackPublish");
        runjekaw("dev.jeka.samples.dependers", "-JKC=FatJarBuild clean java#pack");
        runjekaw("dev.jeka.samples.dependers", "-JKC=NormalJarBuild clean java#pack");
        runjekaw("dev.jeka.samples.junit5", "clean java#pack");
        runjekaw("dev.jeka.samples.junit5", "clean java#pack checkReportGenerated -java#tests.fork");
        runjeka("dev.jeka.samples.jacoco", "@../../plugins/dev.jeka.plugins.jacoco clean java#pack checkReportGenerated");
        testScaffoldWithExternalPlugin();
    }

    private void testScaffoldWithExternalPlugin() {
        JkLog.info("Test scaffold with springboot plugin");
        Path dir = JkUtilsPath.createTempDirectory("jeka-test");
        JkProcess.ofWinOrUx("jeka.bat", "jeka").setWorkingDir(dir)
                .addParams("-LB", "-LRI", "-LSU", "-LV=false", "-JKC=",
                        "scaffold#run", "@dev.jeka:springboot-plugin:+", "springboot#")
                .setFailOnError(true).exec();
        JkPathTree.of(dir).deleteRoot();
    }

    public static void main(String[] args) throws IOException {
        new SamplesRunner().run();
    }

}
