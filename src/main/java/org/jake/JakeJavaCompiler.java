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

/**
 * Stand for a compilation setting and process.
 * Use this class to perform java compilation.
 */
public final class JakeJavaCompiler {

	/** Stands for Java version 1.3 */
	public static final String V1_3 = "1.3";

	/** Stands for Java version 1.4 */
	public static final String V1_4 = "1.4";

	/** Stands for Java version 5 */
	public static final String V5 = "5";

	/** Stands for Java version 6 */
	public static final String V6 = "6";

	/** Stands for Java version 7 */
	public static final String V7 = "7";

	/** Stands for Java version 8 */
	public static final String V8 = "8";

	/** Filter to retain only source files */
	public static final JakeFileFilter JAVA_SOURCE_ONLY_FILTER = JakeFileFilter
			.include("**/*.java");

	/**
	 * Creates a {@link JakeJavaCompiler} producing its output in the given directory.
	 */
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



	private final List<String> options;

	private final List<File> javaSourceFiles;

	private final boolean failOnError;

	private final JakeProcess fork;


	private JakeJavaCompiler(List<String> options, List<File> javaSourceFiles,
			boolean failOnError, JakeProcess fork) {
		super();
		this.options = options;
		this.javaSourceFiles = javaSourceFiles;
		this.failOnError = failOnError;
		this.fork = fork;
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but with the specified failed on error parameter.
	 * If <code>fail</code> is <code>true</code> then a compilation error will throw a {@link JakeException}.
	 */
	public JakeJavaCompiler failOnError(boolean fail) {
		return new JakeJavaCompiler(options, javaSourceFiles, fail, fork);
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but adding the specified options.
	 * Options are option you pass in javac command line as -deprecation, -nowarn, ...
	 * For example, if you want something equivalent to javac -deprecation -cp path1 path2, you should
	 * pass "-deprecation", "-cp", "path1", "path2" parameters (all space separated words must stands for one parameter,
	 * in other words : parameters must not contain any space).
	 */
	public JakeJavaCompiler andOptions(String... options) {
		final List<String> newOptions = new LinkedList<String>(this.options);
		newOptions.addAll(Arrays.asList(options));
		return new JakeJavaCompiler(newOptions, javaSourceFiles, failOnError, fork);
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but with the specified options.
	 * 
	 * @see #andOptions(String...)
	 */
	public JakeJavaCompiler withOptions(String... options) {
		final List<String> newOptions = new LinkedList<String>(this.options);
		newOptions.addAll(Arrays.asList(options));
		return new JakeJavaCompiler(newOptions, javaSourceFiles, failOnError, fork);
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but with the specified classpath.
	 */
	public JakeJavaCompiler withClasspath(Iterable<File> files) {
		final String classpath = JakeClasspath.of(files).toString();
		return this.andOptions("-cp", classpath);
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but with the specified source version.
	 */
	public JakeJavaCompiler withSourceVersion(String version) {
		return andOptions("-source", version);
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but with the target version.
	 */
	public JakeJavaCompiler withTargetVersion(String version) {
		return andOptions("-target", version);
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but with forking the javac process.
	 * The javac process is created using specified argument defined in {@link JakeProcess#ofJavaTool(String, String...)}
	 */
	public JakeJavaCompiler fork(String ... parameters) {
		return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JakeProcess.ofJavaTool("javac", parameters));
	}

	/**
	 * As {@link #fork(String...)} but the fork is actually done only if the <code>fork</code> parameter is <code>true</code>.
	 */
	public JakeJavaCompiler fork(boolean fork, String... parameters) {
		if (fork) {
			return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JakeProcess.ofJavaTool("javac"));
		} else {
			return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, null);
		}

	}

	/**
	 * As {@link #fork(String...)} but specifying the executable for the compiler.
	 * @param executable The executable for the compiler as 'jike' or '/my/speciel/jdk/javac'
	 */
	public JakeJavaCompiler forkOnCompiler(String executable, String ... parameters) {
		return new JakeJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JakeProcess.of(executable, parameters));
	}

	/**
	 * Creates a copy of this {@link JakeJavaCompiler} but adding specified source files.
	 */
	public JakeJavaCompiler andSources(Iterable<File> files) {
		final List<File> newSources = new LinkedList<File>(this.javaSourceFiles);
		for (final File file : files) {
			if (file.getName().toLowerCase().endsWith(".java")) {
				newSources.add(file);
			}
		}
		return new JakeJavaCompiler(options, newSources, failOnError, fork);
	}



	/**
	 * Actually compile the source files to the output directory.
	 * 
	 * @return <code>false</code> if a compilation error occurred.
	 * 
	 * @throws if a compilation error occured and the 'failOnError' flag in on.
	 */
	public boolean compile() throws JakeException {
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
