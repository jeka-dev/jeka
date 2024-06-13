import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.tooling.docker.JkDocker;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
class SamplesTester extends JekaCommandLineExecutor {

    void run() {

        // Run Self-App
        /*
        run("dev.jeka.samples.baseapp", "-Djeka.java.version=17 base: pack -c");
        if (JkDocker.isPresent()) {
            run("dev.jeka.samples.baseapp", "-Djeka.java.version=17 docker: build -c");
        }

         */

        // Test also if the KBean hosted in jeka-src, is considered as the default KBean
        //run("dev.jeka.samples.baselib", "base: pack : ok --debug --inspect");

        // Test caching by running twice
        Path sampleBaseDir = Paths.get("../samples/dev.jeka.samples.baselib").normalize();
        /*
        runWithDistribJekaShell(sampleBaseDir, "ok");
        runWithDistribJekaShell(sampleBaseDir, "ok");

         */


        // Test with injecting dep via @JkInjectClasspath(...)
        run("dev.jeka.samples.springboot", "-la=false -c -cw " +
                "project: pack runJar run.programArgs=auto-close maven: publishLocal -Djeka.java.version=17");

        // Test with injecting plugin dep via "+"
        run("dev.jeka.samples.sonarqube", "-vic " +
                "-cp=../../plugins/dev.jeka.plugins.sonarqube/jeka-output/dev.jeka.sonarqube-plugin.jar " +
                "project: info pack sonarqube: -Djeka.java.version=17");

        // Test with injecting plugin dep via jeka.properties file
        run("dev.jeka.samples.protobuf", "-ivc project: pack");

        // Test with injecting dep via @JkInjectClasspath(...)
        run("dev.jeka.samples.jacoco", "-la=false -cp=../../plugins/dev.jeka.plugins.jacoco " +
                "project: pack : checkGeneratedReport");

        // No Jeka deps test samples
        run("dev.jeka.samples.basic", "-kb=simpleProject cleanPackPublish checkedValue=A checkValueIsA");
        run("dev.jeka.samples.basic", "-kb=signedArtifacts cleanPackPublish");
        run("dev.jeka.samples.basic", "-kb=thirdPartyDependencies cleanPack");
        run("dev.jeka.samples.basic", "-kb=antStyle cleanPackPublish");

        // Test with @JkInjectBaserun
        run("dev.jeka.samples.dependers", "-kb=fatJar -c project: pack");
        run("dev.jeka.samples.dependers", "-kb=normalJar -c project: pack");

        // Test with junit5
        run("dev.jeka.samples.junit5", "-la=false -c project: pack");
        run("dev.jeka.samples.junit5", "-c project: pack : checkReportGenerated project: tests.fork=true");
    }

    private void run(String sampleDir, String cmdLine) {

        // assume running from 'master' dir
        Path sampleBaseDir = Paths.get("../samples").resolve(sampleDir).normalize();
        runWithDistribJekaShell(sampleBaseDir, cmdLine);
    }

}
