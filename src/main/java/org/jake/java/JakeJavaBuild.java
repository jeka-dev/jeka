package org.jake.java;

import java.io.File;

import org.jake.DirView;
import org.jake.FileList;
import org.jake.Filter;
import org.jake.JakeBaseBuild;

public class JakeJavaBuild extends JakeBaseBuild {
	
	protected static final Filter JAVA_SOURCE_ONLY = Filter.include("**/*.java");
	
	/**
	 * Returns <code>DirVieww>/code> to be used by the default {@link #sourceFiles()}
	 * method. 
	 */
	protected DirView sourceDir() {
		return baseDir().relative("src/main/java");
	}
	
	/**
	 * Specific directory where resources are stored. If resources 
	 * are only stored along the source files then return <code>null</code>.
	 */
	protected DirView resourceDir() {
		return baseDir().relative("src/main/resources");
	}
	
	protected Iterable<File> sourceFiles() {
		return sourceDir().andFilter(JAVA_SOURCE_ONLY);
	} 
	
	public static void main(String[] args) {
		new JakeJavaBuild().doDefault();
	}
	
	protected DirView classDir() {
		return buildOuputDir().relative("classes").createIfNotExist();
	}
	
	// ------------ Operations ------------
	
	/**
	 * Compiles source files returned by {@link #sourceFiles()}.
	 */
	public void compile() {
		JavaCompilation compilation = new JavaCompilation();
		FileList fileList = FileList.of(sourceFiles());
	    logger().info("Compiling " + fileList.count() + " source files to " + classDir().path());
	    compilation.addSourceFiles(fileList);
	    compilation.setOutputDirectory(classDir());
	    compilation.compileOrFail();
	    logger().info("Done");
	}
	
	/**
	 * Copy in {@link #classDir()}
	 * <ul>
	 * 		<li>Files located in {@link #sourceDir()} except .java files</li>
	 * 		<li>Files located in {@link #resourceDir()} if not null</li>
	 * <ul>
	 */
	public void copyResources() {
		logger().info("Coping resource files to " + classDir().getBase().getPath());
		int count = 0;
		
		// Copy resources from source directory
		if (sourceDir() != null && sourceDir().exists()) {
			count += sourceDir().andFilter(JAVA_SOURCE_ONLY.reverse()).copyTo(classDir());
		}
		
		// Copy resources from resource directory
		if (resourceDir() != null && resourceDir().exists()) {
		//	count += resourceDir().copyTo(classDir());
		}
		logger().info(count + " file(s) copied");
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		copyResources();
		
	}
	

}
