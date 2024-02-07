import dev.jeka.core.JekaCommandLineExecutor;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
class SamplesTester extends JekaCommandLineExecutor {

    void run() {

        // Test with injecting dep via @JkInjectClasspath(...)
        run("dev.jeka.samples.springboot", "-lna  " +
                "project#clean project#pack maven#publishLocal -cw -Djeka.java.version=17");

        // Test with injecting plugin dep via "+"
        run("dev.jeka.samples.sonarqube", "-lri " +
                "+../../plugins/dev.jeka.plugins.sonarqube/jeka-output/dev.jeka.sonarqube-plugin.jar " +
                "project#cleanPack sonarqube# -Djeka.java.version=17");

        // Test with injecting plugin dep via jeka.properties file
        run("dev.jeka.samples.protobuf", "-liv project#cleanPack");

        // Test with injecting dep via @JkInjectClasspath(...)
        run("dev.jeka.samples.jacoco", "-lna +../../plugins/dev.jeka.plugins.jacoco project#cleanPack " +
                "project#pack #checkGeneratedReport");



        // No Jeka deps test samples
        run("dev.jeka.samples.basic", "-kb=simpleProject #cleanPackPublish #checkedValue=A #checkValueIsA");
        run("dev.jeka.samples.basic", "-kb=signedArtifacts #cleanPackPublish");
        run("dev.jeka.samples.basic", "-kb=thirdPartyDependencies #cleanPack");
        run("dev.jeka.samples.basic", "-kb=antStyle #cleanPackPublish");

        // Test with @JkInjectBaserun
        run("dev.jeka.samples.dependers", "-kb=fatJar project#cleanPack");
        run("dev.jeka.samples.dependers", "-kb=normalJar project#cleanPack");

        // Test with junit5
        run("dev.jeka.samples.junit5", "-lna project#cleanPack");
        run("dev.jeka.samples.junit5", "project#clean project#pack #checkReportGenerated " +
                "project#tests.fork=true");

        // Run Self-App
       // run("dev.deja.samples.selfapp", "self#buildJar");
       // run("dev.deja.samples.baselib", "self#buildJar");
    }

    private void run(String sampleDir, String cmdLine) {

        // assume running from 'master' dir
        Path sampleBaseDir = Paths.get("../samples").resolve(sampleDir).normalize();
        runWithDistribJekaShell(sampleBaseDir, cmdLine);
    }

}
