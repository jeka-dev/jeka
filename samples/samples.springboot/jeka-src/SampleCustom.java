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

import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.plugins.springboot.JkSpringbootProject;
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkDep("../../plugins/plugins.springboot/jeka-output/dev.jeka.springboot-plugin.jar")
//@JkDep("dev.jeka:springboot-plugin")
public class SampleCustom extends KBean {

    public String aa;

    @JkInject
    ProjectKBean projectKBean;

    @JkInject
    SpringbootKBean springbootKBean;

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.springboot-plugin.jar", "plugins.springboot")
                .setModuleAttributes("plugins.springboot", JkIml.Scope.COMPILE, null)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIml.Scope.COMPILE, null)
                .setSuggestedJdk("17");
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.compilation.dependencies
                .add("org.springframework.boot:spring-boot-starter-web")
                .add("org.springframework.boot:spring-boot-starter-data-jpa")
                .add("com.h2database:h2");

        projectKBean.project.testing.compilation.dependencies
                .add("org.springframework.boot:spring-boot-starter-test");

        projectKBean.project
                .setModuleId("dev.jeka:samples-springboot")
                .setVersion("0.0.1-SNAPSHOT");  // Snapshot is necessary otherwise it can not deploy twice in maven local repo
        JkSpringbootProject.of(projectKBean.project)
                .configure()
                .includeParentBom("3.3.4");
    }

    public void cleanPack() {
        projectKBean.clean();
        projectKBean.pack();
    }

    public void testRun() {
        System.out.println(this.aa);
        cleanPack();
        springbootKBean.load(ProjectKBean.class).runJar();
    }

    public static void main(String[] args) {
        JkInit.kbean(SampleCustom.class, args).projectKBean.info();
    }

}
