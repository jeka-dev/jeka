package org.jake;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jake.utils.JakeUtilsString;

public final class JakeJavaCompiler {

	public static final String V1_3 = "1.3";

	public static final String V1_4 = "1.4";

	public static final String V5 = "5";

	public static final String V6 = "6";

	public static final String V7 = "7";


	public static final JakeFileFilter JAVA_SOURCE_ONLY_FILTER = JakeFileFilter
			.include("**/*.java");


	private final List<String> options;

	private final List<File> javaSourceFiles;

	private final boolean failOnError;

	private final JakeProcess fork;

	@SuppressWarnings("unchecked")
	public static JakeJavaCompiler ofOutput(File outputDir) {
		if (!outputDir.isDirectory()) {
			throw new IllegalArgumentException(outputDir.getAbsolutePath()
					+ " is not a directory.");
		}
		final List<String> options = new LinkedList<String>();
		options.add("-d");
		options.add(outputDir.getAbsolutePath());
		return new JakeJavaCompiler(options, Collections.EMPTY_LIST, true, null);
	}

	private JakeJavaCompiler(List<String> options, List<File> javaSourceFiles,
			boolean failOnError, JakeProcess fork) {
		super();
		this.options = options;
		this.javaSourceFiles = javaSourceFiles;
		this.failOnError = failOnError;
		this.fork = fork;
	}

	public JakeJavaCompiler failOnError(boolean fail) {
		return new JakeJavaCompiler(options, javaSourceFiles, fail, fork);
	}

	public JakeJavaCompiler withOptions(String... options) {
		final List<String> newOptions = new LinkedList<String>(this.options);
		newOptions.addAll(Arrays.asList(options));
		return new JakeJavaCompiler(newOptions, javaSourceFiles, failOnError, fork);
	}

	public JakeJavaCompiler withClasspath(Iterable<File> files) {
		final String classpath = JakeClasspath.of(files).toString();
		return this.withOptions("-cp", classpath);
	}

	public JakeJavaCompiler withSourceVersion(String version) {
		return withOptions("-source", version);
	}

	public JakeJavaCompiler withTargetVersion(String version) {
		return withOptions("-target", version);
	}

	public JakeJavaCompiler fork(String ... parameters) {
		return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JakeProcess.ofJavaTool("javac", parameters));
	}

	public JakeJavaCompiler fork(boolean fork) {
		if (fork) {
			return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JakeProcess.ofJavaTool("javac"));
		} else {
			return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, null);
		}

	}

	public JakeJavaCompiler forkOnCompiler(String executable, String ... parameters) {
		return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JakeProcess.of(executable, parameters));
	}

	public JakeJavaCompiler andSources(Iterable<File> files) {
		final List<File> newSources = new LinkedList<File>(this.javaSourceFiles);
		for (final File file : files) {
			newSources.add(file);
		}
		return new JakeJavaCompiler(options, newSources, failOnError, fork);
	}

	public JakeJavaCompiler andSources(JakeDirSet jakeDirSet) {
		return andSources(jakeDirSet.andFilter(JAVA_SOURCE_ONLY_FILTER).files());
	}

	public JakeJavaCompiler andSources(JakeDir jakeDir) {
		return andSources(jakeDir.andFilter(JAVA_SOURCE_ONLY_FILTER).listFiles());
	}

	public boolean compile() {
		final JavaCompiler compiler = getDefaultOrFail();
		final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		JakeLog.startAndNextLine(("Compiling " + javaSourceFiles.size()
				+ " source files using options : " + JakeUtilsString.toString(options, " ")));
		final boolean result;
		if (this.fork == null) {
			final Iterable<? extends JavaFileObject> javaFileObjects =
					fileManager.getJavaFileObjectsFromFiles(this.javaSourceFiles);
			final CompilationTask task = compiler.getTask(new PrintWriter(JakeLog.warnStream()), null, null, options, null, javaFileObjects);
			result = task.call();
		} else {
			result = runOnFork();
		}
		JakeLog.done();
		if (!result) {
			if (failOnError) {
				throw new JakeException("Compilation failed.");
			}
			return false;
		}
		return true;
	}

	private boolean runOnFork() {
		final List<String> sourcePaths = new LinkedList<String>();
		for (final File file : javaSourceFiles) {
			sourcePaths.add(file.getAbsolutePath());
		}
		final JakeProcess jakeProcess = this.fork.andParameters(options).andParameters(sourcePaths);
		final int result = jakeProcess.startAndWaitFor();
		return (result == 0);
	}

	private static JavaCompiler getDefaultOrFail() {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("This plateform does not provide compiler.");
		}
		return compiler;
	}

}
