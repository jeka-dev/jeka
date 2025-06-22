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

package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocUrl;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.util.Optional;

@JkDoc("Add Kotlin compilation capability to `project` KBean.")
@JkDocUrl("https://github.com/jeka-dev/jeka/tree/master/plugins/dev.jeka.plugins.kotlin")
public class KotlinJvmKBean extends KBean {

    private static final String DEFAULT_VERSION = "2.1.21";

    @JkDoc("Overrides the Kotlin version for compiling and running defined in 'jeka.kotlin.version' property.")
    private String kotlinVersion;

    @JkDoc("Location of Kotlin sources")
    private String sourceDir = "src/main/kotlin";

    @JkDoc("Location of Kotlin sources for tests")
    private String testSourceDir = "src/test/kotlin";

    @JkDoc("If true, includes standard lib for compiling")
    private boolean includeStdlib = true;

    @JkDoc("If true, the project KBean will be automatically configured to use Kotlin.")
    private boolean configureProject = true;

    private JkKotlinJvm kotlinJvmProject;

    @JkDoc("Adds Kotlin source compilation and Kotlin standard library to dependencies")
    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        if (configureProject) {
            getKotlinJvm().configure(projectKBean.project, sourceDir, testSourceDir);
        }
    }

    /**
     * Retrieves the Kotlin JVM project. If the project has already been created, it is returned.
     * Otherwise, it creates a new Kotlin JVM project using the projectKBean found in runbase..
     * If no projectKean is found in the runbase, an IllegalStateException is thrown.
     *
     * @throws IllegalStateException if no projectKean is found in the runbase
     */
    public JkKotlinJvm getKotlinJvm() {
        if (kotlinJvmProject != null) {
            return kotlinJvmProject;
        }
        JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(getRunbase().getDependencyResolver().getRepos(),
                getKotlinVersion());
        kotlinJvmProject = JkKotlinJvm.of(kotlinCompiler).setAddStdlib(this.includeStdlib);
        return kotlinJvmProject;
    }

    private String getKotlinVersion() {
        String result = kotlinVersion;
        if (result == null) {
            result = Optional.ofNullable(getRunbase().getProperties()
                    .get(JkKotlinCompiler.KOTLIN_VERSION_OPTION)).orElse(DEFAULT_VERSION);
        }
        return result;
    }

}