import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
class SamplesTester extends JekaCommandLineExecutor {

    SamplesTester(JkProperties properties) {
        super(Paths.get(".."), properties);
    }

    void run() {
        runDistribJeka("dev.jeka.samples.protobuf", "-liv @../../plugins/dev.jeka.plugins.protobuf project#cleanPack");
        runDistribJeka("dev.jeka.samples.basic", "-kb=simpleProject #cleanPackPublish #checkedValue=A #checkValueIsA");
        if (JkJavaVersion.ofCurrent().isEqualOrGreaterThan(JkJavaVersion.V17)) {
            runDistribJeka("dev.jeka.samples.springboot", "-lna @../../plugins/dev.jeka.plugins.springboot project#clean project#pack mavenPublication#publishLocal -cw");
        }
        runDistribJeka("dev.jeka.samples.basic", "-kb=signedArtifacts #cleanPackPublish");
        runDistribJeka("dev.jeka.samples.basic", "-kb=thirdPartyDependencies #cleanPack");
        runDistribJeka("dev.jeka.samples.basic", "-kb=antStyle #cleanPackPublish");
        runDistribJeka("dev.jeka.samples.dependers", "-kb=fatJar project#clean project#pack");
        runDistribJeka("dev.jeka.samples.dependers", "-kb=normalJar project#clean project#pack");
        runDistribJeka("dev.jeka.samples.junit5", "-lna project#clean project#pack");
        runDistribJeka("dev.jeka.samples.junit5", "project#clean project#pack #checkReportGenerated -project#tests.fork");
        runDistribJeka("dev.jeka.samples.jacoco", "-lna @../../plugins/dev.jeka.plugins.jacoco project#clean project#pack #checkReportGenerated");
        runDistribJeka("dev.jeka.samples.sonarqube", "-lna @../../plugins/dev.jeka.plugins.sonarqube project#clean project#pack");
    }

    public void launchManually(String cmdLine) {
        Path dir = JkUtilsPath.createTempDirectory("jeka-sample-generated");
        runDistribJeka(dir.toString(), cmdLine);
        try {
            Desktop.getDesktop().open(dir.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
