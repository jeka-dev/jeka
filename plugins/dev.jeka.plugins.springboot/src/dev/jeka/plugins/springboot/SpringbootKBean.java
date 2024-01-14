package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldKBean;
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

    @JkDoc(hide = true, value = "For internal test purpose : if not null, scaffolded build class will reference this classpath for <i>dev.jeka:springboot-plugin</i> dependency.")
    private String scaffoldDefClasspath;

    @JkDoc("Kind of build class to be scaffolded")
    private final JkSpringbootProject.ScaffoldBuildKind scaffoldKind = JkSpringbootProject.ScaffoldBuildKind.LIB;

    @JkDoc("Scaffold a basic example application in package org.example")
    public void scaffoldSample() {
        getRunbase().find(ProjectKBean.class).ifPresent(projectKBean ->
                JkSpringbootProject.of(projectKBean.project).scaffoldSample());
    }

    @Override
    protected void init() {

        // Spring-Boot KBean is intended to enhance either ProjectKBean nor SelfKBean.
        // If none is present yet in the runbase, we assume that ProjectKBean should be instantiated implicitly
        Optional<SelfKBean> optionalSelfAppKBean = getRunbase().findInstanceOf(SelfKBean.class);

        if (!optionalSelfAppKBean.isPresent()) {
            JkLog.trace("No SelfKBean found in runbase. Assume SpringbootKBean is for configuring Project.");
            load(ProjectKBean.class);
        } else {
            JkLog.trace("SelfKBean found in runbase. Assume SpringbootKBean is for configuring SelfApp. ");
            SelfKBean selfApp = optionalSelfAppKBean.get();

            selfApp.setMainClass(SelfKBean.AUTO_FIND_MAIN_CLASS);
            selfApp.setMainClassFinder(() -> JkSpringbootJars.findMainClassName(
                    getBaseDir().resolve(JkConstants.DEF_BIN_DIR)));

            selfApp.setJarMaker(path -> JkSpringbootJars.createBootJar(
                    selfApp.getAppClasses(),
                    selfApp.getAppLibs(),
                    getRunbase().getDependencyResolver().getRepos(),
                    path,
                    selfApp.getManifest())
            );
        }

        // Configure KBean Project if no selfApp KBean is present.
        if (!optionalSelfAppKBean.isPresent()) {
            configure(load(ProjectKBean.class).project);
        }

        // Configure Scaffold KBean
        getRunbase().find(ScaffoldKBean.class).ifPresent(scaffoldKBean ->
                configureScaffold(scaffoldKBean.scaffold)
        );

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

    private void configureScaffold(JkScaffold scaffold) {
        getRunbase().find(ProjectKBean.class).ifPresent(projectKBean ->
            JkSpringbootProject.of(projectKBean.project)
                    .configureScaffold(
                            scaffold,
                            scaffoldKind,
                            scaffoldDefClasspath,
                            projectKBean.scaffold.getTemplate()));
    }

    private void configure(JkProject project) {
        JkSpringbootProject springbootProject = JkSpringbootProject.of(project)
                .configure(this.createBootJar, this.createWarFile, this.createOriginalJar);
        if (springbootVersion != null) {
            springbootProject.includeParentBom(springbootVersion);
        }
        if (springRepo != null) {
            springbootProject.addSpringRepo(springRepo);
        }
    }

}
