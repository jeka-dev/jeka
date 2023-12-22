import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.plugins.springboot.JkSpringbootProject;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends KBean implements JkIdeSupportSupplier {

    @JkDoc("Clean output directory then compile, test and create jar")
    public void cleanPack() {
        project().clean().pack();
    }

    @JkDoc("Runs the built bootable jar")
    public void runJar() {
        project().runMainJar(false, "", "");
    }

    @JkDoc("Display project info on console")
    public void info() {
        System.out.println(project().getInfo());
    }

    @JkDoc("Display dependency tree on console")
    public void depTree() {
        project().displayDependencyTree();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }

    private JkProject project() {
        JkProject project = JkProject.of();
        JkSpringbootProject.of(project)
                .setSpringbootVersion("${springbootVersion}")
                .configure();
        project.flatFacade()
                .addCompileDeps(
                        "org.springframework.boot:spring-boot-starter-web"
                )
                .addCompileOnlyDeps(
                        "org.projectlombok:lombok:1.18.30"
                )
                .addTestDeps(
                        "org.springframework.boot:spring-boot-starter-test"
                )
                .setPublishedVersionFromGitTag();  // Infer version from Git
        return project;
    }

}
