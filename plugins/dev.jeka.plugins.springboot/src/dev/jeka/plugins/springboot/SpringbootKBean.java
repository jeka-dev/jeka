package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.scaffold.JkScaffold;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldKBean;

import java.util.Optional;

@JkDoc(
        "Adapt projectKBean or SelfAppKean for Spring-Boot.\n" +
        "- Produce bootable jars\n" +
        "- Customize .war file for projectKBean\n" +
        "- Adapt scaffold\n" +
        "The project or the SelfApp is automatically configured during this KBean initialization. "
)
public final class SpringbootKBean extends KBean {

    private static final String DEFAULT_SPRINGBOOT_VERSION = "3.2.0";



    @JkDoc("Version of Spring Boot version used to resolve dependency versions.")
    @JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-dependencies:")
    public String springbootVersion = DEFAULT_SPRINGBOOT_VERSION;

    @JkDoc("If true, create a bootable jar artifact.")
    public boolean createBootJar = true;

    @JkDoc("If true, create original jar artifact for publication (jar without embedded dependencies")
    public boolean createOriginalJar;

    @JkDoc("If true, create a .war filed.")
    public boolean createWarFile;

    @JkDoc("If true, download spring artifacts from Spring Maven repositories.")
    public boolean useSpringRepos = true;

    @JkDoc(hide = true, value = "For internal test purpose : if not null, scaffolded build class will reference this classpath for <i>dev.jeka:springboot-plugin</i> dependency.")
    public String scaffoldDefClasspath;

    @JkDoc("Kind of build class to be scaffolded")
    public JkSpringbootProject.ScaffoldBuildKind scaffoldBuildKind = JkSpringbootProject.ScaffoldBuildKind.KBEAN;



    @Override
    protected void init() {
        Optional<ProjectKBean> optionalProjectKBean = getRuntime().getOptionalKBean(ProjectKBean.class);
        optionalProjectKBean.ifPresent(projectKBean ->
                configure(projectKBean.project));
        getRuntime().getOptionalKBean(ScaffoldKBean.class).ifPresent(scaffoldKBean ->
                configureScaffold(scaffoldKBean.scaffold));
    }

    @JkDoc("Scaffold a basic example application in package org.example")
    public void scaffoldSample() {
        if (getRuntime().getOptionalKBean(ProjectKBean.class).ifPresent(projectKBean -> {
            JkSpringbootProject.of(projectKBean.project).scaffoldSample();
        });
    }

    @JkDoc("Provides info about this plugin configuration")
    public void info() {
        JkLog.info("Springboot version : " + springbootVersion);
        JkLog.info("Create Bootable Jar : " + this.createBootJar);
        JkLog.info("Create original Jar : " + this.createOriginalJar);
        JkLog.info("Create .war file : " + this.createWarFile);
    }

    private void configureScaffold(JkScaffold scaffold) {
        if (getRuntime().getOptionalKBean(ProjectKBean.class).ifPresent(projectKBean -> {
            JkSpringbootProject.of(projectKBean.project)
                    .configureScaffold(
                            scaffold,
                            scaffoldBuildKind,
                            scaffoldDefClasspath,
                            projectKBean.scaffold.template);
        });
    }

    private void configure(JkProject project) {
        JkSpringbootProject.of(project)
                .setUseSpringRepos(this.useSpringRepos)
                .setCreateBootJar(this.createBootJar)
                .setSpringbootVersion(this.springbootVersion)
                .setCreateWarFile(this.createWarFile)
                .setCreateOriginalJar(this.createOriginalJar)
                .configure();
    }

}
