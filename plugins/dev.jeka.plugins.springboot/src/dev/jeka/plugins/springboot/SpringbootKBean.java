package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.self.SelfKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;

import java.util.Optional;

@JkDoc(
        "Adapt projectKBean or SelfAppKean for Spring-Boot.\n" +
        "- Produce bootable jars\n" +
        "- Customize .war file for projectKBean\n" +
        "- Adapt scaffold\n" +
        "The project or the SelfApp is automatically configured during this KBean initialization. "
)
public final class SpringbootKBean extends KBean {

    @JkDoc("Version of Spring Boot version used to resolve dependency versions.")
    @JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-dependencies:")
    private String springbootVersion;

    @JkDoc("If true, create a bootable jar artifact.")
    private final boolean createBootJar = true;

    @JkDoc("If true, create original jar artifact for publication (jar without embedded dependencies")
    private boolean createOriginalJar;

    @JkDoc("If true, create a .war filed.")
    private boolean createWarFile;

    @JkDoc("Specific Spring repo where to download spring artifacts. Not needed if you use official release.")
    private JkSpringRepo springRepo;

    @Override
    protected void init() {

        // Customize ProjectKBean if present
        Optional<ProjectKBean> optionalProjectKBean = getRunbase().findInstanceOf(ProjectKBean.class);
        if (optionalProjectKBean.isPresent()) {
            customizeProjectKBean(optionalProjectKBean.get());

            // Otherwise, force use SelfKBean
        } else {
            customizeSelfKBean(load(SelfKBean.class));
        }

        // Configure Docker KBean to add port mapping on run
        DockerKBean dockerKBean = load(DockerKBean.class);
        dockerKBean.customize(dockerBuild -> {
            if (dockerBuild.getExposedPorts().isEmpty()) {
                dockerBuild.setExposedPorts(8080);
            }
        });

    }

    @JkDoc("Provides info about this plugin configuration")
    public void info() {
        JkLog.info("Spring-Boot version : " + springbootVersion);
        JkLog.info("Create Bootable Jar : " + this.createBootJar);
        JkLog.info("Create original Jar : " + this.createOriginalJar);
        JkLog.info("Create .war file : " + this.createWarFile);
    }

    private void customizeProjectKBean(ProjectKBean projectKBean) {

        // Customize scaffold
        projectKBean.getProjectScaffold().addCustomizer(SpringbootScaffold::adapt);

        JkSpringbootProject springbootProject = JkSpringbootProject.of(projectKBean.project)
                .configure(this.createBootJar, this.createWarFile, this.createOriginalJar);
        if (springbootVersion != null) {
            springbootProject.includeParentBom(springbootVersion);
        }
        if (springRepo != null) {
            springbootProject.addSpringRepo(springRepo);
        }
    }

    private void customizeSelfKBean(SelfKBean selfKBean) {

        // customize scaffold
        selfKBean.getSelfScaffold().addCustomizer(SpringbootScaffold::adapt);

        selfKBean.setMainClass(SelfKBean.AUTO_FIND_MAIN_CLASS);
        selfKBean.setMainClassFinder(() -> JkSpringbootJars.findMainClassName(
                getBaseDir().resolve(JkConstants.JEKA_SRC_CLASSES_DIR)));

        selfKBean.setJarMaker(path -> JkSpringbootJars.createBootJar(
                selfKBean.getAppClasses(),
                selfKBean.getAppLibs(),
                getRunbase().getDependencyResolver().getRepos(),
                path,
                selfKBean.getManifest())
        );
    }



}
