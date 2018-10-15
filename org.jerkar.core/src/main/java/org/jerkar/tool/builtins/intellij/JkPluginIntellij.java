package org.jerkar.tool.builtins.intellij;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.ide.intellij.JkImlGenerator;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.tool.*;
import org.jerkar.tool.builtins.java.JkPluginJava;
import org.jerkar.tool.builtins.scaffold.JkPluginScaffold;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@JkDoc("Generation of Idea Intellij metadata files (*.iml and modules.xml).")
@JkDocPluginDeps(JkPluginScaffold.class)
public final class JkPluginIntellij extends JkPlugin {

    @JkDoc("If true, dependency paths will be expressed relatively to $JERKAR_REPO$ and $JERKAR_HOME$ path variable instead of absolute paths.")
    public boolean useVarPath = true;

    @JkDoc("If true, the project dependencies are not taken in account to generate iml, only run class dependencies are.")
    public boolean onlyRunDependencies = false;

    @JkDoc("If true, the project taken in account is not the run project but the project configured in java plugin.")
    public boolean externalDir = false;

    private final JkPluginScaffold scaffold;

    protected JkPluginIntellij(JkRun run) {
        super(run);
        scaffold = run.plugins().get(JkPluginScaffold.class);
    }

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file.")
    public void generateIml() {
        final JkImlGenerator generator;
        if (owner.plugins().hasLoaded(JkPluginJava.class)) {
            generator = JkImlGenerator.of(owner.plugins().get(JkPluginJava.class).project());
        } else {
            generator = JkImlGenerator.of(owner.baseDir());
        }
        final List<Path> depProjects = new LinkedList<>();
        for (final JkRun depRun : owner.importedRuns().directs()) {
            depProjects.add(depRun.baseTree().getRoot());
        }
        generator.setUseVarPath(useVarPath);
        generator.setRunDependencies(externalDir ? null : owner.runDependencyResolver(), owner.runDependencies());

        generator.setImportedProjects(depProjects);
        Path basePath = owner.baseDir();
        if (owner.plugins().hasLoaded(JkPluginJava.class)) {
            JkJavaProject project = owner.plugins().get(JkPluginJava.class).project();
            if (!onlyRunDependencies) {
                generator.setDependencies(project.maker().getDependencyResolver(), project.getDependencies());
            } else {
                generator.setDependencies(project.maker().getDependencyResolver(), JkDependencySet.of());
            }
            generator.setSourceJavaVersion(project.getSourceVersion());
            generator.setForceJdkVersion(true);
            if (externalDir) {
                basePath = project.baseDir();
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
        final Path current = owner.baseTree().getRoot();
        final Iterable<Path> imls = owner.baseTree().andAccept("**.iml").getFiles();
        final ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
        JkLog.info("File generated at : " + modulesXmlGenerator.outputFile());
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void generateAllIml() {
        final Iterable<Path> folders = owner.baseTree()
                .andAccept("**/" + JkConstants.DEF_DIR, JkConstants.DEF_DIR)
                .andReject("**/" + JkConstants.OUTPUT_PATH + "/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.execute("Generating iml file on " + projectFolder, () ->
                Main.exec(projectFolder, "intellij#generateIml"));
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
        scaffold.addExtraAction(this::generateIml);
    }
}