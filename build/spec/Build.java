
import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDir;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaPacker;
import org.jake.utils.JakeUtilsFile;

/**
 * Build class for Jake itself.
 * This build does not rely on any dependence manager.
 */
public class Build extends JakeJavaBuild {

    // Just to run directly the whole build bypassing the Jake bootstrap mechanism.
    // Was necessary in first place to build Jake with itself.
    public static void main(String[] args) {
        JakeOptions.forceVerbose(true);
        new Build().base();
    }

    // Include a time stamped version file as resource.
    @Override
    protected void generateResources() {
        final File versionFile = new File(generatedResourceDir(),"org/jake/version.txt");
        JakeUtilsFile.writeString(versionFile, version().name(), false);
    }

    // Normally the default method just go to compile and unit tests.
    // Here we tell that the default method should also package the application
    @Override
    public void base() {
        super.base();
        pack();
    }

    // Include the making of the distribution into the application packaging.
    @Override
    public void pack() {
        super.pack();
        distrib();
    }

    // Create the whole distribution : creates distrib directory and zip containing all
    private void distrib() {
        final File distribDir = ouputDir("jake-distrib");
        final File distripZipFile = ouputDir("jake-distrib.zip");

        JakeLog.start("Creating distrib " + distripZipFile.getPath());
        JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
        final JakeJavaPacker jarPacker = packer();
        JakeUtilsFile.copyFile(jarPacker.jarFile(), new File(distribDir,"jake.jar"));
        JakeUtilsFile.copyFile(jarPacker.jarSourceFile(), new File(distribDir,"jake-sources.jar"));
        JakeDir.of(this.baseDir("build/libs/compile")).include("**/*.jar").copyTo(new File(distribDir, "libs/required"));
        JakeDir.of(this.baseDir("build/libs-sources")).copyTo(new File(distribDir, "libs/sources"));
        JakeUtilsFile.zipDir(distripZipFile, Deflater.BEST_COMPRESSION, distribDir);
        JakeLog.done();
    }

}
