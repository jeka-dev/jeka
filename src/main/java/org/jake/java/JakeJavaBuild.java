package org.jake.java;

import java.io.File;
import java.net.URLClassLoader;

import org.jake.JakeBaseBuild;
import org.jake.Notifier;
import org.jake.file.DirViews;
import org.jake.file.Filter;
import org.jake.file.Zip;
import org.jake.file.utils.FileUtils;
import org.jake.java.utils.ClassloaderUtils;
import org.jake.java.utils.TestUtils;
import org.jake.utils.IterableUtils;

public class JakeJavaBuild extends JakeBaseBuild {
	
	protected static final Filter JAVA_SOURCE_ONLY_FILTER = Filter.include("**/*.java");
	
	protected static final Filter RESOURCE_FILTER = Filter.exclude("**/*.java")
			.andExcludeAll("**/package.html").andExcludeAll("**/doc-files");
	
	/**
	 * Returns location of production source code.
	 */
	protected DirViews sourceDirs() {
		return DirViews.of( baseDir().sub("src/main/java") );
	}
	
	/**
	 * Returns location of production resources.
	 */
	protected DirViews resourceDirs() {
		return sourceDirs().withFilter(RESOURCE_FILTER).and(baseDir().sub("src/main/resources"));
	} 
	
	/**
	 * Returns location of test source code.
	 */
	protected DirViews testSourceDirs() {
		return DirViews.of( baseDir().sub("src/test/java") );
	}
	
	/**
	 * Returns location of test resources.
	 */
	protected DirViews testResourceDirs() {
		return DirViews.of(baseDir().sub("src/test/resources"))
				.and(testSourceDirs().withFilter(RESOURCE_FILTER));
	} 
		
		
	protected File classDir() {
		return buildOuputDir().sub("classes").createIfNotExist().root();
	}
	
	protected File testClassDir() {
		return buildOuputDir().sub("testClasses").createIfNotExist().root();
	}
	
	protected DependencyResolver dependenciesPath() {
		return LocalDependencyResolver.standard(baseDir("build/libs"));		
	}
	
	
	// ------------ Operations ------------
	
	
	protected void compile(DirViews sources, File destination, Iterable<File> classpath) {
		JavaCompilation compilation = new JavaCompilation();
		DirViews javaSources = sources.withFilter(JAVA_SOURCE_ONLY_FILTER);
		Notifier.start("Compiling " + javaSources.countFiles(false) + " source files to " + destination.getPath());
	    compilation.addSourceFiles(javaSources.listFiles());
	    compilation.setClasspath(classpath);
	    compilation.setOutputDirectory(destination);
	    compilation.compileOrFail();
	    Notifier.done();
	}
	
	/**
	 * Compiles production code.
	 */
	public void compile() {
		compile(sourceDirs(), classDir(), this.dependenciesPath().compile());
	}
	
	/**
	 * Compiles test code.
	 */
	@SuppressWarnings("unchecked")
	public void compileTest() {
		compile(testSourceDirs(), testClassDir(), 
				IterableUtils.concatToList(this.classDir(), this.dependenciesPath().test()));
	}
	
	/**
	 * Copies production resources in <code>class dir</code>. 
	 */
	public void copyResources() {
		Notifier.start("Coping resource files to " + classDir().getPath());
		int count = resourceDirs().copyTo(classDir());
		Notifier.done(count + " file(s) copied.");
	}
	
	/**
	 * Copies test resource in <code>class dir</code>. 
	 */
	public void copyTestResources() {
		Notifier.start("Coping test resource files to " + testClassDir().getPath());
		int count = testResourceDirs().copyTo(testClassDir());
		Notifier.done(count + " file(s) copied.");
	}
	
	@SuppressWarnings("unchecked")
	public void runUnitTests() {
		Notifier.start("Launching JUnit Tests");
		final URLClassLoader classLoader = ClassloaderUtils.createFrom(
				IterableUtils.concatToList(this.testClassDir(), this.dependenciesPath().test()));
		int count = TestUtils.launchJunitTests(classLoader, FileUtils.acceptOnly(testClassDir()));
		Notifier.done(count + " test(s) Launched.");	
	}
	
	public void javadoc() {
		Notifier.start("Generating Javadoc");
		File dir = buildOuputDir(projectName() + "-javadoc");
		Javadoc.of(this.sourceDirs()).withClasspath(this.dependenciesPath().compile()).process(dir);
		Zip.of(dir).create(buildOuputDir(projectName() + "-javadoc.zip"));
		Notifier.done();
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		copyResources();
		compileTest();
		copyTestResources();
		runUnitTests();
	}
	
	public static void main(String[] args) {
		new JakeJavaBuild().doDefault();
	}
	

}
