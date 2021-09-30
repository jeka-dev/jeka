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
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Full black-box tests on sample projects
 */
class SampleTester {

    private final JkPathTree sampleBaseDir;

    private final JkPathTree sampleDependerBaseDir;

    private final JkPathTree sampleJunit5BaseDir;

    private final JkPathTree output;

    private String jekaCmd;

    private String junit5JekaCmd;
    
    boolean restoreEclipseClasspathFile;

    SampleTester(JkPathTree buildDir) {
        super();
        this.sampleBaseDir = buildDir.goTo("../dev.jeka.core-samples");
        this.sampleDependerBaseDir = buildDir.goTo("../dev.jeka.core-samples-dependers");
        this.sampleJunit5BaseDir = buildDir.goTo("../dev.jeka.core-samples-junit5");
        this.output = sampleBaseDir.goTo(JkConstants.OUTPUT_PATH);
        String scriptName = JkUtilsSystem.IS_WINDOWS ? "jekaw.bat" : "jekaw";
        jekaCmd = sampleBaseDir.get(scriptName).toString();
        junit5JekaCmd = sampleJunit5BaseDir.get(scriptName).toString();
    }

    void doTest() throws IOException {

        testJunit5("Junit5Build");

        testSampleWithJavaPlugin("JacocoPluginBuild");

        testScaffoldWithExternalPlugin();

        testSampleWith("JavaPluginBuild", "cleanPackPublish");
        testSampleWith("SignedArtifactsBuild", "cleanPackPublish");
        testSampleWith("ThirdPartyPoweredBuild", "cleanPack");

        testSampleWith("SonarPluginBuild", "cleanPackSonar");
        testSampleWith("AntStyleBuild", "cleanPackPublish");
        testSampleWith("PureApiBuild", "cleanBuild");

        testDepender("FatJarBuild");

        // Test eclipse
        JkLog.startTask("Test Eclipse .classpath generation");
        Path classpathFile = sampleBaseDir.get(".classpath");
        Path classpathFile2 = sampleBaseDir.get(".classpath2");
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

        testDepender("NormalJarBuild");

        testFork();

    }

    private void testSampleWithJavaPlugin(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        JkProcess.of(jekaCmd).setWorkingDir(sampleBaseDir.getRoot().toAbsolutePath().normalize())
                .addParamsIf(!JkUtilsString.isBlank(className), "-JKC=" + className)
                .addParams("clean", "java#pack", "java#publish", "-java#publish.localOnly", "-LB", "-LRI", "-LSU", "-LV=false")
                .addParams(args)
                .setFailOnError(true).exec();
    }

    private void testSampleWith(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        JkProcess.of(jekaCmd).setWorkingDir(sampleBaseDir.getRoot().toAbsolutePath().normalize())
                .addParamsIf(!JkUtilsString.isBlank(className), "-JKC=" + className)
                .addParams("-LB", "-LRI", "-LSU", "-LV=false")
                .addParams(args)
                .setFailOnError(true)
                .exec();
    }

    private void testDepender(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        JkProcess.of(jekaCmd).setWorkingDir(this.sampleDependerBaseDir.getRoot())
                .addParamsIf(!JkUtilsString.isBlank(className), "-JKC=" + className)
                .addParams("clean", "java#pack", "-LB", "-LRI", "-LSU")
                .addParams(args)
                .setFailOnError(true).exec();
    }

    private void testJunit5(String className, String... args) {
        JkLog.info("Test " + className + " " + Arrays.toString(args));
        JkProcess.of(junit5JekaCmd)
                .setWorkingDir(this.sampleJunit5BaseDir.getRoot())
                .addParamsIf(!JkUtilsString.isBlank(className), "-JKC=" + className)
                .addParams("clean", "java#pack", "-LB", "-LRI", "-LSU")
                .addParams(args)
                .setFailOnError(true)
                .exec();
    }

    // => pb when used with wrapper as the jeka location is mentioned relative to original sample dir
    private void testScaffoldJava() {
        JkLog.info("Test scaffold Java");
        Path root = JkUtilsPath.createTempDirectory("jeka");
        process().setWorkingDir(root).addParams("scaffold#run", "java#", "intellij#").exec();
        process().setWorkingDir(root).addParams("java#pack").exec();
        JkPathTree.of(root).deleteRoot();
    }

    private void scaffoldAndEclipse() {
        Path scafoldedProject = output.getRoot().resolve("scaffolded");
        JkProcess scaffoldProcess = process().setWorkingDir(scafoldedProject);
        JkUtilsPath.createDirectories(scafoldedProject);
        scaffoldProcess.clone().addParams("scaffold").exec(); // scaffold
        // project
        scaffoldProcess.exec(); // Build the scaffolded project
        JkLog.info("Test eclipse generation and compile            ");
        scaffoldProcess.clone().addParams("eclipse#all").exec();
        scaffoldProcess.clone().addParams("eclipse#").exec(); // build using the .classpath for resolving classpath
        scaffoldProcess.clone().addParams("idea#generateIml", "idea#generateModulesXml").exec();
    }

    private JkProcess process() {
        return JkProcess.of(jekaCmd).setFailOnError(true);
    }

    private void testFork() {
        testSampleWithJavaPlugin("JavaPluginBuild", "-java#tests.fork");
        JkUtilsAssert.state(output.goTo("test-report").exists(), "No test report generated in test fork mode.");
    }

    private void testScaffoldWithExternalPlugin() {
        JkLog.info("Test scaffold with springboot plugin");
        Path dir = JkUtilsPath.createTempDirectory("jeka-test");
        JkProcess.ofWinOrUx("jeka.bat", "jeka").setWorkingDir(dir)
                .addParams("-LB", "-LRI", "-LSU", "-LV=false", "-JKC=",
                        "scaffold#run", "@dev.jeka:springboot-plugin:+", "springboot#")
                .setFailOnError(true).exec();
        JkPathTree.of(dir).deleteRoot();
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(CoreBuild.class, "-verbose=true").testSamples();
    }

}
