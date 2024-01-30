package dev.jeka.plugins.springboot;

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.self.JkSelfScaffold;
import dev.jeka.core.tool.builtins.self.SelfKBean;

import java.nio.file.Path;
import java.util.List;

class SpringbootSelfScaffold extends JkSelfScaffold {

    public SpringbootSelfScaffold(SelfKBean selfKBean) {
        super(selfKBean);
    }

    SpringbootSelfScaffold(Path baseDir, SelfKBean.SelfScaffoldOptions options) {
        super(baseDir, options);
    }

    @Override
    protected void configureScaffold() {

        String lastSpringbootVersion = findLatestVersion(
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

        buildClassCode = buildClassCode.replace("${inject}", toJkInject(devDeps));
        addFileEntry(BUILD_CLASS_PATH, buildClassCode);

        // Test code
        addFileEntry(JkConstants.JEKA_SRC_DIR + "/_dev/test/BaseIT.java", readSnippet("SelfTest.java"));
        addFileEntry(JkConstants.JEKA_SRC_DIR + "/_dev/test/ControllerIT.java", readSnippet("ControllerIT.java"));

        // App code
        String appCode = readSnippet("SelfApplication.java");
        appCode = appCode.replace("${inject}", toJkInject(deps));
        addFileEntry(APP_CLASS_PATH, appCode);
        addFileEntry(JkConstants.JEKA_SRC_DIR + "/app/Controller.java", readSnippet("Controller.java"));

    }

    private static String readSnippet(String name) {
        return JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + name));
    }
}
