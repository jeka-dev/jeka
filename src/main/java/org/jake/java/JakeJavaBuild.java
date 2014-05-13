package org.jake.java;

import java.io.File;

import org.jake.DirView;
import org.jake.DirViews;
import org.jake.Filter;
import org.jake.JakeBaseBuild;

public class JakeJavaBuild extends JakeBaseBuild {
	
	protected static final Filter JAVA_SOURCE_ONLY_FILTER = Filter.include("**/*.java");
	
	public static final Filter RESOURCE_FILTER = Filter.exclude("**/*.java")
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
				.compile(    libDir.include("/*.jar", "/compile/*.jar") )
				.andRuntime( libDir.include("/runtime/*.jar"))
				.andTest(    libDir.include("/test/*.jar"))
				.andProvided(libDir.include("/provided/*.jar"));
	}
	
	
	// ------------ Operations ------------
	
	
	protected void compile(DirViews sources, File destination) {
		JavaCompilation compilation = new JavaCompilation();
		DirViews javaSources = sources.withFilter(JAVA_SOURCE_ONLY_FILTER);
	    logger().info("Compiling " + javaSources.fileCount(false) + " source files to " + destination.getPath());
	    compilation.addSourceFiles(javaSources.asIterableFile());
	    compilation.setOutputDirectory(classDir());
	    compilation.compileOrFail();
	    logger().info("Done");
	}
	
	/**
	 * Compiles production code.
	 */
	public void compile() {
		compile(sourceDirs(), classDir());
	}
	
	/**
	 * Compiles test code.
	 */
	public void compileTest() {
		compile(testSourceDirs(), testClassDir());
	}
	
	/**
	 * Copies production resources in <code>class dir</code>. 
	 */
	public void copyResources() {
		logger().info("Coping resource files to " + classDir().getPath());
		int count = resourceDirs().copyTo(classDir());
		logger().info(count + " file(s) copied");
	}
	
	/**
	 * Copies test resource in <code>class dir</code>. 
	 */
	public void copyTestResources() {
		logger().info("Coping resource files to " + classDir().getPath());
		int count = resourceDirs().copyTo(classDir());
		logger().info(count + " file(s) copied");
	}
	
	public void runUnitTest() {
		
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		copyResources();
		compileTest();
		copyResources();
	}
	
	public static void main(String[] args) {
		new JakeJavaBuild().doDefault();
	}
	

}
