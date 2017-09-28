package org.jerkar.tool.builtins.idea;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.ide.idea.JkImlGenerator;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildPlugin;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkJavaProjectBuild;
import org.jerkar.tool.Main;

@JkDoc("Provides method to generate Idea Intellij metadata files.")
public final class JkBuildPluginIdea implements JkBuildPlugin {


    @JkDoc("If true, path to cache repository and to Jerkar install will be replaces by $JERKAR_REPO$ and $JERKAR_HOME$ path variable")
    boolean useVarPath = false;

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file")
    public void generateIml(JkJavaProjectBuild build) {
        final JkJavaProject project = build.project();
        final JkImlGenerator generator = new JkImlGenerator(project);
        final List<Path> depProjects = new LinkedList<>();
        for (final JkBuild depBuild : build.importedBuilds().directs()) {
            depProjects.add(depBuild.baseTree().rootPath());
        }
        generator.setUseVarPath(useVarPath);
        generator.setBuildDependencies(build.buildDependencyResolver(), build.buildDependencies());
        generator.setImportedBuildProjects(depProjects);
        generator.setDependencies(project.maker().getDependencyResolver(), project.getDependencies());
        generator.setSourceJavaVersion(project.getSourceVersion());
        generator.setForceJdkVersion(true);
        final String xml = generator.generate();
        final Path imlFile = project.getSourceLayout().basePath().resolve(
                project.getSourceLayout().basePath().getFileName().toString() + ".iml");
        JkUtilsPath.deleteFile(imlFile);
        JkUtilsPath.write(imlFile, xml.getBytes(Charset.forName("UTF-8")));
        JkLog.info(imlFile + " generated.");
    }

    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file")
    public void generateModulesXml(JkBuild build) {
        final Path current = build.baseTree().rootPath();
        final Iterable<Path> imls = build.baseTree().include("**/*.iml").paths(false);
        final ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void generateAllIml(JkBuild build) {
        final Iterable<Path> folders = build.baseTree()
                .include("**/" + JkConstants.BUILD_DEF_DIR)
                .exclude("**/build/output/**")
                .paths(true);
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startln("Generating iml file on " + projectFolder);
            Main.exec(projectFolder, "idea#generateIml");
            JkLog.done();
        }
    }

    @JkDoc(("Shorthand for #generateAllIml + generateModulesXml"))
    public void generateAll(JkBuild build) {
        generateAllIml(build);
        generateModulesXml(build);
    }

    @Override
    public void apply(JkBuild build) {

    }
}