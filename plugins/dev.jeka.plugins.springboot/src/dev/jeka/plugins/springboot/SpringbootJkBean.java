package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.core.tool.builtins.scaffold.JkScaffolder;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldJkBean;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@JkDoc("Configure project KBean in order to produce bootable springboot jar and war files.")
public final class SpringbootJkBean extends JkBean {

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

    @JkDoc(hide = true, value = "For internal test purpose : if not null, scaffolded build class will reference this classpath for springboot plugin dependency.")
    public String scaffoldDefClasspath;

    @JkDoc("If true, download spring artifacts from Spring Maven repositories.")
    public boolean useSpringRepos = true;

    @JkDoc(hide = true, value = "")
    public final ProjectJkBean projectBean = getBean(ProjectJkBean.class).lately(this::configure);

    SpringbootJkBean() {
        getBean(ScaffoldJkBean.class).lately(this::configure);
    }

    public SpringbootJkBean setSpringbootVersion(@JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-dependencies:") String springbootVersion) {
        this.springbootVersion = springbootVersion;
        return this;
    }

    private JkSpringboot projectConfigurator() {
        return JkSpringboot.of()
                .setUseSpringRepos(this.useSpringRepos)
                .setCreateBootJar(this.createBootJar)
                .setSpringbootVersion(this.springbootVersion)
                .setCreateWarFile(this.createWarFile)
                .setCreateOriginalJar(this.createOriginalJar);
    }

    private void configure(JkProject project) {
        projectConfigurator().configure(project);
    }

    private void configure (JkScaffolder scaffolder) {
        String code = JkUtilsIO.read(SpringbootJkBean.class.getClassLoader().getResource("snippet/Build.java"));
        String defClasspath = scaffoldDefClasspath != null ? scaffoldDefClasspath.replace("\\", "/") : "dev.jeka:springboot-plugin";
        code = code.replace("${dependencyDescription}", defClasspath);
        code = code.replace("${springbootVersion}", springbootVersion);
        final String jkClassCode = code;
        if (this.projectBean.scaffold.template != ProjectJkBean.JkScaffoldOptions.Template.CODE_LESS) {
            scaffolder.setJekaClassCodeProvider(() -> jkClassCode);
        }
        scaffolder.extraActions.append(this::scaffoldSample);
        String readmeContent = JkUtilsIO.read(SpringbootJkBean.class.getClassLoader().getResource("snippet/README.md"));
        scaffolder.extraActions.append(() -> {
            JkPathFile readmeFile = JkPathFile.of(getBaseDir().resolve("README.md")).createIfNotExist();
            readmeFile.write(readmeContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        });
    }

    @JkDoc("Scaffold a basic example application in package org.example")
    public void scaffoldSample() {
        String basePackage = "your/basepackage";
        Path sourceDir = projectBean.getProject().compilation.layout
                .getSources().getRootDirsOrZipFiles().get(0);
        Path pack = sourceDir.resolve(basePackage);
        URL url = SpringbootJkBean.class.getClassLoader().getResource("snippet/Application.java");
        JkPathFile.of(pack.resolve("Application.java")).createIfNotExist().fetchContentFrom(url);
        url = SpringbootJkBean.class.getClassLoader().getResource("snippet/Controller.java");
        JkPathFile.of(pack.resolve("Controller.java")).createIfNotExist().fetchContentFrom(url);
        Path testSourceDir = projectBean.getProject().testing.compilation.layout
                .getSources().getRootDirsOrZipFiles().get(0);
        pack = testSourceDir.resolve(basePackage);
        url = SpringbootJkBean.class.getClassLoader().getResource("snippet/ControllerIT.java");
        JkPathFile.of(pack.resolve("ControllerIT.java")).createIfNotExist().fetchContentFrom(url);
        JkPathFile.of(projectBean.getProject().compilation.layout.getResources()
                .getRootDirsOrZipFiles().get(0).resolve("application.properties")).createIfNotExist();
    }

    @JkDoc("Provides info about this plugin configuration")
    public void info() {
        JkLog.info("Springboot version : " + springbootVersion);
        JkLog.info("Create Bootable Jar : " + this.createBootJar);
        JkLog.info("Create original Jar : " + this.createOriginalJar);
        JkLog.info("Create .war file : " + this.createWarFile);
    }

}
