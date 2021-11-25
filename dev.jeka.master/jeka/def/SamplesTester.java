import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
class SamplesTester extends JekaCommandLineExecutor {

    SamplesTester() {
        super("..");
    }

    void run() {
        runJeka("dev.jeka.samples.springboot", "@../../plugins/dev.jeka.plugins.springboot clean project#pack");
        runJekaw("dev.jeka.samples.basic", "-JKC=JavaPluginBuild cleanPackPublish");
        runJekaw("dev.jeka.samples.basic", "-JKC=SignedArtifactsBuild cleanPackPublish");
        runJekaw("dev.jeka.samples.basic", "-JKC=ThirdPartyPoweredBuild cleanPack");
        runJekaw("dev.jeka.samples.basic", "-JKC=AntStyleBuild cleanPackPublish");
        runJekaw("dev.jeka.samples.dependers", "-JKC=FatJarBuild clean project#pack");
        runJekaw("dev.jeka.samples.dependers", "-JKC=NormalJarBuild clean project#pack");
        runJekaw("dev.jeka.samples.junit5", "clean project#pack");
        runJekaw("dev.jeka.samples.junit5", "clean project#pack checkReportGenerated -project#tests.fork");
        runJeka("dev.jeka.samples.jacoco", "@../../plugins/dev.jeka.plugins.jacoco clean project#pack checkReportGenerated");
        runJeka("dev.jeka.samples.sonarqube", "@../../plugins/dev.jeka.plugins.sonarqube clean project#pack");
    }

    public void launchManually(String cmdLine) {
        Path dir = JkUtilsPath.createTempDirectory("jeka-sample-generated");
        runJeka(dir.toString(), cmdLine);
        try {
            Desktop.getDesktop().open(dir.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        JkLog.setDecorator(JkLog.Style.BRACE);
        new SamplesTester().run();
    }

    public static class ExtraLauncher {
        public static void main(String[] args) {
            new SamplesTester().launchManually("scaffold#run project#");
        }
    }


}
