package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldKBean;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;

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
        getRuntime().find(ProjectKBean.class).ifPresent(projectKBean ->
                JkSpringbootProject.of(projectKBean.project).scaffoldSample());
    }

    @Override
    protected void init() {

        // Spring-Boot KBean is intended to enhance either ProjectKBean nor SelfAppKBean.
        // If none is present yet in the runtime, we assume that ProjectKBean should be instantiated implicitly
        Optional<SelfAppKBean> optionalSelfAppKBean = getRuntime().findInstanceOf(SelfAppKBean.class);
        if (!optionalSelfAppKBean.isPresent()) {
            JkLog.trace("No SelfAppKBean found in runtime. Assume SpringbootKBean is for configuring JkProject.");
            load(ProjectKBean.class);
        } else {
            JkLog.trace("SelfAppKBean found in runtime. Assume SpringbootKBean is for configuring SelfApp. ");
            SelfAppKBean selfApp = optionalSelfAppKBean.get();
            selfApp.setJarMaker(path -> JkSpringbootJars.createBootJar(
                    selfApp.classTree(), selfApp.libs(), getRuntime().getDependencyResolver().getRepos(), path)
            );
            selfApp.dockerBuildCustomizers.add(dockerBuild -> dockerBuild.setExposedPorts(8080));
            selfApp.dockerRunParams = "-p 8080:8080";
        }

        Optional<ProjectKBean> optionalProjectKBean = getRuntime().find(ProjectKBean.class);
        optionalProjectKBean.ifPresent(projectKBean ->
                configure(projectKBean.project));
        getRuntime().find(ScaffoldKBean.class).ifPresent(scaffoldKBean ->
                configureScaffold(scaffoldKBean.scaffold));
    }

    @JkDoc("Provides info about this plugin configuration")
    public void info() {
        JkLog.info("Spring-Boot version : " + springbootVersion);
        JkLog.info("Create Bootable Jar : " + this.createBootJar);
        JkLog.info("Create original Jar : " + this.createOriginalJar);
        JkLog.info("Create .war file : " + this.createWarFile);
    }

    private void configureScaffold(JkScaffold scaffold) {
        getRuntime().find(ProjectKBean.class).ifPresent(projectKBean ->
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
