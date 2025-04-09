/*
 * Copyright 2014-2025  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package _dev.test;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.utils.JkUtilsSystem;

import dev.jeka.core.tool.JkDep;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class has to be run using dev.jeka.master as working dir.
 * It assumes that dev.jeka.core module has already been built.
 */

@JkDep("org.junit.jupiter:junit-jupiter:5.12.0")
@JkDep("org.junit.platform:junit-platform-launcher:1.12.0")
class SamplesTest {

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
        Path sonarqubePluginJar = fromPlugins("plugins.sonarqube/jeka-output/dev.jeka.sonarqube-plugin.jar");
        run("samples.sonarqube", "-cw -i -v " +
                "-cp=" + sonarqubePluginJar +
                " project: info pack sonarqube: -Djeka.java.version=17");
    }

    @Test
    void protobuf_ok() {

        // Protobuf seems failed on last macos ship
        if (JkUtilsSystem.IS_MACOS && JkUtilsSystem.getProcessor().isAarch64()) {
            return;
        }
        Path protobufPluginJar = fromPlugins("plugins.protobuf/jeka-output/dev.jeka.protobuf-plugin.jar");
        run("samples.protobuf", "-ivc project: test pack -cp=" + protobufPluginJar);
    }

    @Test
    void jacoco_ok() {
        Path jacocoPluginJar = fromPlugins("plugins.jacoco/jeka-output/dev.jeka.jacoco-plugin.jar");
        run("samples.jacoco", "-c -la=false -cp=" + jacocoPluginJar +
                " project: test pack : checkGeneratedReport");
    }

    @Test
    void basic_ok() {
        run("samples.basic", "cleanPackPublish checkedValue=A checkValueIsA");
        run("samples.basic", "--kbean=signedArtifacts cleanPackPublish");
        run("samples.basic", "--kbean=thirdPartyDependencies cleanPack");
        run("samples.basic", "--kbean=antStyle cleanPackPublish");
    }

    @Test
    void dependee_ok() {
        run("samples.dependers", "--kbean=fatJar -c -i project: pack");
        run("samples.dependers", "--kbean=normalJar -c project: pack");
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

    private static Path pluginsDir() {
        if (Paths.get("").toAbsolutePath().getFileName().toString().equals("jeka")) {
            return Paths.get("plugins");
        }
        return Paths.get("..");
    }

    private static Path fromPlugins(String relativePath) {
        Path result = pluginsDir().resolve(relativePath).toAbsolutePath().normalize();
        if (!Files.exists(result)) {
            throw new IllegalStateException(result + " not found");
        }
        return result;
    }

}
