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

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Full black-box tests on sample projects
 */
class SampleTester {

    private final JkPathTree sampleBaseDir;

    private final JkPathTree sampleDependerBaseDir;

    private final JkPathTree output;

    private String jekaScript;
    
    boolean restoreEclipseClasspathFile;

    SampleTester(JkPathTree buildDir) {
        super();
        this.sampleBaseDir = buildDir.goTo("../dev.jeka.core.samples");
        this.sampleDependerBaseDir = buildDir.goTo("../dev.jeka.core.depender-samples");
        this.output = sampleBaseDir.goTo(JkConstants.OUTPUT_PATH);
        String scriptName = JkUtilsSystem.IS_WINDOWS ? "jeka.bat" : "jeka";
        jekaScript = buildDir.goTo(JkConstants.OUTPUT_PATH).get("distrib/" + scriptName).toAbsolutePath().toString();
    }

    void doTest() throws IOException {
        testSamples("AClassicBuild");
        testSamples("AntStyleBuild");
        testSamples("MavenStyleBuild");
        testSamples("OpenSourceJarBuild");
        testSamples("HttpClientTaskBuild");
        testSamples("SimpleScopeBuild");
        testDepender("FatJarBuild");

        // Test eclipse
        JkLog.startTask("Test Eclipse .classpath generation");
        Path classpathFile = sampleBaseDir.get(".classpath");
        Path classpathFile2 = sampleBaseDir.get(".classpath2");
        Files.copy(classpathFile, classpathFile2, StandardCopyOption.REPLACE_EXISTING);
        testSamples("", "eclipse#generateAll");
        if (restoreEclipseClasspathFile) {
            Files.move(classpathFile2, classpathFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.delete(classpathFile2);
        }

        // Test intellij
        JkLog.startTask("Test Intellij generate all");
        Path project = JkUtilsPath.createTempDirectory("jeka-test-");
        sampleBaseDir.andMatching(false, ".idea/**/*", "jeka/output/**/*").copyTo(project);
        JkProcess.of(jekaScript).withFailOnError(true).withWorkingDir(project).andParams("intellij#generateAll").runSync();
        JkLog.endTask();

        testDepender("NormalJarBuild");
        testFork();
        testScaffoldJava();
    }

    private void testSamples(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        JkProcess.of(jekaScript).withWorkingDir(sampleBaseDir.getRoot().toAbsolutePath().normalize())
                .withParamsIf(!JkUtilsString.isBlank(className), "-LV=true -RC=" + className)
                .andParams("clean", "java#pack", "java#publish", "-java#publish.localOnly", "-LH")
                .andParams(args)
                .withFailOnError(true).runSync();
    }

    private void testDepender(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        JkProcess.of(jekaScript).withWorkingDir(this.sampleDependerBaseDir.getRoot())
                .withParamsIf(!JkUtilsString.isBlank(className), "-RC=" + className)
                .withParams("clean", "java#pack")
                .andParams(args)
                .withFailOnError(true).runSync();
    }

    private void testScaffoldJava() {
        JkLog.info("Test scaffold Java");
        Path root = JkUtilsPath.createTempDirectory("jeka");
        process().withWorkingDir(root).andParams("scaffold#run", "java#", "intellij#").runSync();
        process().withWorkingDir(root).andParams("java#pack").runSync();
        JkPathTree.of(root).deleteRoot();
    }

    private void scaffoldAndEclipse() {
        Path scafoldedProject = output.getRoot().resolve("scaffolded");
        JkProcess scaffoldProcess = process().withWorkingDir(scafoldedProject);
        JkUtilsPath.createDirectories(scafoldedProject);
        scaffoldProcess.withParams("scaffold").runSync(); // scaffold
        // project
        scaffoldProcess.runSync(); // Build the scaffolded project
        JkLog.info("Test eclipse generation and compile            ");
        scaffoldProcess.withParams("eclipse#generateAll").runSync();
        scaffoldProcess.withParams("eclipse#").runSync(); // build using the .classpath for resolving classpath
        scaffoldProcess.withParams("idea#generateIml", "idea#generateModulesXml").runSync();
    }

    private JkProcess process() {
        return JkProcess.of(jekaScript).withFailOnError(true);
    }

    private void testFork() {
        testSamples("", "-java#tests.fork");
        JkUtilsAssert.isTrue(output.goTo("test-reports/junit").exists(), "No test report generated in test fork mode.");
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(CoreBuild.class, "-verbose=true").testSamples();
    }

}
