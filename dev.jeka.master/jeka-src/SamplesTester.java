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

    void run() {
        run("dev.jeka.samples.protobuf", "-liv @../../plugins/dev.jeka.plugins.protobuf project#cleanPack");
        run("dev.jeka.samples.basic", "-kb=simpleProject #cleanPackPublish #checkedValue=A #checkValueIsA");

        // springboot sample
        run("dev.jeka.samples.springboot", "-lna @../../plugins/dev.jeka.plugins.springboot " +
                    "project#clean project#pack mavenPublication#publishLocal -cw");

        run("dev.jeka.samples.basic", "-kb=signedArtifacts #cleanPackPublish");
        run("dev.jeka.samples.basic", "-kb=thirdPartyDependencies #cleanPack");
        run("dev.jeka.samples.basic", "-kb=antStyle #cleanPackPublish");
        run("dev.jeka.samples.dependers", "-kb=fatJar project#clean project#pack");
        run("dev.jeka.samples.dependers", "-kb=normalJar project#clean project#pack");
        run("dev.jeka.samples.junit5", "-lna project#clean project#pack");
        run("dev.jeka.samples.junit5", "project#clean project#pack #checkReportGenerated " +
                "project#tests.fork");
        run("dev.jeka.samples.jacoco", "-lna @../../plugins/dev.jeka.plugins.jacoco project#clean " +
                "project#pack #checkReportGenerated");
        run("dev.jeka.samples.sonarqube", "-lna @../../plugins/dev.jeka.plugins.sonarqube " +
                "project#clean project#pack");
    }

    private void run(String sampleDir, String cmdLine) {

        // assume running from 'master' dir
        Path sampleBaseDir = Paths.get("../samples").resolve(sampleDir).normalize();
        runWithDistribJekaShell(sampleBaseDir, cmdLine);
    }

}
