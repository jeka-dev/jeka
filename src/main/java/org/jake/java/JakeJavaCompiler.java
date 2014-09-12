package org.jake.java;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.file.JakeDir;
import org.jake.file.JakeDirSet;
import org.jake.file.JakeFileFilter;
import org.jake.file.utils.JakeUtilsFile;


public final class JakeJavaCompiler {

	public static final JakeFileFilter JAVA_SOURCE_ONLY_FILTER = JakeFileFilter
			.include("**/*.java");

	private final JavaCompiler compiler;
	private final StandardJavaFileManager fileManager;

	private final List<String> options = new LinkedList<String>();
	private final List<File> javaSourceFiles = new LinkedList<File>();

	private final File outputDir;

	private String classpath = "";

	private JakeJavaCompiler(JavaCompiler compiler, File outputDir) {
		super();
		this.setOutputDirectory(outputDir);
		this.outputDir = outputDir;
		this.compiler = compiler;
		this.fileManager = compiler.getStandardFileManager(null, null, null);
	}

	public static JakeJavaCompiler ofOutput(File outputDir) {
		return new JakeJavaCompiler(getDefaultOrFail(), outputDir);
	}

	private void setOutputDirectory(File dir) {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(dir.getAbsolutePath()
					+ " is not a directory.");
		}
		options.add("-d");
		options.add(dir.getAbsolutePath());
	}

	public JakeJavaCompiler setClasspath(Iterable<File> files) {
		options.add("-cp");
		this.classpath = JakeUtilsFile.toPathString(files, ";");
		options.add(this.classpath);
		return this;
	}

	public JakeJavaCompiler addOption(String option) {
		if (option.isEmpty()) {
			return this;
		}
		options.add(option);
		return this;
	}

	private JakeJavaCompiler addSourceFiles(Iterable<File> files) {
		for (final File file : files) {
			this.javaSourceFiles.add(file);
		}
		return this;
	}

	public JakeJavaCompiler addSourceFiles(JakeDirSet jakeDirSet) {
		return addSourceFiles(jakeDirSet.withFilter(JAVA_SOURCE_ONLY_FILTER).listFiles());
	}

	public JakeJavaCompiler addSourceFiles(JakeDir jakeDir) {
		return addSourceFiles(jakeDir.withFilter(JAVA_SOURCE_ONLY_FILTER).listFiles());
	}

	public boolean compile() {
		final Iterable<? extends JavaFileObject> javaFileObjects =
				fileManager.getJavaFileObjectsFromFiles(this.javaSourceFiles);
		final CompilationTask task = compiler.getTask(new PrintWriter(JakeLog.warnStream()), null, null, options, null, javaFileObjects);
		JakeLog.startAndNextLine(("Compiling " + javaSourceFiles.size()
				+ " source files to " + outputDir.getPath()));
		JakeLog.info("using classpath [" + classpath + "]" );

		final boolean result = task.call();
		JakeLog.done();
		if (!result) {
			return false;
		}
		return true;
	}

	public void compileOrFail() {
		if (!compile()) {
			throw new JakeException("Compilation failed.");
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
