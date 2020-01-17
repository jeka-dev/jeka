package dev.jeka.core.tool.builtins.intellij;

import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@JkDoc("Generation of Idea Intellij metadata files (*.iml and modules.xml).")
@JkDocPluginDeps(JkPluginScaffold.class)
public final class JkPluginIntellij extends JkPlugin {

    @JkDoc("If true, dependency paths will be expressed relatively to $JEKA_REPO$ and $JEKA_HOME$ path variable instead of absolute paths.")
    public boolean useVarPath = true;

    @JkDoc("If true, the project taken in account is not the run project but the project configured in java plugin.")
    public boolean externalDir = false;

    @JkDoc("By default, generated iml files specify a JDK inherited from the project setup. " +
            "Set this option to 'true' for  forcing the JDK version to the one defined in JkJavaProject.")
    public boolean forceJdkVersion = false;

    private final JkPluginScaffold scaffold;

    protected JkPluginIntellij(JkCommands run) {
        super(run);
        scaffold = run.getPlugins().get(JkPluginScaffold.class);
    }

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file.")
    public void iml() {
        final JkImlGenerator generator;
        if (getCommands().getPlugins().hasLoaded(JkPluginJava.class)) {
            generator = JkImlGenerator.of(getCommands().getPlugins().get(JkPluginJava.class).getProject()
                    .getJavaProjectIde());
        } else {
            generator = JkImlGenerator.of(getCommands().getBaseDir());
        }
        final List<Path> depProjects = getCommands().getImportedCommands().getImportedRunRoots();
        generator.setUseVarPath(useVarPath);
        generator.setRunDependencies(externalDir ? null : getCommands().getRunDependencyResolver(),
                getCommands().getDefDependencies());
        generator.setImportedProjects(depProjects);
        Path basePath = getCommands().getBaseDir();
        if (getCommands().getPlugins().hasLoaded(JkPluginJava.class)) {
            JkJavaProject project = getCommands().getPlugins().get(JkPluginJava.class).getProject();
            generator.setSourceJavaVersion(project.getCompileSpec().getSourceVersion());
            generator.setForceJdkVersion(forceJdkVersion);
            if (externalDir) {
                basePath = project.getBaseDir();
            }
        }
        final String xml = generator.generate();
        String filename = basePath.getFileName().toString() + ".iml";
        Path candidateImlFile = basePath.resolve(".idea").resolve(filename);
        final Path imlFile = Files.exists(candidateImlFile) ? candidateImlFile :
                basePath.resolve(filename);
        JkUtilsPath.deleteIfExists(imlFile);
        JkUtilsPath.createDirectories(imlFile.getParent());
        JkUtilsPath.write(imlFile, xml.getBytes(Charset.forName("UTF-8")));
        JkLog.info("Iml file generated at " + imlFile);
    }

    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file.")
    public void modulesXml() {
        final Path current = getCommands().getBaseTree().getRoot();
        final Iterable<Path> imls = getCommands().getBaseTree().andMatching(true,"**.iml").getFiles();
        final ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
        JkLog.info("File generated at : " + modulesXmlGenerator.outputFile());
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void allIml() {
        final Iterable<Path> folders = getCommands().getBaseTree()
                .andMatching(true, "**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andMatching(false, "**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startTask("Generating iml file on " + projectFolder);
            try {
                Main.exec(projectFolder, "intellij#iml");
            } catch (Exception e) {
                JkLog.warn("Generating Iml failed : Try to generate it using -CC=JkCommands option. Failure cause : ");
                JkLog.warn(e.getMessage());
                PrintWriter printWriter = new PrintWriter(JkLog.getErrorStream());
                e.printStackTrace(printWriter);
                printWriter.flush();
                try {
                    Main.exec(projectFolder, "intellij#iml", "-CC=JkCommands");
                } catch (Exception e1) {
                    JkLog.warn("Generating Iml file failed;");
                }
            }
            JkLog.endTask();
        }
    }

    @JkDoc("Shorthand for intellij#allIml + intellij#modulesXml.")
    public void all() {
        allIml();
        modulesXml();
    }

    @JkDoc("Adds *.iml generation to scaffolding.")
    @Override
    protected void activate() {
        scaffold.getScaffolder().getExtraActions().chain(this::iml);
    }
}