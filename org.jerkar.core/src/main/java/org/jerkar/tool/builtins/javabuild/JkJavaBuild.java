package org.jerkar.tool.builtins.javabuild;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkFileSystemDependency;
import org.jerkar.api.depmanagement.JkIvyPublication;
import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaCompilerSpec;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkUnit;
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
 *
 * @deprecated Replaced by {@link JkJavaProjectBuild}
 */
@Deprecated
public class JkJavaBuild extends JkBuildDependencySupport {

    /**
     * A dependency declared with this scope will be available at compile time but won't be part of the packaged
     * product (similar to Maven scope 'provided'.
     */
    public static final JkScope PROVIDED = JkScope.build("provided")
            .transitive(false)
            .descr("Dependencies to compile the project but that should not be embedded in produced artifacts.").build();

    /**
     * A dependency declared with this scope will be available in all classpaths (compiling, testing, running and packaging the product).<p>
     *
     * A dependency resolution made with this scope will only fetch dependencies declared with {@link #COMPILE} scope and
     * transitive dependencies declared with {@link #COMPILE } scope as well. <p>>
     *
     * <b>CAUTION :</b> When resolving {@link #RUNTIME} dependencies, transitive 'runtime' dependencies won't be fetched if
     * it's coming to a 'compile' one. <b/>
     * In such it differs to Maven 'compile' scope (resolving 'runtime' in Maven will fetch transitive 'runtime' dependencies coming to 'compile' ones).<b/>
     * If you want to have a dependency scope equivalent to Maven 'compile', you need to declare dependencies with
     * two scopes : {@link #COMPILE} and {@link #RUNTIME} or their shorthand {@link #COMPILE_AND_RUNTIME}.
     */
    public static final JkScope COMPILE = JkScope.build("compile").descr(
            "Dependencies to compile the project.").build();

    /**
     * A dependency declared with this scope will be present in the classpath for packaging or running the module.<b/>
     * If it is a library, dependencies will be included in the fat jar.<b/>
     * If it is a war, dependencies will be included in war file. <b/>
     * If it is a main application, dependencies will be part of the runtime classpath.<p>
     *
     * A dependency resolution made with this scope will fetch dependencies declared with {@link #COMPILE} or {@link #RUNTIME}
     * plus their transitive dependencies declared with {@link #COMPILE } or {@link #RUNTIME}.
     */
    public static final JkScope RUNTIME = JkScope.build("runtime").extending(COMPILE)
            .descr("Dependencies to embed in produced artifacts (as war or fat jar * files).").build();

    /**
     * A dependency declared with this scope will be present in testing classpath only.
     *
     * A dependency resolution made with this scope will fetch dependencies declared with {@link #COMPILE}, {@link #RUNTIME} or  {@link #TEST}
     * plus their transitive dependencies declared with {@link #COMPILE }, {@link #RUNTIME} or {@link #TEST}.
     */
    public static final JkScope TEST = JkScope.build("test").extending(RUNTIME, PROVIDED)
            .descr("Dependencies necessary to compile and run tests.").build();

    /** This scope is used for publication purpose */
    public static final JkScope SOURCES = JkScope.build("sources").transitive(false)
            .descr("Contains the source artifacts.").build();

    /** This scope is used for publication purpose */
    public static final JkScope JAVADOC = JkScope.build("javadoc").transitive(false)
            .descr("Contains the javadoc of this project").build();

    /**
     * Shorthand to declare both COMPILE and RUNTIME scope at once. This is the default scope for dependencies.
     * It is equivalent to Maven 'compile'.
     */
    public static final JkScope[] COMPILE_AND_RUNTIME = new JkScope[] {COMPILE, RUNTIME};

    private static final String ARCHIVE_MASTER = "archives(master)";

    public static final JkScopeMapping DEFAULT_SCOPE_MAPPING = JkScopeMapping
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
    public JkTestOptions tests = new JkTestOptions();

    /**
     * Options about packaging jars. This object will be used to populate the default {@link JkJavaPacker} for this build.
     * You can override this default setting or set more detailed setting overriding {@link #createPacker()} method.
     */
    @JkDoc("Packaging")
    public JkOptionPack pack = new JkOptionPack();

    /** Options about injected extra dependencies*/
    @JkDoc("JkExtraPacking dependencies")
    public JkOptionExtaPath extraPath = new JkOptionExtaPath();


    /** Options about manifest creation */
    @JkDoc("Manifest")
    public final JkManifestOption manifest = new JkManifestOption();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Class<Object>> pluginTemplateClasses() {
        final List<Class<Object>> result = new LinkedList<>();
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
     * You can use constants defined in #JkJavaCompiler.
     */
    public String javaSourceVersion() {
        return JkUtilsJdk.runningJavaVersion();
    }

    /**
     * Returns the Java target version for the compiler (as "1.4", 1.6", "7", "8", ...).
     * By default it returns the same version as {@link #javaSourceVersion()}.
     */
    public String javaTargetVersion() {
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
        return editedSources().and(generatedSourceDir());
        //return JkJavaBuildPlugin.applySourceDirs(this.plugins.getActivated(),
        //        editedSources().and(generatedSourceDir()));
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
        return original;
        //return JkJavaBuildPlugin.applyResourceDirs(this.plugins.getActivated(), original);
    }

    /**
     * Returns location of test source code.
     */
    public JkFileTreeSet unitTestSources() {
        return unitTestEditedSources();
        // return JkJavaBuildPlugin.applyTestSourceDirs(this.plugins.getActivated(),
        //        unitTestEditedSources());
    }

    /**
     * Returns location of test resources.
     */
    public JkFileTreeSet unitTestResources() {
        final JkFileTreeSet original = unitTestSources().andFilter(RESOURCE_FILTER);
        return original;
        //return JkJavaBuildPlugin.applyTestResourceDirs(this.plugins.getActivated(), original);
    }

    /**
     * Returns location of generated sources.
     */
    public File generatedSourceDir() {
        return ouputFile("generated-sources/java");
    }

    /**
     * Returns location of generated resources.
     */
    public File generatedResourceDir() {
        return ouputFile("generated-resources");
    }

    /**
     * Returns location of generated resources for tests.
     */
    public File generatedTestResourceDir() {
        return ouputFile("generated-unitTest-resources");
    }

    /**
     * Returns location where the java production classes are compiled.
     */
    public File classDir() {
        return ouputTree().go("classes").createIfNotExist().root();
    }

    /**
     * Returns location where the test reports are written.
     */
    public File testReportDir() {
        return ouputFile("test-reports");
    }

    /**
     * Returns location where the java production classes are compiled.
     */
    public File testClassDir() {
        return ouputTree().go("test-classes").createIfNotExist().root();
    }

    // --------------------------- Configurer -----------------------------

    /**
     * Returns the compiler used to compile production code.
     */
    public JkJavaCompiler productionCompiler() {
        final JkJavaCompilerSpec spec = JkJavaCompilerSpec.of().withEncoding(this.sourceEncoding())
                .withSourceVersion(JkJavaVersion.name(this.javaSourceVersion()))
                .withTargetVersion(JkJavaVersion.name(this.javaTargetVersion()));
        return JkJavaCompiler.outputtingIn(classDir()).andSources(sources())
                .withClasspath(depsFor(COMPILE, PROVIDED))
                .andOptions(spec.asOptions())
                .forkedIfNeeded(JkOptions.getAll());
    }

    /**
     * Returns the compiler used to compile unit tests.
     */
    public JkJavaCompiler unitTestCompiler() {
        final JkJavaCompilerSpec spec = JkJavaCompilerSpec.of().withEncoding(this.sourceEncoding())
                .withSourceVersion(JkJavaVersion.name(this.javaSourceVersion()))
                .withTargetVersion(JkJavaVersion.name(this.javaTargetVersion()));
        return JkJavaCompiler.outputtingIn(testClassDir()).andSources(unitTestSources())
                .withClasspath(this.depsFor(TEST, PROVIDED).andHead(classDir()))
                .andOptions(spec.asOptions())
                .forkedIfNeeded(JkOptions.getAll());
    }

    /**
     * Returns the object used to process unit tests.
     */
    public final JkUnit unitTester() {
        return createUnitTester();
        //return JkJavaBuildPlugin.applyUnitTester(plugins.getActivated(), createUnitTester());
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
            result = result.forked(javaProcess);
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
        return createPacker();
        // return JkJavaBuildPlugin.applyPacker(plugins.getActivated(), createPacker());
    }

    /**
     * Override this method if you want to create a packager that behave a different way than the default one.<br/>
     * This will override setting set through {@link JkOptionPack}.<br/>
     * By providing your own packer, you can be more precise about what you want or not to be produced by the build.
     * For example you can create checksum files or specify actions producing/modifying files to package.
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
    public JkScaffolder createScaffolder() {
        final Runnable addFolder = () -> {
            for (final JkFileTree dir : editedSources().fileTrees()) {
                dir.root().mkdirs();
                final String packageName = JkUtilsString.conformPackageName(moduleId().fullName());
                final String path = packageName.replace('.', '/');
                dir.file(path).mkdirs();
            }
            for (final JkFileTree dir : unitTestEditedSources().fileTrees()) {
                dir.root().mkdirs();
            }
        };
        scaffoldedBuildClassCode();
        super.scaffolder().buildClassWriter(scaffoldedBuildClassCode());
        super.scaffolder().extraActions.chain(addFolder);
        return super.scaffolder();
    }

    /**
     * Returns a {@link JkComputedDependency} that consist of the jar file produced by this build
     * plus all of its transitive runtime dependencies.
     */
    public JkComputedDependency asJavaDependency() {
        return this.asDependency(this.depsFor(RUNTIME).andHead(this.packer().jarFile()));
    }


    private Supplier<String> scaffoldedBuildClassCode() {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        if (baseTree().file("pom.xml").exists() && JkMvn.INSTALLED) {
            JkLog.info("pom.xml detected and Maven installed : try to generate build class to existing pom.");
            try {
                return () -> JkMvn.of(baseTree().root()).createBuildClassCode(null, "Build", baseTree());
            } catch (final RuntimeException e) {
                e.printStackTrace();
                JkLog.info("Maven migration failed. Just generate standard build class.");
            }
        }

        codeWriter.extendedClass = "JkJavaBuild";
        codeWriter.dependencies = JkDependencies.builder().on(JkPopularModules.JUNIT, "4.11").scope(TEST).build();
        codeWriter.imports.clear();
        codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsForJkJavaBuild());
        codeWriter.staticImports.addAll(JkCodeWriterForBuildClass.staticImportsForJkJavaBuild());

        return codeWriter;
    }

    /** Generates sources and resources, compiles production sources and process production resources to the class directory. */
    @JkDoc("Generates sources and resources, compiles production sources and processes production resources to the class directory.")
    public void compile() {
        JkLog.startln("Processing production code and resources");
        //  JkJavaBuildPlugin.applyPriorCompile(this.plugins.getActivated());
        generateSources();
        productionCompiler().compile();
        generateResources();
        processResources();
        JkLog.done();
    }

    /** Compiles and runs all unit tests. */
    @JkDoc("Compiles and runs all unit tests.")
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

    /** Produces documents for this project (javadoc, Html site, ...) */
    @JkDoc("Produces documents for this project (javadoc, Html site, ...)")
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

    /** Creates many jar files containing respectively binaries, sources, test binaries and test sources. */
    @JkDoc({
        "Creates many jar files containing respectively binaries, sources, test binaries and test sources.",
    "The jar containing the binary is the one that will be used as a depe,dence for other project." })
    public void pack() {
        packer().pack();
    }

    /** Method executed by default when none is specified. By default this method equals to #deleteArtifacts + #doPack" */
    @JkDoc("Method executed by default when none is specified. By default this method equals to #deleteArtifacts + #doPack")
    @Override
    public void doDefault() {
        doPack();
    }

    /** Publishes the produced artifact to the defined repositories.  */
    @JkDoc({
        "Publishes the produced artifact to the defined repositories. ",
    "This can work only if a 'publishable' repository has been defined and the artifact has been generated (pack method)." })
    public void publish() {
        final JkDependencies dependencies = this.dependencies();
        final JkVersionProvider resolvedVersions = this.dependencyResolver()
                .resolve(dependencies, dependencies.involvedScopes()).resolvedVersionProvider();
        if (this.publisher().hasMavenPublishRepo()) {
            final JkMavenPublication publication = mavenPublication();
            final JkDependencies deps = effectiveVersion().isSnapshot() ? dependencies
                    .resolvedWith(resolvedVersions) : dependencies;
                    this.publisher().publishMaven(versionedModule(), publication, deps);
        }
        if (this.publisher().hasIvyPublishRepo()) {
            this.publisher().publishIvy(versionedModule(), ivyPublication(), dependencies, COMPILE,
                    DEFAULT_SCOPE_MAPPING, buildTime(), resolvedVersions);
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
        return DEFAULT_SCOPE_MAPPING;
    }

    @Override
    protected JkScope[] defaultScope() {
        return COMPILE_AND_RUNTIME;
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
     * Returns the specified relative path to this project as a {@link JkPath} instance.
     */
    protected final JkPath toPath(String pathAsString) {
        if (pathAsString == null) {
            return JkPath.of();
        }
        return JkPath.of(baseTree().root(), pathAsString);
    }

    /**
     * Returns the manifest that will be inserted in generated jars. Override it
     * if you want to add extra infoString.
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
     * Options about archive packaging.
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
            "file name will be 'mylib-uber.jar'.",
            "if suffix is null or empty, than fat jar will have a suffix less name and normal jar, will be suffixed by '-original'"
        })
        public String fatJarSuffix = "fat";



    }

    private static JkJavadocMaker javadocMaker(JkJavaBuild javaBuild, boolean fullName,
            boolean includeVersion) {
        String name = fullName ? javaBuild.moduleId().fullName() : javaBuild.moduleId().name();
        if (includeVersion) {
            name = name + "-" + javaBuild.effectiveVersion().name();
        }
        name = name + "-javadoc";
        return JkJavadocMaker.of(javaBuild.sources(), javaBuild.ouputFile(name),
                javaBuild.ouputFile(name + ".jar")).withClasspath(
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

    @Override
    public String infoString() {
        final String builder = super.infoString() + "\n\n" +
                "source dirs : " + this.sources() + "\n" +
                "test dirs: " + this.unitTestSources() + "\n" +
                "java source version : " + this.javaSourceVersion() + "\n" +
                "java target version : " + this.javaTargetVersion();
        return builder;
    }




}
