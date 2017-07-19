package org.jerkar;

import java.io.File;
import java.util.jar.Attributes.Name;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkZipper.JkCompressionLevel;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

/**
 * Build class for Jerkar itself. This build does not rely on any dependence
 * manager.
 */
public class CoreBuild extends AbstractBuild {

    private final File distripZipFile;

    /** The folder where is generated the core distrib */
    public File distribFolder;

    CoreBuild() {
        distripZipFile = ouputDir("jerkar-distrib.zip");
        distribFolder = ouputDir("jerkar-distrib");
        this.pack.fatJar = true;
        this.pack.fatJarSuffix = "all";
    }

    /** Run the doDefault method */
    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).doDefault();
    }

    @Override
    protected JkManifest jarManifest() {
        String version = version().name();
        if (version().isSnapshot()) {
            version = version + " - built at " + buildTimestamp();
        }
        return super.jarManifest().addMainClass("org.jerkar.tool.Main")
                .addMainAttribute(Name.IMPLEMENTATION_VERSION, version);
    }

    // Include the making of the distribution into the application packaging.
    @Override
    public void pack() {
        super.pack();
        JkFileTree.of(this.classDir()).exclude("**/*.jar").zip().to(packer().jarFile("lean"));
        distrib();
    }

    private void distrib() {

        final JkFileTree distrib = JkFileTree.of(distribFolder);
        JkLog.startln("Creating distrib " + distripZipFile.getPath());
        distrib.importFiles(file("../LICENSE"));
        final JkJavaPacker packer = packer();
        distrib.importDirContent(file("src/main/dist"));
        distrib.importDirContent(file("src/main/java/META-INF/bin"));

        // Create lean jar

        // Simpler to put both Jerkar and Jerkar-fat jar at the root (in order
        // to find the Jerker HOME)
        distrib.importFiles(packer.jarFile());
        distrib.importFiles(packer().jarFile("lean"));
        distrib.jump("libs-sources").importDirContent(file("build/libs-sources"))
        .importFiles(packer.jarSourceFile());
        distrib.jump("libs-javadoc").importFiles(this.javadocMaker().zipFile());
        distrib.zip().with(JkCompressionLevel.BEST_COMPRESSION).to(distripZipFile);
        signIfNeeded(distripZipFile);
        JkLog.done();
    }

}
