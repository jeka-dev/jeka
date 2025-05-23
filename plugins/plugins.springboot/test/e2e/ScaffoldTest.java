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

package e2e;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcHandler;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end tests about scaffolding.
 */
class ScaffoldTest {

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    private static Path PLUGIN_JAR;

    @BeforeAll
    static void beforeAll() {
        JkLog.setDecorator(JkLog.Style.INDENT);
        String relPath = "jeka-output/dev.jeka.springboot-plugin.jar";
        Path path = Paths.get(relPath);
        if (!Files.exists(path)) {
            path = Paths.get("plugins/plugins.springboot").resolve(relPath);
        }
        PLUGIN_JAR = path.toAbsolutePath().normalize();
    }

    @Test
    void scaffold_regularProject_ok() {
        RunChecker runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldArgs("project: scaffold");
        runChecker.buildCmd = withJavaVersionArgs("project: info test pack version=0.0.1");
        runChecker.run();
    }

    @Test
    @Disabled("Springboot 3.5.0 make junit fail when no src/main/test")
    void scaffold_simpleLayoutProject_ok() {
        RunChecker runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldArgs("springboot: project: layout.style=SIMPLE scaffold.jekaVersion=NO scaffold");
        runChecker.buildCmd = withJavaVersionArgs("project: test pack");
        runChecker.run();
    }

    @Test
    @Disabled("Springboot 3.5.0 make junit fail when no src/main/test")
    void scaffold_base_ok() {
        RunChecker runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldArgs("base: scaffold springboot: -Djeka.version=.");
        runChecker.buildCmd = withJavaVersionArgs("base: test pack");
        runChecker.cleanup = false;
        Path baseDir = runChecker.run();
        JkUtilsAssert.state(!Files.exists(baseDir.resolve("src")),
                "Base Springboot was scaffolded with project structure !");
        JkPathTree.of(baseDir).deleteRoot();
    }

    private String scaffoldArgs(String original)  {

        // inject springboot-plugin.jar both as a jeka dependencies (for running plugin)
        // And as a substitute of  @JkDep("${jeka.springboot.plugin.dependency}") in scaffolded project
        return String.format(original + " -Djeka.springboot.plugin.dependency=%s -cp=%s",
                PLUGIN_JAR, PLUGIN_JAR);
    }

    private static String withJavaVersionArgs(String original)  {

        // Needed to force starting springboot process with java 17
        return original + " -Djeka.java.version=17";
    }

    private class RunChecker {
        String scaffoldCmd;
        String buildCmd;
        String runCmd;
        boolean cleanup = true;
        int checkHttpTimeout = 8000;
        int checkHttpSleep = 2000;
        String url = "http://localhost:8080";

        Path run() {

            Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
            executor.runWithDistribJekaShell(path, scaffoldCmd);
            String jekaPropsContent = JkPathFile.of(path.resolve("jeka.properties")).readAsString();
            JkLog.info(jekaPropsContent);
            executor.runWithDistribJekaShell(path, buildCmd);

            // TODO Fix the problem of springboot jar on Windows
            if (runCmd != null) {
                JkUtilsAssert.state(!JkUtilsNet.isStatusOk(url, true), "A server is already listening to %s", url);

                System.out.println("======= Checking health with HTTP ================== ");
                System.out.println("Run command " + runCmd);

                // launch checker in separate process
                JkProcHandler handler = executor.prepareWithBaseDirJekaShell(path, runCmd)
                        .setCollectStdout(true)
                        .setCollectStderr(true)
                        .execAsync();

                // try to get a Ok response
                try {
                    JkUtilsNet.checkUntilOk(url, checkHttpTimeout, checkHttpSleep);
                } catch (RuntimeException e) {
                    System.out.println("=======Std out ================= ");
                    System.out.println(handler.getOutput());
                    System.out.println("=======Std err ================= ");
                    System.out.println(handler.getOutput());
                    throw e;
                }

                // destroy the sub-process
                handler.getProcess().destroyForcibly();
                boolean ended = handler.waitFor(2000, TimeUnit.MILLISECONDS);
                JkUtilsAssert.state(ended, "Can't kill process");

            } else {
                executor.runJeka(!JkUtilsSystem.IS_WINDOWS, path, buildCmd);
            }

            return path;
        }

    }

}
