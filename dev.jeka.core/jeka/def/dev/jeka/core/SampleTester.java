package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkInit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Full black-box tests on sample projects
 */
class SampleTester {

    private final Path samplesRootDir;
    
    boolean restoreEclipseClasspathFile;

    SampleTester(Path samplesRootDir) {
        super();
        this.samplesRootDir = samplesRootDir;
    }

    private String jekawCmd(Path dir) {
        String scriptName = JkUtilsSystem.IS_WINDOWS ? "jekaw.bat" : "jekaw";
        return JkUtilsPath.relativizeFromWorkingDir(dir.resolve(scriptName)).toAbsolutePath().toString();
    }

    void doTest() throws IOException {

        // Test eclipse
        Path basicSamplesDir = samplesRootDir.resolve("dev.jeka.samples.basic");
        JkLog.startTask("Test Eclipse .classpath generation");
        Path classpathFile = basicSamplesDir.resolve(".classpath");
        Path classpathFile2 = basicSamplesDir.resolve(".classpath2");
        boolean copyClasspath = false;
        if (Files.exists(classpathFile)) {
            Files.copy(classpathFile, classpathFile2, StandardCopyOption.REPLACE_EXISTING);
            copyClasspath = true;
        }
        testSampleWithJavaPlugin("", "-JKC=JavaPluginBuild", "eclipse#files");
        if (restoreEclipseClasspathFile && copyClasspath) {
            Files.move(classpathFile2, classpathFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(classpathFile2);
        }
        JkLog.endTask();
    }

    private void testSampleWithJavaPlugin(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        Path dir = samplesRootDir.resolve("dev.jeka.samples.basic");
        process(dir)
                .addParamsIf(!JkUtilsString.isBlank(className), "-JKC=" + className)
                .addParams("clean", "java#pack", "java#publish", "-java#publish.localOnly", "-LB", "-LRI", "-LSU", "-LV=false")
                .addParams(args)
                .exec();
    }

    private void scaffoldAndEclipse() {
        Path output = samplesRootDir.resolve("dev.jeka.core").resolve(JkConstants.OUTPUT_PATH);
        Path scafoldedProject = output.resolve("scaffolded");
        JkProcess scaffoldProcess = process(scafoldedProject);
        JkUtilsPath.createDirectories(scafoldedProject);
        scaffoldProcess.clone().addParams("scaffold").exec(); // scaffold
        // project
        scaffoldProcess.exec(); // Build the scaffolded project
        JkLog.info("Test eclipse generation and compile            ");
        scaffoldProcess.clone().addParams("eclipse#all").exec();
        scaffoldProcess.clone().addParams("eclipse#").exec(); // build using the .classpath for resolving classpath
        scaffoldProcess.clone().addParams("idea#generateIml", "idea#generateModulesXml").exec();
    }

    private JkProcess process(Path dir) {
        return JkProcess.of(jekawCmd(dir))
                .setWorkingDir(dir)
                .setFailOnError(true);
    }



    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(CoreBuild.class, "-LV").testSamples();
    }

}
