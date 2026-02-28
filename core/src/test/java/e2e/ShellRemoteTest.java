/*
 * Copyright 2014-2024  the original author or authors.
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

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.docker.JkDocker;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
class ShellRemoteTest  {

    private static final String GIT_URL = "https://github.com/jeka-dev/sample-for-integration-test.git#0.0.1";

    private static final String COWSAY_VERSION = "0.0.6";

    private static final String COW_SAY_URL = "https://github.com/jeka-dev/demo-cowsay"; //#" + COWSAY_VERSION;

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @BeforeAll
    public static void beforeAll() {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
    }

    @AfterAll
    public static void afterAll() {
        JkLog.restoreToInitialState();
        JkLog.setVerbosity(JkLog.Verbosity.INFO);
    }

    @Test
    void sampleItTest_displayOk() {
        String output = prepareJeka("-ru %s ok", GIT_URL).exec().getStdoutAsString();
        Assertions.assertEquals("ok" + System.lineSeparator(), output);
    }

    @Test
    void sampleItTest_specificJdk_displayOk() {
        String javaVersion = "20";
        String distro = "corretto";

        // Delete cached distrib to force reloading
        Path cachedJdk = JkLocator.getCacheDir().resolve("jdks").resolve(distro + "-" + javaVersion);
        JkPathTree.of(cachedJdk).createIfNotExist().deleteRoot();

        String output = prepareJeka("-ru %s ok", GIT_URL)
                .setEnv("jeka.java.version", javaVersion)
                .setEnv("jeka.java.distrib", distro)
                .exec().getStdoutAsString();
        Assertions.assertEquals("ok" + System.lineSeparator(), output);
    }

    @Test
    void sampleItTest_withShorthand_displayOk() {
        String output = prepareJeka("::myShortHand")
                .setEnv("jeka.cmd.myShortHand", "-r " + GIT_URL + " ok")
                .exec().getStdoutAsString();
        Assertions.assertEquals("ok" + System.lineSeparator(), output);
    }

    @Test
    void cowsay_currentJekaVersion_ok() {

        // We want also testing that sys properties declared as program arguments are
        // handled as regular sys properties
        JkProcResult result = prepareJeka("-ru %s -Djeka.java.version=21 -Djeka.version=. -p HelloJeKa -Dcowsay.prefix=Mooo", COW_SAY_URL)
                .exec();
        String stdout = result.getStdoutAsString();
        String stderr = result.getStderrAsString();

        String errMsg1 = String.format("Expecting output containing 'MoooHelloJeKa', " +
                "was %n%s %n stderr was %n%s", stdout, stderr);
        String errMsg2 = String.format("Expecting output starting " +
                "with '_____', was %n%s %n stderr was %n%s", stdout, stderr);

        assertTrue(stdout.contains("MoooHelloJeKa"), errMsg1);
        assertTrue(stdout.trim().startsWith("_____"), errMsg2);
    }

    @Test
    void cowsay_dockerBuild_ok() {
        if (isDockerAbsent()) {
            return;
        }
        prepareJeka("-ru %s -Djeka.java.version=21 docker: build", COW_SAY_URL).exec();

        // Run image
        JkDocker.of().addParams("run", "--rm", "github.com_jeka-dev_demo-cowsay:" + COWSAY_VERSION, "toto")
                .setLogCommand(true)
                .setInheritIO(false)
                .setLogWithJekaDecorator(true)
                .exec();
    }

    @Test
    @Disabled("Re-enable after release")
    void cowsay_dockerBuildNative_ok() {
        if (isDockerAbsent()) {
            return;
        }
        prepareJeka("-v -ru %s -Djeka.java.version=21 docker: buildNative", COW_SAY_URL).exec();
    }

    private JkProcess prepareJeka(String cmdLine, String... tokens) {
       return executor.prepareJeka(false, Paths.get(""), String.format(cmdLine, (Object[]) tokens))
               .setLogCommand(true)
               .setCollectStderr(true)
               .setCollectStdout(true);
    }

    private static boolean isDockerAbsent() {
        if (!JkDocker.of().isPresent()) {
            JkLog.warn("Docker not present. Skip docker tests");
            return true;
        }
        return false;
    }

}
