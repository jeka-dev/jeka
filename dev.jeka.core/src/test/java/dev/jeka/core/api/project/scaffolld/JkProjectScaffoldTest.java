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

package dev.jeka.core.api.project.scaffolld;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JkProjectScaffoldTest {


    @Test
    public void scaffold_regular_ok()  {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        JkProjectScaffold projectScaffold = JkProjectScaffold.of(project);
        projectScaffold.compileDeps.add("toto:titi:0.0.1");
        projectScaffold.runtimeDeps.add("foo:bar");
        projectScaffold
                .setTemplate(JkProjectScaffold.Kind.REGULAR)
                .run();

        // check .gitIgnore
        if (System.getenv("GITHUB_WORKFLOW") == null) {  // may fail on github action
            String gitIgnoreContent = JkPathFile.of(baseDir.resolve(".gitIgnore")).readAsString();
            assertTrue(gitIgnoreContent.contains("/.jeka-work"));
            assertTrue(gitIgnoreContent.contains("/jeka-output"));
        }

        // Check Build class is present
        assertTrue(Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR).resolve("Build.java")));

        // Check project layout
        assertTrue(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/main/resources")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/test/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/test/resources")));

        // cleanup
        // Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffold_withSimpleLayout_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        project.flatFacade.setLayoutStyle(JkCompileLayout.Style.SIMPLE);

        JkProjectScaffold.of(project).setUseSimpleStyle(true).run();

        // Check project layout
        assertFalse(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src")));
        assertTrue(Files.isDirectory(baseDir.resolve("test")));
        assertTrue(Files.isDirectory(baseDir.resolve("res")));

        // Check property is here
        String jekaContent = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE)).readAsString();
        assertTrue(jekaContent.contains("@project.layout.style=SIMPLE"));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

}
