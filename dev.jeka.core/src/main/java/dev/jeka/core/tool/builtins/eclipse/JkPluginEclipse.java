package dev.jeka.core.tool.builtins.eclipse;


import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.java.project.JkJavaIdeSupport;
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

    protected JkPluginEclipse(JkCommandSet run) {
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
    protected void afterSetup() {
        scaffold.getScaffolder().getExtraActions().append(this::files);  // If this plugin is activated while scaffolding, we want Eclipse metada file be generated.
    }

    @JkDoc("Generates Eclipse files (.classpath and .project) in the current directory. The files reflect project " +
            "dependencies and source layout.")
    public void files() {
        final Path dotProject = getCommandSet().getBaseDir().resolve(".project");
        JkJavaIdeSupport projectIde = getProjectIde(getCommandSet());
        if (projectIde != null) {
            final List<Path> importedRunProjects = new LinkedList<>();
            for (final JkCommandSet depRun : getCommandSet().getImportedCommandSets().getDirects()) {
                importedRunProjects.add(depRun.getBaseTree().getRoot());
            }
            final JkEclipseClasspathGenerator classpathGenerator =
                    JkEclipseClasspathGenerator.of(projectIde);
            classpathGenerator.setDefDependencies(getCommandSet().getDefDependencyResolver(),
                    getCommandSet().getDefDependencies());
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
            final Path dotClasspath = getCommandSet().getBaseDir().resolve(".classpath");
            JkUtilsPath.write(dotClasspath, result.getBytes(Charset.forName("UTF-8")));
            JkLog.info("File " + dotClasspath + " generated.");

            if (!Files.exists(dotProject)) {
                JkEclipseProjectGenerator.ofJavaNature(getCommandSet().getBaseTree().getRoot().getFileName().toString())
                        .writeTo(dotProject);
                JkLog.info("File " + dotProject + " generated.");
            }
        } else {
            if (!Files.exists(dotProject)) {
                JkEclipseProjectGenerator.ofSimpleNature(getCommandSet().getBaseTree().getRoot().getFileName().toString())
                        .writeTo(dotProject);
                JkLog.info("File " + dotProject + " generated.");
            }
        }
    }

    @JkDoc("Generates Eclipse files (.project and .classpath) on all sub-folders of the current directory. Only sub-folders having a jeka/def directory are taken in account. See eclipse#files.")
    public void all() {
        final Iterable<Path> folders = getCommandSet().getBaseTree()
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

    public static JkJavaIdeSupport getProjectIde(JkCommandSet commands) {
        if (commands instanceof JkJavaIdeSupport.JkSupplier) {
            JkJavaIdeSupport.JkSupplier supplier = (JkJavaIdeSupport.JkSupplier) commands;
            return supplier.getJavaIdeSupport();
        }
        List<JkJavaIdeSupport.JkSupplier> suppliers = commands.getPlugins().getLoadedPluginInstanceOf(
                JkJavaIdeSupport.JkSupplier.class);
        return suppliers.stream()
                .filter(supplier -> supplier != null)
                .map(supplier -> supplier.getJavaIdeSupport())
                .filter(projectIde -> projectIde != null)
                .findFirst().orElse(null);
    }

}
