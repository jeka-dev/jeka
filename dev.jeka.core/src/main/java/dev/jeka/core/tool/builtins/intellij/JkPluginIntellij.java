package dev.jeka.core.tool.builtins.intellij;

import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.api.ide.intellij.JkImlGenerator;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;

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

    private final JkPluginScaffold scaffold;

    protected JkPluginIntellij(JkCommands run) {
        super(run);
        scaffold = run.getPlugins().get(JkPluginScaffold.class);
    }

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file.")
    public void generateIml() {
        final JkImlGenerator generator;
        if (getRun().getPlugins().hasLoaded(JkPluginJava.class)) {
            generator = JkImlGenerator.of(getRun().getPlugins().get(JkPluginJava.class).getProject());
        } else {
            generator = JkImlGenerator.of(getRun().getBaseDir());
        }
        final List<Path> depProjects = getRun().getImportedCommands().getImportedRunRoots();
        generator.setUseVarPath(useVarPath);
        generator.setRunDependencies(externalDir ? null : getRun().getRunDependencyResolver(),
                getRun().getDefDependencies());
        generator.setImportedProjects(depProjects);
        Path basePath = getRun().getBaseDir();
        if (getRun().getPlugins().hasLoaded(JkPluginJava.class)) {
            JkJavaProject project = getRun().getPlugins().get(JkPluginJava.class).getProject();
            generator.setSourceJavaVersion(project.getSourceVersion());
            generator.setForceJdkVersion(true);
            if (externalDir) {
                basePath = project.getBaseDir();
            }
        }
        final String xml = generator.generate();
        String filename = basePath.getFileName().toString() + ".iml";
        Path candidateImlFile = basePath.resolve(filename);
        final Path imlFile = Files.exists(candidateImlFile) ? candidateImlFile :
                basePath.resolve(".idea").resolve(filename);
        JkUtilsPath.deleteIfExists(imlFile);
        JkUtilsPath.createDirectories(imlFile.getParent());
        JkUtilsPath.write(imlFile, xml.getBytes(Charset.forName("UTF-8")));
        JkLog.info("Iml file generated at " + imlFile);
    }

    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file.")
    public void generateModulesXml() {
        final Path current = getRun().getBaseTree().getRoot();
        final Iterable<Path> imls = getRun().getBaseTree().andMatching(true,"**.iml").getFiles();
        final ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
        JkLog.info("File generated at : " + modulesXmlGenerator.outputFile());
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void generateAllIml() {
        final Iterable<Path> folders = getRun().getBaseTree()
                .andMatching(true, "**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andMatching(false, "**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startTask("Generating iml file on " + projectFolder);
            try {
                Main.exec(projectFolder, "intellij#generateIml");
            } catch (Exception e) {
                JkLog.warn("Generating Iml failed : Try to generate it using -CC=JkCommands option.");
                try {
                    Main.exec(projectFolder, "intellij#generateIml", "-CC=JkCommands");
                } catch (Exception e1) {
                    JkLog.warn("Generatint Iml file failed;");
                }
            }
            JkLog.endTask();
        }
    }

    @JkDoc("Shorthand for intellij#generateAllIml + intellij#generateModulesXml.")
    public void generateAll() {
        generateAllIml();
        generateModulesXml();
    }

    @JkDoc("Adds *.iml generation to scaffolding.")
    @Override
    protected void activate() {
        scaffold.getScaffolder().getExtraActions().chain(this::generateIml);
    }
}