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

package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.base.JkBaseScaffold;

import java.nio.file.Paths;
import java.util.List;

class SpringbootScaffold {

    @JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-dependencies:")
    private static final String DEFAULT_SPRINGBOOT_VERSION = "3.5.0";

    public static void customize(JkProjectScaffold projectScaffold) {

        projectScaffold.setKind(JkProjectScaffold.Kind.EMPTY);

        // Add springboot-plugin to jeka.classpath
        projectScaffold.addJekaPropValue(JkConstants.CLASSPATH_PROP + "=dev.jeka:springboot-plugin");
        projectScaffold.addJekaPropValue(JkConstants.KBEAN_DEFAULT_PROP + "=project");
        projectScaffold.addJekaPropsContent("\n");
        projectScaffold.addJekaPropValue("@custom=on");
        projectScaffold.addJekaPropValue("@springboot=on");
        projectScaffold.addCustomKbeanFileEntry();

        String lastSpringbootVersion = projectScaffold.findLatestStableVersion(
                JkSpringModules.Boot.STARTER_PARENT.toColonNotation(),
                DEFAULT_SPRINGBOOT_VERSION);

        // For testability purpose
        String overriddenPluginDep = System.getProperty(JkSpringbootProject
                .OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME);
        if (overriddenPluginDep != null) {
            String pluginDep = overriddenPluginDep.replace("\\", "/");
            projectScaffold.setJekaPropsCustomizer(content -> content.replace("dev.jeka:springboot-plugin", pluginDep));
        }

        // Add dependencies
        projectScaffold.compileDeps.add("org.springframework.boot:spring-boot-starter-web");
        projectScaffold.testDeps.add("org.springframework.boot:spring-boot-starter-test");
        projectScaffold.versionDeps.add(JkSpringbootProject.BOM_COORDINATE + lastSpringbootVersion + "@pom");

        // Add sample code
        String basePackage = "app";

        // -- src
        String pack = projectScaffold.getSrcRelPath() + "/" + basePackage;
        projectScaffold.addFileEntry(pack + "/Application.java", readSnippet("Application.java"));
        projectScaffold.addFileEntry(pack + "/Controller.java", readSnippet("Controller.java"));

        // -- resources
        projectScaffold.addFileEntry(projectScaffold.getResRelPath() + "/application.properties", "");

        // -- test
        pack = projectScaffold.getTestRelPath() + "/" + basePackage;
        projectScaffold.addFileEntry(pack + "/ControllerIT.java", readSnippet("ControllerIT.java"));
    }

    static void customize(JkBaseScaffold baseScaffold) {

        baseScaffold.baseScaffoldOption.kind = null; // to generate README.MD

        // Remove the build class defined by default
        baseScaffold.removeFileEntriesStartingWith(Paths.get(JkConstants.JEKA_SRC_DIR));

        String lastSpringbootVersion = baseScaffold.findLatestStableVersion(
                JkSpringModules.Boot.STARTER_PARENT.toColonNotation(),
                DEFAULT_SPRINGBOOT_VERSION);

        List<String> deps = JkUtilsIterable.listOf(
                JkSpringbootProject.BOM_COORDINATE + lastSpringbootVersion + "@pom",
                "org.springframework.boot:spring-boot-starter-web");

        List<String> devDeps = JkUtilsIterable.listOf(
                "org.springframework.boot:spring-boot-starter-test");

        // Build class code
        String buildClassCode = readSnippet("BaseBuild.java");

        // -- For testability purpose
        String overriddenPluginDep = System.getProperty(JkSpringbootProject
                .OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME);
        String injectClasspath = overriddenPluginDep != null ?
                overriddenPluginDep.replace("\\", "/") : "dev.jeka:springboot-plugin";
        buildClassCode = buildClassCode.replace("${springboot-plugin}", injectClasspath);

        buildClassCode = buildClassCode.replace("${inject}", JkBaseScaffold.toJkInject(devDeps));
        baseScaffold.addFileEntry(JkBaseScaffold.BUILD_CLASS_PATH, buildClassCode);

        // Test code
        baseScaffold.addFileEntry(JkConstants.JEKA_SRC_DIR + "/_dev/test/ControllerIT.java", readSnippet("BaseControllerIT.java"));

        // App code
        String appCode = readSnippet("BaseApplication.java");
        appCode = appCode.replace("${inject}", JkBaseScaffold.toJkInject(deps));
        baseScaffold.addFileEntry(JkBaseScaffold.APP_CLASS_PATH, appCode);
        baseScaffold.addFileEntry(JkConstants.JEKA_SRC_DIR + "/app/Controller.java", readSnippet("Controller.java"));

    }

    private static String readSnippet(String name) {
        return JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + name));
    }
}
