package dev.jeka.plugins.springboot;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkConstants;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class SpringbootProjectScaffold extends JkProjectScaffold {


    public SpringbootProjectScaffold(JkProject project) {
        super(project);
    }

    @Override
    protected void configureScaffold() {
        super.configureScaffold();

        // Remove the default build class defined for project
        removeFileEntry(JkProjectScaffold.BUILD_CLASS_PATH);

        String springbootLastVersion = JkSpringbootJars.findLatestSpringbootVersion(getDownloadRepos());

        if (getTemplate() == Template.BUILD_CLASS) {
            String code = readSnippet("Build.java");

            // For testability purpose
            String overriddenPluginDep = System.getProperty(JkSpringbootProject.OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME);
            String injectClasspath = overriddenPluginDep != null ?
                    overriddenPluginDep.replace("\\", "/") : "dev.jeka:springboot-plugin";

            // Add customized build class
            code = code.replace("${dependencyDescription}", injectClasspath);
            code = code.replace("${springbootVersion}", springbootLastVersion);
            addFileEntry(JkProjectScaffold.BUILD_CLASS_PATH, code);

        } else if (getTemplate() == Template.PROPS) {

            // Augment jeka.properties
            addJekaPropValue(JkConstants.CLASSPATH_INJECT_PROP + "=dev.jeka:springboot-plugin");
            addJekaPropValue("");
            addJekaPropValue("springboot#springbootVersion=" + springbootLastVersion);

            // Add dependencies
            compileDeps.add("org.springframework.boot:spring-boot-starter-web");
            testDeps.add("org.springframework.boot:spring-boot-starter-test");
        }

        // Add sample code

        String basePackage = "app";

        // -- src
        Path sourceDir = project.compilation.layout
                .getSources().getRootDirsOrZipFiles().get(0);
        String pack = sourceDir.resolve(basePackage).toString();
        addFileEntry(pack + "/Application.java", readSnippet("Application.java"));
        addFileEntry(pack + "/Controller.java", readSnippet("Controller.java"));

        // -- test
        Path testDir = project.testing.compilation.layout.getSources().getRootDirsOrZipFiles().get(0);
        pack = testDir.resolve(basePackage).toString();
        addFileEntry(pack + "/ControllerIT.java", readSnippet("ControllerIT.java"));
    }

    private static String readSnippet(String name) {
        return JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + name));
    }
}
