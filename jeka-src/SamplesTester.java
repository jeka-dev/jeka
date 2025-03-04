import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
class SamplesTester extends JekaCommandLineExecutor {

    void run() {

        // Run Self-App
        run("samples.baseapp", "-Djeka.java.version=17 base: test pack -c");
        if (JkDocker.of().isPresent()) {
            run("samples.baseapp", "-Djeka.java.version=17 docker: build -c");
        }

        // Run project app
        run("samples.project-app", "-Djeka.java.version=17 project: test runJar run.programArgs=oo" +
                " -c");

        // Test also if the KBean hosted in jeka-src, is considered as the default KBean
        run("samples.baselib", "base: pack : ok --debug --inspect");

        // Test caching by running twice
        Path sampleBaseDir = Paths.get("samples/samples.baselib").normalize();

        runWithDistribJekaShell(sampleBaseDir, "ok");
        runWithDistribJekaShell(sampleBaseDir, "ok");


        // Test with injecting dep via @JkDep(...)
        run("samples.springboot", "-la=false -c -cw " +
                "project: pack runJar run.programArgs=auto-close maven: publishLocal -Djeka.java.version=17");

        // Test with injecting plugin dep via "+"
        run("samples.sonarqube", "-vic " +
                "-cp=../../plugins/plugins.sonarqube/jeka-output/dev.jeka.sonarqube-plugin.jar " +
                "project: info pack sonarqube: -Djeka.java.version=17");

        // Protobuf seems failed on last macos ship
        if (!(JkUtilsSystem.IS_MACOS && JkUtilsSystem.getProcessor().isAarch64())) {
            // Test with injecting plugin dep via jeka.properties file
            run("samples.protobuf", "-ivc project: test pack -cp=../../plugins/plugins.protobuf/jeka-output/dev.jeka.protobuf-plugin.jar");
        }

        // Test with injecting dep via @JkDep(...)
        run("samples.jacoco", "-c -la=false -cp=../../plugins/dev.jeka.plugins.jacoco " +
                "project: test pack : checkGeneratedReport");

        // No Jeka deps test samples
        run("samples.basic", "cleanPackPublish checkedValue=A checkValueIsA");
        run("samples.basic", "-Djeka.kbean.local=signedArtifacts cleanPackPublish");
        run("samples.basic", "-Djeka.kbean.local=thirdPartyDependencies cleanPack");
        run("samples.basic", "-Djeka.kbean.local=antStyle cleanPackPublish");

        // Test with @JkInjectBaserun
        run("samples.dependers", "-Djeka.kbean.local=fatJar -c project: pack");
        run("samples.dependers", "-Djeka.kbean.local=normalJar -c project: pack");

        // Test with junit5
        run("samples.junit5", "-la=false -c project: pack");
        run("samples.junit5", "-c project: test pack : checkReportGenerated project: test.fork=true");
    }

    private void run(String sampleDir, String cmdLine) {

        // assume running from 'master' dir
        Path sampleBaseDir = Paths.get("samples").resolve(sampleDir).normalize();
        runWithDistribJekaShell(sampleBaseDir, cmdLine);
    }

}
