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
        runjeka("dev.jeka.samples.springboot", "@../../plugins/dev.jeka.plugins.springboot clean java#pack");
        runjekaw("dev.jeka.samples.basic", "-JKC=JavaPluginBuild cleanPackPublish");
        runjekaw("dev.jeka.samples.basic", "-JKC=SignedArtifactsBuild cleanPackPublish");
        runjekaw("dev.jeka.samples.basic", "-JKC=ThirdPartyPoweredBuild cleanPack");
        runjekaw("dev.jeka.samples.basic", "-JKC=AntStyleBuild cleanPackPublish");
        runjekaw("dev.jeka.samples.dependers", "-JKC=FatJarBuild clean java#pack");
        runjekaw("dev.jeka.samples.dependers", "-JKC=NormalJarBuild clean java#pack");
        runjekaw("dev.jeka.samples.junit5", "clean java#pack");
        runjekaw("dev.jeka.samples.junit5", "clean java#pack checkReportGenerated -java#tests.fork");
        runjeka("dev.jeka.samples.jacoco", "@../../plugins/dev.jeka.plugins.jacoco clean java#pack checkReportGenerated");
        runjeka("dev.jeka.samples.sonarqube", "@../../plugins/dev.jeka.plugins.sonarqube clean java#pack");
    }

    public void launchManually(String cmdLine) {
        Path dir = JkUtilsPath.createTempDirectory("jeka-sample-generated");
        runjeka(dir.toString(), cmdLine);
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
            new SamplesTester().launchManually("scaffold#run java#");
        }
    }


}
