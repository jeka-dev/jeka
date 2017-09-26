package org.jerkar.tool.builtins.idea;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.ide.idea.JkImlGenerator;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildPlugin2;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.Main;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkBuildPlugin2Idea implements JkBuildPlugin2 {


    @JkDoc("If true, path to cache repository and to Jerkar install will be replaces by $JERKAR_REPO$ and $JERKAR_HOME$ path variable")
    boolean useVarPath = false;

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file")
    public void generateIml(JkJavaProjectBuild build) {
        final JkJavaProject project = build.project();
        final JkImlGenerator generator = new JkImlGenerator(project);
        final List<File> depProjects = new LinkedList<>();
        for (final JkBuild depBuild : build.importedBuilds().directs()) {
            depProjects.add(depBuild.baseTree().root());
        }
        generator.setUseVarPath(useVarPath);
        generator.setBuildDependencies(build.buildDependencyResolver(), build.buildDependencies());
        generator.setImportedBuildProjects(depProjects);
        generator.setDependencies(project.maker().getDependencyResolver(), project.getDependencies());
        generator.setSourceJavaVersion(project.getSourceVersion());
        generator.setForceJdkVersion(true);
        final String xml = generator.generate();
        final File imlFile = new File(project.getSourceLayout().baseDir(), project.getSourceLayout().baseDir().getName()+".iml");
        JkUtilsFile.delete(imlFile);
        JkUtilsFile.writeString(imlFile, xml, false);
        JkLog.info(imlFile.getPath() + " generated.");
    }

    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file")
    public void generateModulesXml(JkBuild build) {
        final File current = build.baseTree().root();
        final Iterable<File> imls = build.baseTree().include("**/*.iml");
        final ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void generateAllIml(JkBuild build) {
        final Iterable<File> folders = build.baseTree()
                .include("**/" + JkConstants.BUILD_DEF_DIR)
                .exclude("**/build/output/**")
                .files(true);
        for (final File folder : folders) {
            final File projectFolder = folder.getParentFile().getParentFile();
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