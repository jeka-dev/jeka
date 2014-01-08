package org.jake.java;

import java.io.FilenameFilter;

import org.jake.Directory;
import org.jake.FileSet;
import org.jake.JakeBaseBuilder;
import org.jake.utils.FileUtils;

public class JakeJavaBuilder extends JakeBaseBuilder {
	
	protected static final FilenameFilter SOURCE_FILTER = FileUtils.endingBy(".java");
	
	
	protected Directory sourceDir() {
		return baseDir().relative("src/main/java", false);
	}
	
	/**
	 * Specific directory where resources are stored. If resources 
	 * are only stored along the source files then returns <code>null</code>.
	 */
	protected Directory resourceDir() {
		return baseDir().relative("src/main/resources", false);
	}
	
	protected FileSet sourceFiles() {
		return sourceDir().fileSet().retainOnly(SOURCE_FILTER);
	} 
	
	public static void main(String[] args) {
		new JakeJavaBuilder().doDefault();
	}
	
	protected Directory classDir() {
		return buildOuputDir().relative("classes", true);
	}
	
	// ------------ Operations ------------
	
	/**
	 * Compiles source files returned by {@link #sourceFiles()}.
	 */
	public boolean compile() {
		JavaCompilation compilation = new JavaCompilation();
	    FileSet sourceFiles = sourceFiles();
	    logger().info("Compiling " + sourceFiles.count() + " source files to " + classDir().path());
	    compilation.addSourceFiles(sourceFiles);
	    compilation.setOutputDirectory(classDir());
	    boolean result = compilation.compile();
	    logger().info("Done");
	    return result;
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
		int count = sourceDir().copyTo(classDir(), FileUtils.reverse(SOURCE_FILTER));
		if (resourceDir() != null && resourceDir().exists()) {
			count += resourceDir().copyTo(classDir(), null);
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
