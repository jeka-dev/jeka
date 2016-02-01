package org.jerkar.tool.builtins.javabuild;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkFileSystemDependency;
import org.jerkar.api.depmanagement.JkIvyPublication;
import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.tooling.JkMvn;
import org.jerkar.api.utils.JkUtilsJdk;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkBuildDependencySupport;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkException;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.JkScaffolder;

/**
 * Template class to define build on Java project. This template is flexible
 * enough to handle exotic project structure as it proposes to override default
 * setting at different level of granularity.<b/> Beside this template define a
 * set of "standard" scope to define dependencies. You are not forced to use it
 * strictly but it can simplify dependency management to follow a given
 * standard.
 *
 * @author Jerome Angibaud
 */
public class JkJavaBuild extends JkBuildDependencySupport {

    /** Dependencies to compile the project but that should not be embedded in produced artifacts */
    public static final JkScope PROVIDED = JkScope
            .of("provided")
            .transitive(false)
            .descr("Dependencies to compile the project but that should not be embedded in produced artifacts.");

    /** Dependencies to compile the project */
    public static final JkScope COMPILE = JkScope.of("compile").descr(
            "Dependencies to compile the project.");

    /** Dependencies to embed in produced artifacts (as war or fat jar * files). */
    public static final JkScope RUNTIME = JkScope.of("runtime").extending(COMPILE)
            .descr("Dependencies to embed in produced artifacts (as war or fat jar * files).");

    /** Dependencies necessary to compile and run tests. */
    public static final JkScope TEST = JkScope.of("test").extending(RUNTIME, PROVIDED)
            .descr("Dependencies necessary to compile and run tests.");

    /** Contains the source artifacts. */
    public static final JkScope SOURCES = JkScope.of("sources").transitive(false)
            .descr("Contains the source artifacts.");

    /** Contains the Javadoc of this project". */
    public static final JkScope JAVADOC = JkScope.of("javadoc").transitive(false)
            .descr("Contains the javadoc of this project");

    private static final String ARCHIVE_MASTER = "archives(master)";

    private static final JkScopeMapping SCOPE_MAPPING = JkScopeMapping
            .of(COMPILE).to(ARCHIVE_MASTER, COMPILE.name() + "(default)")
            .and(PROVIDED).to(ARCHIVE_MASTER, COMPILE.name() + "(default)")
            .and(RUNTIME).to(ARCHIVE_MASTER, RUNTIME.name() + "(default)")
            .and(TEST).to(ARCHIVE_MASTER, RUNTIME.name() + "(default)", TEST.name() + "(default)");

    /**
     * Filter to excludes everything in a java source directory which are not
     * resources.
     */
    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    /** Options about tests */
    @JkDoc("Tests")
    public JkOptionTest tests = new JkOptionTest();

    /** Options about packaging jars */
    @JkDoc("Packaging")
    public JkOptionPack pack = new JkOptionPack();

    /** Options about injected extra dependencies*/
    @JkDoc("JkExtraPacking dependencies")
    public JkOptionExtaPath extraPath = new JkOptionExtaPath();

    /** Options about publication */
    @JkDoc("Publication")
    public JkPublishOptions publication = new JkPublishOptions();

    /** Options about manifest creation */
    @JkDoc("Manifest")
    public final JkManifestOption manifest = new JkManifestOption();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected List<Class<Object>> pluginTemplateClasses() {
        final List<Class<Object>> result = new LinkedList<Class<Object>>();
        final Class clazz = JkJavaBuildPlugin.class;
        result.add(clazz);
        return result;
    }

    // --------------------------- Project settings -----------------------

    /**
     * Returns the encoding of source files for the compiler.
     */
    public String sourceEncoding() {
        return "UTF-8";
    }

    /**
     * Returns the Java source version for the compiler (as "1.4", 1.6", "7", "8", ...).
     */
    public String javaSourceVersion() {
        return JkUtilsJdk.runningJavaVersion();
    }

    /**
     * Returns the Java target version for the compiler (as "1.4", 1.6", "7", "8", ...).
     * By default it returns the same version as {@link #javaSourceVersion()}.
     */
    public String javaTargerVersion() {
        return javaSourceVersion();
    }

    /**
     * Returns the location of production source code that has been edited
     * manually (not generated).
     */
    public JkFileTreeSet editedSources() {
        return JkFileTreeSet.of(file("src/main/java"));
    }

    /**
     * Returns the location of unit test source code that has been edited
     * manually (not generated).
     */
    public JkFileTreeSet unitTestEditedSources() {
        return JkFileTreeSet.of(file("src/test/java"));
    }

    /**
     * Returns location of production source code (containing edited + generated
     * sources).
     */
    public JkFileTreeSet sources() {
        return JkJavaBuildPlugin.applySourceDirs(this.plugins.getActives(),
                editedSources().and(generatedSourceDir()));
    }

    /**
     * Returns the location of production resources that has been edited
     * manually (not generated).
     */
    public JkFileTreeSet editedResources() {
        return JkFileTreeSet.of(file("src/main/resources"));
    }

    /**
     * Returns location of production resources.
     */
    public JkFileTreeSet resources() {
        final JkFileTreeSet original = sources().andFilter(RESOURCE_FILTER).and(editedResources())
                .and(generatedResourceDir());
        return JkJavaBuildPlugin.applyResourceDirs(this.plugins.getActives(), original);
    }

    /**
     * Returns location of test source code.
     */
    public JkFileTreeSet unitTestSources() {
        return JkJavaBuildPlugin.applyTestSourceDirs(this.plugins.getActives(),
                unitTestEditedSources());
    }

    /**
     * Returns location of test resources.
     */
    public JkFileTreeSet unitTestResources() {
        final JkFileTreeSet original = unitTestSources().andFilter(RESOURCE_FILTER);
        return JkJavaBuildPlugin.applyTestResourceDirs(this.plugins.getActives(), original);
    }

    /**
     * Returns location of generated sources.
     */
    public File generatedSourceDir() {
        return ouputDir("generated-sources/java");
    }

    /**
     * Returns location of generated resources.
     */
    public File generatedResourceDir() {
        return ouputDir("generated-resources");
    }

    /**
     * Returns location of generated resources for tests.
     */
    public File generatedTestResourceDir() {
        return ouputDir("generated-unitTest-resources");
    }

    /**
     * Returns location where the java production classes are compiled.
     */
    public File classDir() {
        return ouputDir().from("classes").createIfNotExist().root();
    }

    /**
     * Returns location where the test reports are written.
     */
    public File testReportDir() {
        return ouputDir("test-reports");
    }

    /**
     * Returns location where the java production classes are compiled.
     */
    public File testClassDir() {
        return ouputDir().from("testClasses").createIfNotExist().root();
    }

    // --------------------------- Configurer -----------------------------

    /**
     * Returns the compiler used to compile production code.
     */
    public JkJavaCompiler productionCompiler() {
        return JkJavaCompiler.ofOutput(classDir()).andSources(sources())
                .withClasspath(depsFor(COMPILE, PROVIDED))
                .withSourceVersion(this.javaSourceVersion())
                .withTargetVersion(this.javaTargerVersion())
                .forkedIfNeeded(JkOptions.getAll());
    }

    /**
     * Returns the compiler used to compile unit tests.
     */
    public JkJavaCompiler unitTestCompiler() {
        return JkJavaCompiler.ofOutput(testClassDir()).andSources(unitTestSources())
                .withClasspath(this.depsFor(TEST, PROVIDED).andHead(classDir()))
                .withSourceVersion(this.javaSourceVersion())
                .withTargetVersion(this.javaTargerVersion())
                .forkedIfNeeded(JkOptions.getAll());
    }

    /**
     * Returns the object used to process unit tests.
     */
    public final JkUnit unitTester() {
        return JkJavaBuildPlugin.applyUnitTester(plugins.getActives(), createUnitTester());
    }

    /**
     * Creates the object used to process unit test (compile + run).
     * You can override this method if you want modify the way to process test (output format, classes to tests, JVM agent, ...)
     */
    protected JkUnit createUnitTester() {
        final JkClasspath classpath = JkClasspath.of(this.testClassDir(), this.classDir()).and(
                this.depsFor(TEST, PROVIDED));
        final File junitReport = new File(this.testReportDir(), "junit");
        JkUnit result = JkUnit.of(classpath).withReportDir(junitReport)
                .withReport(this.tests.report).withClassesToTest(this.testClassDir());
        if (this.tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(
                    this.tests.jvmOptions);
            result = result.forked(javaProcess, true);
        }
        return result.withOutputOnConsole(this.tests.output || JkLog.verbose());
    }

    /**
     * Returns the object used to process Javadoc.
     */
    public JkJavadocMaker javadocMaker() {
        return javadocMaker(this, true, false);
    }

    /**
     * Returns the object that produces deliverable files (jar, war, sources, zip, folder, ...) for this project.
     */
    public final JkJavaPacker packer() {
        return JkJavaBuildPlugin.applyPacker(plugins.getActives(), createPacker());
    }

    /**
     * Override this method if you want to create a packager that behave a different way than the default one.
     * By providing your own packer, you can be more precise about what you want or not to be produced by the build.
     * For example you can create checksum files or produces totally project specific files.
     */
    protected JkJavaPacker createPacker() {
        return JkJavaPacker.of(this);
    }

    /**
     * Override this method if you want to use a another resource processor than the default one.
     * By returning another resource processor, you can embed extra files as resources and/or modify
     * the way interpolation is done.
     */
    protected JkResourceProcessor resourceProcessor() {
        return JkResourceProcessor.of(resources());
    }

    // --------------------------- Callable Methods -----------------------

    @Override
    protected JkScaffolder scaffolder() {
        final Runnable addFolder = new Runnable() {

            @Override
            public void run() {
                for (final JkFileTree dir : editedSources().fileTrees()) {
                    dir.root().mkdirs();
                }
                for (final JkFileTree dir : unitTestEditedSources().fileTrees()) {
                    dir.root().mkdirs();
                }
            }
        };
        return super.scaffolder().buildClassWriter(scaffoldedBuildClassCode()).extraAction(addFolder);

    }

    /**
     * Returns a {@link JkComputedDependency} that consist of the jar file produced by this build
     * plus all of its RUNTIME transitive runtime dependencies.
     */
    public JkComputedDependency asJavaDependency() {
        return this.asDependency(this.depsFor(RUNTIME).andHead(this.packer().jarFile()));
    }


    private Object scaffoldedBuildClassCode() {
        if (baseDir().file("pom.xml").exists() && JkMvn.INSTALLED) {
            JkLog.info("pom.xml detected and Maven installed : try to generate build class from existing pom.");
            try {
                return JkMvn.of(baseDir().root()).createBuildClassCode(null, "Build");
            } catch (final RuntimeException e) {
                e.printStackTrace();
                JkLog.info("Maven migration failed. Just generate standard build class.");
            }
        }
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.extendedClass = "JkJavaBuild";
        codeWriter.dependencies = JkDependencies.builder().build();
        codeWriter.imports.clear();
        codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsForJkJavaBuild());
        return codeWriter;
    }

    /** Generate sources and resources, compile production sources and process production resources to the classes directory. */
    @JkDoc("Generate sources and resources, compile production sources and process production resources to the classes directory.")
    public void compile() {
        JkLog.startln("Processing production code and resources");
        generateSources();
        productionCompiler().compile();
        generateResources();
        processResources();
        JkLog.done();
    }

    /** Compile and run all unit tests. */
    @JkDoc("Compile and run all unit tests.")
    public void unitTest() {
        this.generateUnitTestSources();
        if (!checkProcessTests(unitTestSources())) {
            return;
        }
        JkLog.startln("Process unit tests");
        unitTestCompiler().compile();
        generateUnitTestResources();
        processUnitTestResources();
        unitTester().run();
        JkLog.done();
    }

    /** Produce documents for this project (javadoc, Html site, ...) */
    @JkDoc("Produce documents for this project (javadoc, Html site, ...)")
    public void javadoc() {
        javadocMaker().process();
        signIfNeeded(javadocMaker().zipFile());
    }

    /**
     * Signs the specified files with PGP if the option
     * <code>pack.signWithPgp</code> is <code>true</code>. The signature will be
     * detached in the same folder than the signed file and will have the same
     * name but with the <i>.asc</i> suffix.
     */
    protected final void signIfNeeded(File... files) {
        if (pack.signWithPgp) {
            pgp().sign(files);
        }
    }

    /** Create many jar files containing respectively binaries, sources, test binaries and test sources. */
    @JkDoc({
        "Create many jar files containing respectively binaries, sources, test binaries and test sources.",
    "The jar containing the binary is the one that will be used as a depe,dence for other project." })
    public void pack() {
        packer().pack();
    }

    /** Method executed by default when none is specified. By default this method equals to #clean + #doPack" */
    @JkDoc("Method executed by default when none is specified. By default this method equals to #clean + #doPack")
    @Override
    public void doDefault() {
        doPack();
    }

    /** Publish the produced artifact to the defined repositories.  */
    @JkDoc({
        "Publish the produced artifact to the defined repositories. ",
    "This can work only if a 'publishable' repository has been defined and the artifact has been generated (pack method)." })
    public void publish() {
        final JkDependencies dependencies = dependencyResolver().dependenciesToResolve();
        final JkVersionProvider resolvedVersions = this.dependencyResolver()
                .resolve(this.dependencies().involvedScopes()).resolvedVersionProvider();
        if (this.publisher().hasMavenPublishRepo()) {
            final JkMavenPublication publication = mavenPublication();
            final JkDependencies deps = effectiveVersion().isSnapshot() ? dependencies
                    .resolvedWith(resolvedVersions) : dependencies;
                    this.publisher().publishMaven(versionedModule(), publication, deps);
        }
        if (this.publisher().hasIvyPublishRepo()) {
            final Date date = this.buildTime();
            this.publisher().publishIvy(versionedModule(), ivyPublication(), dependencies, COMPILE,
                    SCOPE_MAPPING, date, resolvedVersions);
        }
    }



    // ----------------------- Overridable sub-methods ---------------------

    /**
     * Override this method if you need to generate some production sources
     */
    protected void generateSources() {
        // Do nothing by default
    }

    /**
     * Override this method if you need to generate some unit test sources.
     */
    protected void generateUnitTestSources() {
        // Do nothing by default
    }

    /**
     * Override this method if you need to generate some resources.
     */
    protected void generateResources() {
        // Do nothing by default
    }

    /**
     * Override this method if you need to generate some resources for running
     * unit tests.
     */
    protected void generateUnitTestResources() {
        // Do nothing by default
    }

    /**
     * Copies the generated sources for production into the class dir. If you
     * want to do special processing (as interpolating) you should override this
     * method.
     */
    protected void processResources() {
        this.resourceProcessor().generateTo(classDir());
    }

    /**
     * Copies the generated resources for test into the test class dir. If you
     * want to do special processing (as interpolating) you should override this
     * method.
     */
    protected void processUnitTestResources() {
        JkResourceProcessor.of(unitTestResources()).andIfExist(generatedTestResourceDir())
        .generateTo(testClassDir());
    }

    private boolean checkProcessTests(JkFileTreeSet testSourceDirs) {
        if (this.tests.skip) {
            return false;
        }
        if (testSourceDirs == null || testSourceDirs.fileTrees().isEmpty()) {
            JkLog.info("No test source declared. Skip tests.");
            return false;
        }
        if (!unitTestSources().allExists()) {
            JkLog.info("No existing test source directory found : " + testSourceDirs
                    + ". Skip tests.");
            return false;
        }
        return true;
    }

    @Override
    protected JkScopeMapping scopeMapping() {
        return SCOPE_MAPPING;
    }

    @Override
    protected JkScope defaultScope() {
        return COMPILE;
    }

    /**
     * Override this method to redefine what should be published on Maven repositories.
     */
    protected JkMavenPublication mavenPublication() {
        final JkJavaPacker packer = packer();
        return JkMavenPublication
                .of(packer.jarFile())
                .andIf(publication.publishSources, packer.jarSourceFile(), "sources")
                .andOptional(javadocMaker().zipFile(), "javadoc")
                .andOptionalIf(publication.publishTests, packer.jarTestFile(), "test")
                .andOptionalIf(publication.publishTests && publication.publishSources, packer.jarTestSourceFile(),
                        "testSources");
    }

    /**
     * Override this method to redefine what should be published on Ivy repositories.
     */
    protected JkIvyPublication ivyPublication() {
        final JkJavaPacker packer = packer();
        return JkIvyPublication.of(packer.jarFile(), COMPILE)
                .andIf(publication.publishSources, packer.jarSourceFile(), "source", SOURCES)
                .andOptional(javadocMaker().zipFile(), "javadoc", JAVADOC)
                .andOptionalIf(publication.publishTests, packer.jarTestFile(), "jar", TEST)
                .andOptionalIf(publication.publishTests && publication.publishSources, packer.jarTestSourceFile(), "source", SOURCES);
    }


    /**
     * Override to include or not tests classes in the published artifacts of the project. Default is <code>false</code>
     */
    protected boolean includeTestsInPublication() {
        return false;
    }

    /**
     * Override this method to include or not sources of the project in the published artifacts. Default is <code>true</code>.
     */
    protected boolean includeSourcesInPublication() {
        return true;
    }

    // ------------------------------------

    @Override
    protected JkDependencies implicitDependencies() {
        final JkFileTree libDir = JkFileTree.of(file(STD_LIB_PATH));
        if (!libDir.root().exists()) {
            return super.implicitDependencies();
        }
        return JkDependencies.builder().usingDefaultScopes(COMPILE)
                .on(JkFileSystemDependency.of(libDir.include("*.jar", "compile/*.jar")))
                .usingDefaultScopes(PROVIDED)
                .on(JkFileSystemDependency.of(libDir.include("provided/*.jar")))
                .usingDefaultScopes(RUNTIME)
                .on(JkFileSystemDependency.of(libDir.include("runtime/*.jar")))
                .usingDefaultScopes(TEST)
                .on(JkFileSystemDependency.of(libDir.include("test/*.jar")))
                .usingDefaultScopes(COMPILE)
                .onFiles(toPath(extraPath.compile)).usingDefaultScopes(RUNTIME)
                .onFiles(toPath(extraPath.runtime)).usingDefaultScopes(TEST)
                .onFiles(toPath(extraPath.test)).usingDefaultScopes(PROVIDED)
                .onFiles(toPath(extraPath.provided))
                .build();
    }

    /**
     * Returns the manifest that will be inserted in generated jars. Override it
     * if you want to add extra info.
     */
    protected JkManifest jarManifest() {
        final JkManifest result = JkManifest.ofClassDir(this.classDir());

        // Include Main-Class attribute if needed
        if (this.manifest.isAuto()) {
            final String mainClassName = JkClassLoader.findMainClass(this.classDir());
            if (mainClassName != null) {
                result.addMainClass(mainClassName);
            } else {
                throw new JkException("No class with main method found.");
            }
        } else if (!JkUtilsString.isBlank(manifest.mainClass)) {
            result.addMainClass(manifest.mainClass);
        }

        return result;
    }

    // ----------------- Lifecycle methods

    /** Lifecycle method :#compile. As doCompile is the first stage, this is equals to #compile */
    @JkDoc("Lifecycle method :#compile. As doCompile is the first stage, this is equals to #compile")
    public void doCompile() {
        this.clean();
        this.compile();
    }

    /** Lifecycle method : #doCompile + #unitTest */
    @JkDoc("Lifecycle method : #doCompile + #unitTest")
    public void doUnitTest() {
        this.doCompile();
        this.unitTest();
    }

    /** Lifecycle method : #doUnitTest + #pack */
    @JkDoc("Lifecycle method : #doUnitTest + #pack")
    public void doPack() {
        doUnitTest();
        pack();
    }

    /** Lifecycle method : #doUnitTest + #pack */
    @JkDoc("Lifecycle method : #doUnitTest + #pack")
    public void doVerify() {
        doPack();
        verify();
    }

    /** Lifecycle method : #doVerify + #publish */
    @JkDoc("Lifecycle method : #doVerify + #publish")
    public void doPublish() {
        doVerify();
        publish();
    }

    /**
     * Options about extra path
     */
    public static class JkOptionExtaPath {

        @JkDoc({
            "provided scope : these libs will be added to the compile path but won't be embedded in war files or fat jars.",
        "Example : -extraPath.provided=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
        private String provided;

        @JkDoc({ "runtime scope : these libs will be added to the runtime path.",
        "Example : -extraPath.runtime=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
        private String runtime;

        @JkDoc({ "compile scope : these libs will be added to the compile and runtime path.",
        "Example : -extraPath.compile=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
        private String compile;

        @JkDoc({ "test scope : these libs will be added to the compile and runtime path.",
        "Example : -extraPath.test=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
        private String test;

        /** Injected extra dependency path for the 'provided' scope */
        public String provided() {
            return provided;
        }

        /** Injected extra dependency path for the 'runtime' scope */
        public String runtime() {
            return runtime;
        }

        /** Injected extra dependency path for the 'compile' scope */
        public String compile() {
            return compile;
        }

        /** Injected extra dependency path for the 'test' scope */
        public String test() {
            return test;
        }

    }

    /**
     * Options about tests
     */
    public final static class JkOptionTest {

        /** Turn it on to skip tests. */
        @JkDoc("Turn it on to skip tests.")
        public boolean skip;

        /** Turn it on to run tests in a forked process. */
        @JkDoc("Turn it on to run tests in a forked process.")
        public boolean fork;

        /** Argument passed to the JVM if tests are forked. Example : -Xms2G -Xmx2G */
        @JkDoc("Argument passed to the JVM if tests are forked. Example : -Xms2G -Xmx2G")
        public String jvmOptions;

        /** Detail level for the test report */
        @JkDoc({ "The more details the longer tests take to be processed.",
            "BASIC mention the total time elapsed along detail on failed tests.",
            "FULL detailed report displays additionally the time to run each tests.",
        "Example : -report=NONE" })
        public JunitReportDetail report = JunitReportDetail.BASIC;

        /** Turn it on to display System.out and System.err on console while executing tests.*/
        @JkDoc("Turn it on to display System.out and System.err on console while executing tests.")
        public boolean output;

    }

    /**
     * Options about archive packaging
     */
    public static final class JkOptionPack {

        /** When true, produce a fat-jar, meaning a jar embedding all the dependencies. */
        @JkDoc("When true, produce a fat-jar, meaning a jar embedding all the dependencies.")
        public boolean fatJar;

        /** When true, the produced artifacts are signed with PGP */
        @JkDoc("When true, the produced artifacts are signed with PGP.")
        public boolean signWithPgp;

        /** When true, tests classes and sources are packed in jars.*/
        @JkDoc("When true, tests classes and sources are packed in jars.")
        public boolean tests;

        /** Comma separated list of algorithm to use to produce checksums (ex : 'sha-1,md5'). */
        @JkDoc("Comma separated list of algorithm to use to produce checksums (ex : 'sha-1,md5').")
        public String checksums;

        /** When true, javadoc is created and packed in a jar file.*/
        @JkDoc("When true, javadoc is created and packed in a jar file.")
        public boolean javadoc;

        /**
         * Gives the suffix that will be appended at the end of the 'normal' jar for naming the fat jar.
         * If the name of the normal jar is <i>mylib.jar</i> and the suffix is <i>uber</i> then the fat jar
         * file name will be <i>mylib-uber.jar</i>.
         */
        @JkDoc({"Gives the suffix that will be appended at the end of the 'normal' jar for naming the fat jar.",
            "If the name of the normal jar is 'mylib.jar' and the suffix is 'uber' then the fat jar",
            "file name will be 'mylib-uber.jar'."
        })
        public String fatJarSuffix = "fat";

    }

    private static JkJavadocMaker javadocMaker(JkJavaBuild javaBuild, boolean fullName,
            boolean includeVersion) {
        String name = fullName ? javaBuild.moduleId().toString() : javaBuild.moduleId().toString();
        if (includeVersion) {
            name = name + "-" + javaBuild.effectiveVersion().name();
        }
        name = name + "-javadoc";
        return JkJavadocMaker.of(javaBuild.sources(), javaBuild.ouputDir(name),
                javaBuild.ouputDir(name + ".jar")).withClasspath(
                        javaBuild.depsFor(JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED));
    }

    /**
     * Options about producing the manifest.
     *
     */
    public static class JkManifestOption {

        /** The mainClass value to specify that the main class should be discovered automatically */
        public static final String AUTO = "auto";

        /**
         * The 'Main-Class' attribute value to inject in the packaged manifest.<br/>
         * If this filed is null, then this attribute is not injected.<br/>
         * If this filed is 'auto', then the 'Main-Class' attribute is set with the first compiled class found having a main method.<br/>
         */
        @JkDoc({"The 'Main-Class' attribute value to inject in the packaged manifest.",
            "If this filed is null, then this attribute is not injected.",
        "If this filed is 'auto', then the 'Main-Class' attribute is set with the first compiled class found having a main method." })
        public String mainClass;

        boolean isAuto() {
            return AUTO.equals(mainClass);
        }
    }


    /**
     * Options about publications.
     */
    public static class JkPublishOptions {

        /** Tell if the sources must be published. Default is true. */
        @JkDoc("Tell if the sources must be published")
        public boolean publishSources = true;

        /** Tell if the test classes must be published. Default is false. */
        @JkDoc("Tell if the test classes must be published")
        public boolean publishTests = false;

    }

}
