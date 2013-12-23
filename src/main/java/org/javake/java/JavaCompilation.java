package org.javake.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


public class JavaCompilation {
	
	private final JavaCompiler compiler;
	private final StandardJavaFileManager fileManager;
	
	private final List<String> options = new LinkedList<>();
	private final List<File> javaSourceFiles = new LinkedList<File>();

	public JavaCompilation(JavaCompiler compiler) {
		super();
		this.compiler = compiler;
		this.fileManager = compiler.getStandardFileManager(null, null, null);
	}
	
	public JavaCompilation() {
		this(getDefaultOrFail());
	}
	
	public void setOutputDirectory(File dir) {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(dir.getAbsolutePath() 
					+ " is not a directory.");
		}
		options.add("-d");
		options.add(dir.getAbsolutePath());
	}
	
	public void addSourceFiles(Iterable<File> files) {
		for (File file : files) {
			this.javaSourceFiles.add(file);
		}
	}
	
	public boolean compile() {
		Iterable<? extends JavaFileObject> javaFileObjects = 
				fileManager.getJavaFileObjectsFromFiles(this.javaSourceFiles);
		CompilationTask task = compiler.getTask(null, null, null, options, null, javaFileObjects);
		return task.call();
	}
	
	private static JavaCompiler getDefaultOrFail() {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("This plateform does not provide compiler.");
		}
		return compiler;
	}
	
	

}
