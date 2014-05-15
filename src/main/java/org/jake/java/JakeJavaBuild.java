package org.jake.java;

import java.io.File;

import org.jake.DirView;
import org.jake.DirViews;
import org.jake.Filter;
import org.jake.JakeBaseBuild;
import org.jake.Notifier;

public class JakeJavaBuild extends JakeBaseBuild {
	
	protected static final Filter JAVA_SOURCE_ONLY_FILTER = Filter.include("**/*.java");
	
	protected static final Filter RESOURCE_FILTER = Filter.exclude("**/*.java")
			.andExcludeAll("**/package.html").andExcludeAll("**/doc-files");
	
	/**
	 * Returns location of production source code.
	 */
	protected DirViews sourceDirs() {
		return DirViews.of( baseDir().relative("src/main/java") );
	}
	
	/**
	 * Returns location of production resources.
	 */
	protected DirViews resourceDirs() {
		return sourceDirs().withFilter(RESOURCE_FILTER).and(baseDir().relative("src/main/resources"));
	} 
	
	/**
	 * Returns location of test source code.
	 */
	protected DirViews testSourceDirs() {
		return DirViews.of( baseDir().relative("src/test/java") );
	}
	
	/**
	 * Returns location of test resources.
	 */
	protected DirViews testResourceDirs() {
		return DirViews.of(baseDir().relative("src/test/resources"))
				.and(testSourceDirs().withFilter(RESOURCE_FILTER));
	} 
		
		
	protected File classDir() {
		return buildOuputDir().relative("classes").createIfNotExist().getBase();
	}
	
	protected File testClassDir() {
		return buildOuputDir().relative("testClasses").createIfNotExist().getBase();
	}
	
	protected BuildPath buildPath() {
		final DirView libDir = baseDir().relative("/build/libs");
		return BuildPath
				.compile(    libDir.include("/*.jar", "compile/*.jar") )
				.andRuntime( libDir.include("runtime/*.jar"))
				.andTest(    libDir.include("test/*.jar"))
				.andProvided(libDir.include("provided/*.jar"));
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
		compile(sourceDirs(), classDir(), this.buildPath().getComputedCompileLibs());
	}
	
	/**
	 * Compiles test code.
	 */
	public void compileTest() {
		compile(testSourceDirs(), testClassDir(), 
				this.buildPath().getComputedTestLibs(this.classDir()));
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
	
	public void runUnitTest() {
		
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		copyResources();
		compileTest();
		copyTestResources();
	}
	
	public static void main(String[] args) {
		new JakeJavaBuild().doDefault();
	}
	

}
