package dev.jeka.plugins.springboot;

import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.self.JkSelfScaffold;

import java.nio.file.Paths;
import java.util.List;

class SpringbootScaffold {


    public static void adapt(JkProjectScaffold projectScaffold) {

        // Remove the default build class defined for project
        projectScaffold.removeFileEntry(JkProjectScaffold.BUILD_CLASS_PATH);

        String lastSpringbootVersion = projectScaffold.findLatestVersion(
                JkSpringModules.Boot.STARTER_PARENT.toColonNotation(),
                JkSpringbootJars.DEFAULT_SPRINGBOOT_VERSION);

        if (projectScaffold.getTemplate() == JkProjectScaffold.Template.BUILD_CLASS) {
            String code = readSnippet("Build.java");

            // For testability purpose
            String overriddenPluginDep = System.getProperty(JkSpringbootProject
                    .OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME);
            String injectClasspath = overriddenPluginDep != null ?
                    overriddenPluginDep.replace("\\", "/") : "dev.jeka:springboot-plugin";

            // Add customized build class
            code = code.replace("${dependencyDescription}", injectClasspath);
            code = code.replace("${springbootVersion}", lastSpringbootVersion);
            projectScaffold.addFileEntry(JkProjectScaffold.BUILD_CLASS_PATH, code);

        } else if (projectScaffold.getTemplate() == JkProjectScaffold.Template.PROPS) {

            // Augment jeka.properties
            projectScaffold.addJekaPropValue(JkConstants.CLASSPATH_INJECT_PROP + "=dev.jeka:springboot-plugin");
            projectScaffold.addJekaPropValue("");
            projectScaffold.addJekaPropValue("springboot#springbootVersion=" + lastSpringbootVersion);

            // Add dependencies
            projectScaffold.compileDeps.add("org.springframework.boot:spring-boot-starter-web");
            projectScaffold.testDeps.add("org.springframework.boot:spring-boot-starter-test");
        }

        // Add sample code

        String basePackage = "app";

        // -- src
        String pack = projectScaffold.getSrcRelPath() + "/" + basePackage;
        projectScaffold.addFileEntry(pack + "/Application.java", readSnippet("Application.java"));
        projectScaffold.addFileEntry(pack + "/Controller.java", readSnippet("Controller.java"));

        // -- test
        pack = projectScaffold.getTestRelPath() + "/" + basePackage;
        projectScaffold.addFileEntry(pack + "/ControllerIT.java", readSnippet("ControllerIT.java"));
    }

    static void adapt(JkSelfScaffold selfScaffold) {

        // Remove build class defined by default
        selfScaffold.removeFileEntriesStaringBy(Paths.get(JkConstants.JEKA_SRC_DIR));

        String lastSpringbootVersion = selfScaffold.findLatestVersion(
                JkSpringModules.Boot.STARTER_PARENT.toColonNotation(),
                JkSpringbootJars.DEFAULT_SPRINGBOOT_VERSION);

        List<String> deps = JkUtilsIterable.listOf(
                "org.springframework.boot:spring-boot-dependencies::pom:" + lastSpringbootVersion,
                "org.springframework.boot:spring-boot-starter-web");

        List<String> devDeps = JkUtilsIterable.listOf("org.springframework.boot:spring-boot-starter-test");

        // Build class code
        String buildClassCode = readSnippet("SelfBuild.java");

        // -- For testability purpose
        String overriddenPluginDep = System.getProperty(JkSpringbootProject
                .OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME);
        String injectClasspath = overriddenPluginDep != null ?
                overriddenPluginDep.replace("\\", "/") : "dev.jeka:springboot-plugin";
        buildClassCode = buildClassCode.replace("${springboot-plugin}", injectClasspath);

        buildClassCode = buildClassCode.replace("${inject}", JkSelfScaffold.toJkInject(devDeps));
        selfScaffold.addFileEntry(JkSelfScaffold.BUILD_CLASS_PATH, buildClassCode);

        // Test code
        //selfScaffold.addFileEntry(JkConstants.JEKA_SRC_DIR + "/_dev/test/BaseIT.java", readSnippet("SelfTest.java"));
        selfScaffold.addFileEntry(JkConstants.JEKA_SRC_DIR + "/_dev/test/ControllerIT.java", readSnippet("SelfControllerIT.java"));

        // App code
        String appCode = readSnippet("SelfApplication.java");
        appCode = appCode.replace("${inject}", JkSelfScaffold.toJkInject(deps));
        selfScaffold.addFileEntry(JkSelfScaffold.APP_CLASS_PATH, appCode);
        selfScaffold.addFileEntry(JkConstants.JEKA_SRC_DIR + "/app/Controller.java", readSnippet("Controller.java"));

    }

    private static String readSnippet(String name) {
        return JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + name));
    }
}