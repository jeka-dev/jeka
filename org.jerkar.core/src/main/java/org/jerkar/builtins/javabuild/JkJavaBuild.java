package org.jerkar.builtins.javabuild;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.jerkar.JkBuildDependencySupport;
import org.jerkar.JkClasspath;
import org.jerkar.JkDoc;
import org.jerkar.JkFileFilter;
import org.jerkar.JkFileTree;
import org.jerkar.JkFileTreeSet;
import org.jerkar.JkJavaCompiler;
import org.jerkar.JkLog;
import org.jerkar.JkOption;
import org.jerkar.JkScaffolder;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit.JunitReportDetail;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkDependency;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.publishing.JkIvyPublication;
import org.jerkar.publishing.JkMavenPublication;
import org.jerkar.utils.JkUtilsIterable;
import org.jerkar.utils.JkUtilsJdk;

/**
 * Template class to define build on Java project.
 * This template is flexible enough to handle exotic project structure as it proposes
 * to override default setting at different level of granularity.<b/>
 * Beside this template define a set of "standard" scope to define dependencies.
 * You are not forced to use it strictly but it can simplify dependency management to follow a given standard.
 * 
 * @author Jerome Angibaud
 */
public class JkJavaBuild extends JkBuildDependencySupport {

	public static final JkScope PROVIDED = JkScope.of("provided").transitive(false)
			.descr("Dependencies to compile the project but that should not be embedded in produced artifacts.");

	public static final JkScope COMPILE = JkScope.of("compile")
			.descr("Dependencies to compile the project.");

	public static final JkScope RUNTIME = JkScope.of("runtime").extending(COMPILE)
			.descr("Dependencies to embed in produced artifacts (as war or fat jar * files).");

	public static final JkScope TEST = JkScope.of("test").extending(RUNTIME, PROVIDED)
			.descr("Dependencies necessary to compile and run tests.");

	public static final JkScope SOURCES = JkScope.of("sources").transitive(false)
			.descr("Contains the source artefacts");

	public static final JkScope JAVADOC = JkScope.of("javadoc").transitive(false)
			.descr("Contains the javadoc of this project");

	private static final JkScopeMapping SCOPE_MAPPING = JkScopeMapping
			.of(COMPILE).to("archives(master)", COMPILE.name())
			.and(PROVIDED).to("archives(master)", COMPILE.name())
			.and(RUNTIME).to("archives(master)", RUNTIME.name())
			.and(TEST).to("archives(master)", RUNTIME.name(), "test(master)");


	/**
	 * Filter to excludes everything in a java source directory which are not resources.
	 */
	public static final JkFileFilter RESOURCE_FILTER = JkFileFilter
			.exclude("**/*.java").andExclude("**/package.html")
			.andExclude("**/doc-files");

	@JkOption({
		"Mention if you want to add extra lib in your 'compile' scope but not in your 'runtime' scope. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile path but won't be embedded in war files or fat jars.",
	"Example : -extraProvidedPath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraProvidedPath;

	@JkOption({
		"Mention if you want to add extra lib in your 'runtime' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the runtime path.",
	"Example : -extraRuntimePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraRuntimePath;

	@JkOption({
		"Mention if you want to add extra lib in your 'compile' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile and runtime path.",
	"Example : -extraCompilePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraCompilePath;

	@JkOption({
		"Mention if you want to add extra lib in your 'test' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile and runtime path.",
	"Example : -extraTestPath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraTestPath;

	@JkOption("Turn it on to skip tests.")
	public boolean skipTests;

	@JkOption("When true, unit tests are run in a forked process.")
	public boolean forkTests;

	@JkOption({"The more details the longer tests take to be processed.",
		"BASIC mention the total time elapsed along detail on failed tests.",
		"FULL detailed report displays additionally the time to run each tests.",
	"Example : -junitReportDetail=NONE"})
	public JunitReportDetail junitReportDetail = JunitReportDetail.BASIC;

	@JkOption("When true, produce a fat-jar, meaning a jar embedding all the dependencies.")
	public boolean fatJar;

	@Override
	protected List<Class<Object>> pluginTemplateClasses() {
		return JkUtilsIterable.listOfGeneric(JkJavaBuildPlugin.class);
	}

	// --------------------------- Project settings -----------------------

	/**
	 * Returns the encoding of source files for the compiler.
	 */
	public String sourceEncoding() {
		return "UTF-8";
	}

	/**
	 * Returns the level of detail, junit report is supposed produced.
	 * This level is set using by changing the junitReportDetail property.
	 */
	public final JunitReportDetail junitReportDetail() {
		return junitReportDetail;
	}

	/**
	 * Returns the Java source version for the compiler (as "1.4", 1.6", "7", ...).
	 */
	public String sourceJavaVersion() {
		return JkUtilsJdk.runningJavaVersion();
	}

	/**
	 * Returns the Java target version for the compiler (as "1.4", 1.6", "7", ...).
	 */
	public String targetJavaVersion() {
		return sourceJavaVersion();
	}

	/**
	 * Returns the location of production source code that has been edited manually (not generated).
	 */
	public JkFileTreeSet editedSourceDirs() {
		return JkFileTreeSet.of(baseDir("src/main/java"));
	}

	/**
	 * Returns location of production source code.
	 */
	public JkFileTreeSet sourceDirs() {
		return JkJavaBuildPlugin.applySourceDirs(this.plugins.getActives(),
				editedSourceDirs().and(generatedSourceDir()));
	}

	/**
	 * Returns the location of production resources that has been edited manually (not generated).
	 */
	public JkFileTreeSet editedResourceDirs() {
		return JkFileTreeSet.of(baseDir("src/main/resources"));
	}

	/**
	 * Returns location of production resources.
	 */
	public JkFileTreeSet resourceDirs() {
		final JkFileTreeSet original = sourceDirs().andFilter(RESOURCE_FILTER).and(
				editedResourceDirs()).and(generatedResourceDir());
		return JkJavaBuildPlugin.applyResourceDirs(this.plugins.getActives(), original);
	}

	/**
	 * Returns location of test source code.
	 */
	public JkFileTreeSet testSourceDirs() {
		final JkFileTreeSet original =  JkFileTreeSet.of(baseDir("src/test/java"));
		return JkJavaBuildPlugin.applyTestSourceDirs(this.plugins.getActives(), original);
	}

	/**
	 * Returns location of test resources.
	 */
	public JkFileTreeSet testResourceDirs() {
		final JkFileTreeSet original = JkFileTreeSet.of(baseDir("src/test/resources")).and(
				testSourceDirs().andFilter(RESOURCE_FILTER));
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
		return ouputDir("generated-test-resources");
	}

	/**
	 * Returns location where the java production classes are compiled.
	 */
	public File classDir() {
		return ouputDir().sub("classes").createIfNotExist().root();
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
		return ouputDir().sub("testClasses").createIfNotExist().root();
	}

	// --------------------------- Configurer -----------------------------

	public JkJavaCompiler productionCompiler() {
		return JkJavaCompiler.ofOutput(classDir())
				.andSources(sourceDirs())
				.withClasspath(depsFor(COMPILE, PROVIDED))
				.withSourceVersion(this.sourceJavaVersion())
				.withTargetVersion(this.targetJavaVersion());
	}

	public JkJavaCompiler unitTestCompiler() {
		return JkJavaCompiler.ofOutput(testClassDir())
				.andSources(testSourceDirs())
				.withClasspath(this.depsFor(TEST, PROVIDED).andHead(classDir()))
				.withSourceVersion(this.sourceJavaVersion())
				.withTargetVersion(this.targetJavaVersion());
	}

	public final JkUnit unitTester() {
		return JkJavaBuildPlugin.applyUnitTester(plugins.getActives(), createUnitTester());
	}

	protected JkUnit createUnitTester() {
		final JkClasspath classpath = JkClasspath.of(this.testClassDir(), this.classDir())
				.and(this.depsFor(TEST, PROVIDED));
		final File junitReport = new File(this.testReportDir(), "junit");
		final JkUnit result = JkUnit.of(classpath)
				.withReportDir(junitReport)
				.withReport(this.junitReportDetail)
				.withClassesToTest(this.testClassDir())
				.forked(forkTests);
		return result;
	}

	public JkJavadocMaker javadocMaker() {
		return JkJavadocMaker.of(this,  true, false);
	}

	public final JkJavaPacker packer() {
		return JkJavaBuildPlugin.applyPacker(plugins.getActives(), createPacker());
	}

	protected JkJavaPacker createPacker() {
		return JkJavaPacker.of(this);
	}

	protected JkResourceProcessor resourceProcessor() {
		return JkResourceProcessor.of(resourceDirs());
	}

	// --------------------------- Callable Methods -----------------------



	@Override
	protected JkScaffolder scaffolder() {
		final Runnable action = new Runnable() {

			@Override
			public void run() {
				for (final JkFileTree dir : editedSourceDirs().jkFileTrees()) {
					dir.root().mkdirs();
				}
				for (final JkFileTree dir : testSourceDirs().jkFileTrees()) {
					dir.root().mkdirs();
				}
			}
		};
		return super.scaffolder().withExtraAction(action)
				.withExtendedClass(JkJavaBuild.class);
	}

	@JkDoc("Generate sources and resources, compile production sources and process production resources to the classes directory.")
	public void compile() {
		JkLog.startln("Processing production code and resources");
		generateSources();
		productionCompiler().compile();
		generateResources();
		processResources();
		JkLog.done();
	}

	@JkDoc("Compile and run all unit tests.")
	public void unitTest() {
		if (!checkProcessTests(testSourceDirs())) {
			return;
		}
		JkLog.startln("Process unit tests");
		unitTestCompiler().compile();
		processUnitTestResources();
		unitTester().run();
		JkLog.done();
	}

	@JkDoc("Produce documents for this project (javadoc, Html site, ...)")
	public void doc() {
		javadocMaker().process();
	}



	@JkDoc({	"Create many jar files containing respectively binaries, sources, test binaries and test sources.",
	"The jar containing the binary is the one that will be used as a depe,dence for other project."})
	public void pack() {
		packer().pack();
	}

	@JkDoc("Compile production code and resources, compile test code and resources, launch the unit tests and create artifacts.")
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		unitTest();
		pack();
	}

	@JkDoc({"Publish the produced artifact to the defined repositories. ",
	"This can work only if a 'publishable' repository has been defined and the artifact has been generated (pack method)."})
	public void publish() {
		final Date date = this.buildTime();
		if (this.publisher().hasMavenPublishRepo()) {
			this.publisher().publishMaven(module(), mavenPublication(), dependencyResolver().declaredDependencies(), date);
		}
		if (this.publisher().hasIvyPublishRepo()) {
			this.publisher().publishIvy(module(), ivyPublication(), dependencyResolver().declaredDependencies(), COMPILE, SCOPE_MAPPING, date);
		}
	}


	// ----------------------- Overridable sub-methods ---------------------

	/**
	 * Override this method if you need to
	 */
	protected void generateSources() {
		// Do nothing by default
	}

	@JkDoc("Generate files to be taken as resources.  Do nothing by default.")
	protected void generateResources() {
		// Do nothing by default
	}

	protected void processResources() {
		this.resourceProcessor().generateTo(classDir());
	}

	protected void processUnitTestResources() {
		JkResourceProcessor.of(testResourceDirs()).andIfExist(generatedTestResourceDir()).generateTo(testClassDir());
	}

	protected boolean checkProcessTests(JkFileTreeSet testSourceDirs) {
		if (skipTests) {
			return false;
		}
		if (testSourceDirs == null || testSourceDirs.jkFileTrees().isEmpty()) {
			JkLog.info("No test source declared. Skip tests.");
			return false;
		}
		if (!testSourceDirs().allExists()) {
			JkLog.info("No existing test source directory found : " + testSourceDirs +". Skip tests.");
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

	protected JkMavenPublication mavenPublication(boolean includeTests, boolean includeSources) {
		final JkJavaPacker packer = packer();
		return JkMavenPublication.of(this.moduleId().name() ,packer.jarFile())
				.andIf(includeSources, packer.jarSourceFile(), "sources")
				.andOptional(javadocMaker().zipFile(), "javadoc")
				.andOptionalIf(includeTests, packer.jarTestFile(), "test")
				.andOptionalIf(includeTests && includeSources, packer.jarTestSourceFile(), "testSources");
	}

	protected JkIvyPublication ivyPublication(boolean includeTests, boolean includeSources) {
		final JkJavaPacker packer = packer();
		return JkIvyPublication.of(packer.jarFile(), COMPILE)
				.andIf(includeSources, packer.jarSourceFile(), "source", SOURCES)
				.andOptional(javadocMaker().zipFile(), "javadoc", JAVADOC)
				.andOptionalIf(includeTests, packer.jarTestFile(), "jar", TEST)
				.andOptionalIf(includeTests, packer.jarTestSourceFile(), "source", SOURCES);
	}

	protected JkIvyPublication ivyPublication() {
		return ivyPublication(includeTestsInPublication(), includeSourcesInPublication());
	}

	protected JkMavenPublication mavenPublication() {
		return mavenPublication(includeTestsInPublication(), includeSourcesInPublication());
	}

	protected boolean includeTestsInPublication() {
		return false;
	}

	protected boolean includeSourcesInPublication() {
		return true;
	}



	// ------------------------------------

	public static void main(String[] args) {
		new JkJavaBuild().doDefault();
	}

	@Override
	protected JkDependencies extraCommandLineDeps() {
		return JkDependencies.builder()
				.usingDefaultScopes(COMPILE).onFiles(toPath(extraCompilePath))
				.usingDefaultScopes(RUNTIME).onFiles(toPath(extraRuntimePath))
				.usingDefaultScopes(TEST).onFiles(toPath(extraTestPath))
				.usingDefaultScopes(PROVIDED).onFiles(toPath(extraProvidedPath)).build();
	}

	@Override
	protected JkDependencies implicitDependencies() {
		final JkFileTree libDir = JkFileTree.of(baseDir(STD_LIB_PATH));
		if (!libDir.root().exists()) {
			return super.implicitDependencies();
		}
		return JkDependencies.builder()
				.usingDefaultScopes(COMPILE).on(JkDependency.of(libDir.include("*.jar", "compile/*.jar")))
				.usingDefaultScopes(PROVIDED).on(JkDependency.of(libDir.include("provided/*.jar")))
				.usingDefaultScopes(RUNTIME).on(JkDependency.of(libDir.include("runtime/*.jar")))
				.usingDefaultScopes(TEST).on(JkDependency.of(libDir.include("test/*.jar"))).build();
	}

}
