package test;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.utils.JkUtilsSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */
public class SamplesTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @BeforeAll
    static void beforeAll() {
        JkLog.setDecorator(JkLog.Style.INDENT);
    }

    @Test
    void baseapp_testPackDockerbuild_ok() {
        // Run Self-App
        run("samples.baseapp", "-Djeka.java.version=17 base: test pack -c");
        if (JkDocker.of().isPresent()) {
            run("samples.baseapp", "-Djeka.java.version=17 docker: build -c");
        }
    }

    @Test
    void projectApp_testRunJar_ok() {
        run("samples.project-app", "-c -Djeka.java.version=17 project: test runJar run.programArgs=oo");
    }

    @Test
    void baselib_testPackPublich_ok() {
        run("samples.baselib", "base: pack info : ok maven: publishLocal -cw -i");
    }

    @Test
    void baselib_runTwice_useCache() {
        Path sampleBaseDir = Paths.get("samples/samples.baselib").normalize();
        executor.runWithDistribJekaShell(sampleBaseDir, "ok");
        executor.runWithDistribJekaShell(sampleBaseDir, "ok");
    }

    @Test
    void springboot_ok() {
        run("samples.springboot",
                "-c -cw project: pack runJar run.programArgs=auto-close -Djeka.java.version=17");
    }

    @Test
    void sonarqube_ok() {
        Path sonarqubePluginJar = pluginsDir().resolve("plugins.sonarqube/jeka-output/dev.jeka.sonarqube-plugin.jar");
        run("samples.sonarqube", "-cw " +
                "-cp=" + sonarqubePluginJar +
                " project: info pack sonarqube: -Djeka.java.version=17");
    }

    @Test
    void protobuf_ok() {

        // Protobuf seems failed on last macos ship
        if (JkUtilsSystem.IS_MACOS && JkUtilsSystem.getProcessor().isAarch64()) {
            return;
        }
        Path protobufPluginJar = pluginsDir().resolve("plugins.protobuf/jeka-output/dev.jeka.protobuf-plugin.jar");
        run("samples.protobuf", "-ivc project: test pack -cp=" + protobufPluginJar);
    }

    @Test
    void jacoco_ok() {
        Path jacocoPluginJar = pluginsDir().resolve("plugins.jacoco/jeka-output/dev.jeka.jacoco-plugin.jar");
        run("samples.jacoco", "-c -la=false -cp=" + jacocoPluginJar +
                " project: test pack : checkGeneratedReport");
    }

    @Test
    void basic_ok() {
        run("samples.basic", "cleanPackPublish checkedValue=A checkValueIsA");
        run("samples.basic", "-Djeka.kbean.local=signedArtifacts cleanPackPublish");
        run("samples.basic", "-Djeka.kbean.local=thirdPartyDependencies cleanPack");
        run("samples.basic", "-Djeka.kbean.local=antStyle cleanPackPublish");
    }

    @Test
    void dependee_ok() {
        run("samples.dependers", "-Djeka.kbean.local=fatJar -c project: pack");
        run("samples.dependers", "-Djeka.kbean.local=normalJar -c project: pack");
    }

    @Test
    void junit5_ok() {
        run("samples.junit5", "-la=false -c project: pack");
        run("samples.junit5", "-c project: test pack : checkReportGenerated project: test.fork=true");
    }

    private void run(String sampleDir, String cmdLine) {

        // assume running from 'master' dir
        Path sampleBaseDir = Paths.get("samples").resolve(sampleDir).normalize();
        executor.runWithDistribJekaShell(sampleBaseDir, cmdLine);
    }

    private Path pluginsDir() {
        if (Paths.get("").toAbsolutePath().getFileName().toString().equals("jeka")) {
            return Paths.get("plugins");
        }
        return Paths.get("..");
    }

}
