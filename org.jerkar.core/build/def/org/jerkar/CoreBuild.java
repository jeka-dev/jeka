package org.jerkar;

import java.io.File;
import java.util.zip.Deflater;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.builtins.javabuild.JkJavaPacker;

/**
 * Build class for Jerkar itself. This build does not rely on any dependence
 * manager.
 */
public class CoreBuild extends AbstractBuild {

    public File distripZipFile;

    public File distribFolder;

    @Override
    protected void init() {
	super.init();
	distripZipFile = ouputDir("jerkar-distrib.zip");
	distribFolder = ouputDir("jerkar-distrib");
	this.pack.fatJar = true;
    }

    // Just to run directly the whole build bypassing the Jerkar bootstrap
    // mechanism.
    // Was necessary in first place to build Jerkar with itself.
    public static void main(String[] args) {
	new CoreBuild().initialized().doDefault();
    }

    // Include a time stamped version file as resource.
    @Override
    protected JkResourceProcessor resourceProcessor() {
	final String version = version().name() + " - built at " + buildTimestamp();
	return super.resourceProcessor().interpolating("META-INF/MANIFEST.MF", "version", version);
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

	// Create lean jar

	// Simpler to put both Jerkar and Jerkar-fat jar at the root (in order
	// to find the Jerker HOME)
	distrib.importFiles(packer.jarFile());
	distrib.importFiles(packer().jarFile("lean"));
	distrib.from("libs-sources").importDirContent(file("build/libs-sources")).importFiles(packer.jarSourceFile());
	distrib.from("libs-javadoc").importFiles(this.javadocMaker().zipFile());
	distrib.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
	signIfNeeded(distripZipFile);
	JkLog.done();
    }

}
