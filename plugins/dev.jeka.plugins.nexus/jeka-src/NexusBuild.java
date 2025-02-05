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

import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class NexusBuild extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade
                .setModuleId("dev.jeka:nexus-plugin")
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .dependencies.compile.add(JkLocator.getJekaJarPath());
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(mavenPublication -> mavenPublication
                .pomMetadata
                    .setProjectName("Jeka plugin for Jacoco")
                    .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr"));
    }

}