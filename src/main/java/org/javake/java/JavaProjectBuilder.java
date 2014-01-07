package org.javake.java;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.javake.BaseProjectBuilder;
import org.javake.Directory;
import org.javake.FileUtils;

public class JavaProjectBuilder extends BaseProjectBuilder {
	
	protected static final FilenameFilter SOURCE_FILTER = FileUtils.endingBy(".java");
	
	
	protected Directory sourceDir() {
		return baseDir().relative("src/main/java", false);
	}
	
	protected Directory resourceDir() {
		return baseDir().relative("src/main/resources", false);
	}
	
	protected List<File> sourceFiles() {
		return sourceDir().allFiles(SOURCE_FILTER);
	} 
	
	public static void main(String[] args) {
		new JavaProjectBuilder().doDefault();
	}
	
	protected Directory classDir() {
		return buildOuputDir().relative("classes", true);
	}
	
	// ------------ Operations ------------
	
	public boolean compile() {
		JavaCompilation compilation = new JavaCompilation();
	    List<File> sourceFiles = sourceFiles();
	    compilation.addSourceFiles(sourceFiles);
	    compilation.setOutputDirectory(classDir().getBase());
	    boolean result = compilation.compile();
	    logger().info(sourceFiles.size() + " source files compiled to " + classDir().getBase().getPath());
	    return result;
	}
	
	public void copyResources() {
		int count = sourceDir().copyTo(classDir().getBase(), FileUtils.reverse(SOURCE_FILTER));
		if (resourceDir().getBase().exists()) {
			count += resourceDir().copyTo(classDir().getBase(), null);
		}
		logger().info(count + " resource files copied to " + classDir().getBase().getPath());
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		copyResources();
	}
	

}
