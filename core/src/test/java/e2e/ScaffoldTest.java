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

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class ScaffoldTest {

    private Path runDir;

    private final JekaCmdLineExecutor executor = new JekaCmdLineExecutor();

    @BeforeEach
    public void beforeEach() {
        runDir = newTempDir();
    }

    @AfterEach
    public void afterEach() {
        JkUtilsPath.deleteIfExistsSafely(runDir);
    }

    @Test
    void helpAndVersion_ok() {
        jeka("--version");
        jeka("--help");
    }

    @Test
    void scaffoldBase_script_ok() {
        jeka("base: scaffold scaffold.jekaVersion=NO");
        jeka("hello base: info -v -Djeka.java.version=17");
    }

    @Test
    void scaffoldBase_app_ok() {
        jeka("base: scaffold scaffold.kind=APP scaffold.jekaVersion=NO -vi");
        jeka("base: test runMain");
    }

    @Test
    void scaffoldProject_ok() {
        jeka("project: scaffold scaffold.kind=REGULAR scaffold.jekaVersion=NO");
        jeka(": --help");
        jeka("runJar");
    }

    @Test
    void scaffoldProject_layoutSimple_ok() {
        jeka("project: scaffold.kind=REGULAR scaffold.jekaVersion=NO layout.style=SIMPLE scaffold");
        jeka("test pack --verbose");
        Assertions.assertTrue(Files.exists(runDir.resolve(JkProject.DEPENDENCIES_TXT_FILE)),
                "dependencies.txt has not been generated");
    }

    @Test
    void scaffoldProject_plugin_ok() {
        jeka("project: scaffold scaffold.kind=PLUGIN scaffold.jekaVersion=NO");
        jeka("test pack");
    }

    @Test
    void scaffoldProject_eclipseAndIntellijSyncWork() {
        jeka("project: scaffold scaffold.jekaVersion=NO");
        jeka("eclipse: sync");
        jeka("intellij: sync  -D" + IntellijKBean.IML_SKIP_MODULE_XML_PROP + "=true");
        Assertions.assertTrue(Files.exists(runDir.resolve("src/main/java")),
                "No source tree has been created when scaffolding Java.");
    }

    private void jeka(String cmdLine) {
        executor.runWithDistribJekaShell(runDir, cmdLine);
    }

    private static Path newTempDir() {
        return JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
    }

}
