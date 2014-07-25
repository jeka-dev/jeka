package org.jake.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.file.JakeDirView;
import org.jake.file.utils.JakeUtilsFile;


public final class JakeJavaCompiler {

	private final JavaCompiler compiler;
	private final StandardJavaFileManager fileManager;

	private final List<String> options = new LinkedList<String>();
	private final List<File> javaSourceFiles = new LinkedList<File>();

	public JakeJavaCompiler(JavaCompiler compiler) {
		super();
		this.compiler = compiler;
		this.fileManager = compiler.getStandardFileManager(null, null, null);
	}

	public JakeJavaCompiler() {
		this(getDefaultOrFail());
	}

	public void setOutputDirectory(JakeDirView dir) {
		setOutputDirectory(dir.root());
	}

	public void setOutputDirectory(File dir) {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(dir.getAbsolutePath()
					+ " is not a directory.");
		}
		options.add("-d");
		options.add(dir.getAbsolutePath());
	}

	public void setClasspath(Iterable<File> files) {
		options.add("-cp");
		options.add(JakeUtilsFile.toPathString(files, ";"));
	}

	public void addSourceFiles(Iterable<File> files) {
		for (final File file : files) {
			this.javaSourceFiles.add(file);
		}
	}


	public boolean compile() {
		final Iterable<? extends JavaFileObject> javaFileObjects =
				fileManager.getJavaFileObjectsFromFiles(this.javaSourceFiles);
		final CompilationTask task = compiler.getTask(null, null, null, options, null, javaFileObjects);
		JakeLog.flush();
		final boolean result = task.call();
		JakeLog.flush();
		return result;
	}

	public void compileOrFail() {
		final Iterable<? extends JavaFileObject> javaFileObjects =
				fileManager.getJavaFileObjectsFromFiles(this.javaSourceFiles);
		final CompilationTask task = compiler.getTask(null, null, null, options, null, javaFileObjects);
		JakeLog.flush();
		final boolean result = task.call();
		JakeLog.flush();
		if (!result) {
			throw new JakeException("Compilation failure.");
		}

	}


	private static JavaCompiler getDefaultOrFail() {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("This plateform does not provide compiler.");
		}
		return compiler;
	}



}
