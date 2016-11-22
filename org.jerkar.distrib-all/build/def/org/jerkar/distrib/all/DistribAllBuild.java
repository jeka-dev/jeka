package org.jerkar.distrib.all;

import java.io.File;

import org.jerkar.AbstractBuild;
import org.jerkar.CoreBuild;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.plugins.jacoco.PluginsJacocoBuild;
import org.jerkar.plugins.protobuf.PluginsProtobufBuild;
import org.jerkar.plugins.sonar.PluginsSonarBuild;
import org.jerkar.tool.JkBuildDependencySupport;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkProject;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

class DistribAllBuild extends JkBuildDependencySupport {

    @JkProject("../org.jerkar.plugins-sonar")
    PluginsSonarBuild pluginsSonar;

    @JkProject("../org.jerkar.plugins-jacoco")
    PluginsJacocoBuild pluginsJacoco;
    
    @JkProject("../org.jerkar.plugins-protobuf")
    PluginsProtobufBuild pluginsProtobuf;
    
    AbstractBuild[] plugins = {
        pluginsSonar,
        pluginsJacoco,
        pluginsProtobuf
    };
    
    // The core project is got by transitivity
    CoreBuild core = pluginsJacoco.core;

    public boolean testSamples = false;

    @JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
    public void distrib() {

        JkLog.startln("Creating distribution file");

        JkLog.info("Copy core distribution localy.");
        JkFileTree dist = JkFileTree.of(ouputDir("dist")).importDirContent(core.distribFolder);

        JkLog.info("Add plugins to the distribution");
        JkFileTree ext = dist.from("libs/builtins");
        JkFileTree sourceDir = dist.from("libs-sources");
        JkFileTreeSet sources = core.sources();
        for (AbstractBuild plugin : plugins) {
            JkJavaPacker packer = plugin.packer();
            ext = ext.importFiles(packer.jarFile());
            sourceDir = sourceDir.importFiles(packer.jarSourceFile());
            sources = sources.and(plugin.sources());
        }

        JkLog.info("Add plugins to the fat jar");
        File fat = dist.file(core.packer().fatJarFile().getName());
        JkUtilsFile.copyFile(core.packer().jarFile(), fat);
        JkZipper.of().merge(ext.include("**/*.jar")).appendTo(fat);

        JkLog.info("Create a fat source jar");
        File fatSource = sourceDir.file("org.jerkar.core-all-sources.jar");
        JkZipper.of().merge(sourceDir.include("**.jar", "**.zip").exclude(fatSource.getName())).to(fatSource);

        JkLog.info("Create a fat javadoc");
        File javadocAllDir = ouputDir("javadoc-all");
        File javadocAllFile = dist.file("libs-javadoc/org.jerkar.core-fat-javadoc.jar");
        JkJavadocMaker.of(sources, javadocAllDir, javadocAllFile).process();

        JkLog.info("Pack all");
        dist.zip().to(ouputDir("jerkar-distrib.zip"));

        JkLog.done();
    }
    
    @JkDoc("End to end method to construct a distrib.")
    public void doDefault() {
        super.doDefault();
        core.pack.javadoc = false;
        slaves().invokeDoDefaultMethodOnAll();
        distrib();
        if (testSamples) {
            testSamples();
        }
    }

    public void testSamples() {
        JkLog.startHeaded("Testing Samples");
        new SampleTester(this.baseDir()).doTest();
        JkLog.done();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(DistribAllBuild.class, args).doDefault();
    }

}
