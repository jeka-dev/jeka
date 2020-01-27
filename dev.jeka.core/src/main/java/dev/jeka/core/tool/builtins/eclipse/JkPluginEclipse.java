package dev.jeka.core.tool.builtins.eclipse;


import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.eclipse.JkEclipseClasspathGenerator;
import dev.jeka.core.api.tooling.eclipse.JkEclipseProjectGenerator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@JkDoc("Generation of Eclipse files (.project and .classpath) from actual project structure andPrepending dependencies.")
@JkDocPluginDeps({JkPluginJava.class})
public final class JkPluginEclipse extends JkPlugin {

    @JkDoc("If true, .classpath will include javadoc reference for declared dependencies.")
    boolean javadoc = false;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path in .classpath." })
    public String jreContainer = null;

    /** Flag to set whether 'all' task should use absolute paths instead of classpath variables */
    @JkDoc({ "If true, dependency paths will be expressed relatively to Eclipse path variables instead of absolute paths." })
    public boolean useVarPath = true;

    private final Map<JkDependency, Properties> attributes = new HashMap<>();

    private final Map<JkDependency, Properties> accessRules = new HashMap<>();

    private final JkPluginScaffold scaffold;

    protected JkPluginEclipse(JkCommands run) {
        super(run);
        this.scaffold = run.getPlugins().get(JkPluginScaffold.class);
    }

    // ------------------------- setters ----------------------------

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    // ------------------------ plugin methods ----------------------

    @Override
    @JkDoc("Adds .classpath and .project generation to scaffolding.")
    protected void activate() {
        scaffold.getScaffolder().getExtraActions().chain(this::files);  // If this plugin is activated while scaffolding, we want Eclipse metada file be generated.
    }

    @JkDoc("Generates Eclipse files (.classpath and .project) in the current directory. The files reflect project " +
            "dependencies and source layout.")
    public void files() {
        final Path dotProject = getCommands().getBaseDir().resolve(".project");
        if (getCommands().getPlugins().hasLoaded(JkPluginJava.class)) {
            final JkJavaProject javaProject = getCommands().getPlugins().get(JkPluginJava.class).getProject();
            final List<Path> importedRunProjects = new LinkedList<>();
            for (final JkCommands depRun : getCommands().getImportedCommands().getDirects()) {
                importedRunProjects.add(depRun.getBaseTree().getRoot());
            }
            final JkEclipseClasspathGenerator classpathGenerator =
                    JkEclipseClasspathGenerator.of(javaProject.getJavaProjectIde());
            classpathGenerator.setRunDependencies(getCommands().getRunDependencyResolver(),
                    getCommands().getDefDependencies());
            classpathGenerator.setIncludeJavadoc(this.javadoc);
            classpathGenerator.setJreContainer(this.jreContainer);
            classpathGenerator.setImportedProjects(importedRunProjects);
            classpathGenerator.setUsePathVariables(this.useVarPath);
            this.attributes.entrySet().forEach(entry -> {
                classpathGenerator.addAttributes(entry.getKey(), entry.getValue());
            });
            this.accessRules.entrySet().forEach(entry -> {
                classpathGenerator.addAccessRules(entry.getKey(), entry.getValue());
            });
            final String result = classpathGenerator.generate();
            final Path dotClasspath = getCommands().getBaseDir().resolve(".classpath");
            JkUtilsPath.write(dotClasspath, result.getBytes(Charset.forName("UTF-8")));
            JkLog.info("File " + dotClasspath + " generated.");

            if (!Files.exists(dotProject)) {
                JkEclipseProjectGenerator.ofJavaNature(getCommands().getBaseTree().getRoot().getFileName().toString())
                        .writeTo(dotProject);
                JkLog.info("File " + dotProject + " generated.");
            }
        } else {
            if (!Files.exists(dotProject)) {
                JkEclipseProjectGenerator.ofSimpleNature(getCommands().getBaseTree().getRoot().getFileName().toString())
                        .writeTo(dotProject);
                JkLog.info("File " + dotProject + " generated.");
            }
        }
    }

    @JkDoc("Generates Eclipse files (.project and .classpath) on all sub-folders of the current directory. Only sub-folders having a jeka/def directory are taken in account. See eclipse#files.")
    public void all() {
        final Iterable<Path> folders = getCommands().getBaseTree()
                .andMatching(true,"**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andMatching(false,"**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startTask("Generating Eclipse files on " + projectFolder);
            Main.exec(projectFolder, "eclipse#files");
            JkLog.endTask();
        }
    }


    // -------------------------- Setters

    /**
     * For the specified dependency, specify a child attribute tag to add to the mapping classpathentry tag.
     * @param dependency The dependency paired to the classpathentry we want generate `<attributes></attributes>` children
     *                   for. It can be a {@link dev.jeka.core.api.depmanagement.JkModuleDependency} or a
     *                   {@link dev.jeka.core.api.depmanagement.JkFileSystemDependency}.
     *                   If it is a module dependency, it can be a direct or transitive dependency and only group:name
     *                   is relevant.
     */
    public JkPluginEclipse addAttribute(JkDependency dependency, String name, String value) {
        this.attributes.putIfAbsent(dependency, new Properties());
        this.attributes.get(dependency).put(name, value);
        return this;
    }

    /**
     * For the specified dependency, specify a child accessrule tag to add to the mapping classpathentry tag.
     * @param dependency The dependency paired to the classpathentry we want generate `<attributes></attributes>` children
     *                   for. It can be a {@link dev.jeka.core.api.depmanagement.JkModuleDependency} or a
     *                   {@link dev.jeka.core.api.depmanagement.JkFileSystemDependency}.
     *                   If it is a module dependency, it can be a direct or transitive dependency and only group:name
     *                   is relevant.
     */
    public JkPluginEclipse addAccessRule(JkDependency dependency, String kind, String pattern) {
        this.accessRules.putIfAbsent(dependency, new Properties());
        this.accessRules.get(dependency).put(kind, pattern);
        return this;
    }



}
