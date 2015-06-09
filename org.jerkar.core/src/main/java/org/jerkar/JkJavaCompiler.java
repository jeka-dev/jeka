package org.jerkar;

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

import org.jerkar.file.JkFileTree;
import org.jerkar.file.JkPathFilter;
import org.jerkar.utils.JkUtilsString;

/**
 * Stand for a compilation setting and process.
 * Use this class to perform java compilation.
 */
public final class JkJavaCompiler {

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
	public static final JkPathFilter JAVA_SOURCE_ONLY_FILTER = JkPathFilter
			.include("**/*.java");

	/**
	 * Creates a {@link JkJavaCompiler} producing its output in the given directory.
	 */
	@SuppressWarnings("unchecked")
	public static JkJavaCompiler ofOutput(File outputDir) {
		if (outputDir.exists() && !outputDir.isDirectory()) {
			throw new IllegalArgumentException(outputDir.getAbsolutePath()
					+ " is not a directory.");
		}
		outputDir.mkdirs();
		final List<String> options = new LinkedList<String>();
		options.add("-d");
		options.add(outputDir.getAbsolutePath());
		return new JkJavaCompiler(options, Collections.EMPTY_LIST, true, null);
	}



	private final List<String> options;

	private final List<File> javaSourceFiles;

	private final boolean failOnError;

	private final JkProcess fork;


	private JkJavaCompiler(List<String> options, List<File> javaSourceFiles,
			boolean failOnError, JkProcess fork) {
		super();
		this.options = options;
		this.javaSourceFiles = javaSourceFiles;
		this.failOnError = failOnError;
		this.fork = fork;
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with the specified failed on error parameter.
	 * If <code>fail</code> is <code>true</code> then a compilation error will throw a {@link JkException}.
	 */
	public JkJavaCompiler failOnError(boolean fail) {
		return new JkJavaCompiler(options, javaSourceFiles, fail, fork);
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but adding the specified options.
	 * Options are option you pass in javac command line as -deprecation, -nowarn, ...
	 * For example, if you want something equivalent to javac -deprecation -cp path1 path2, you should
	 * pass "-deprecation", "-cp", "path1", "path2" parameters (all space separated words must stands for one parameter,
	 * in other words : parameters must not contain any space).
	 */
	public JkJavaCompiler andOptions(String... options) {
		final List<String> newOptions = new LinkedList<String>(this.options);
		newOptions.addAll(Arrays.asList(options));
		return new JkJavaCompiler(newOptions, javaSourceFiles, failOnError, fork);
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with the specified options.
	 * 
	 * @see #andOptions(String...)
	 */
	public JkJavaCompiler withOptions(String... options) {
		final List<String> newOptions = new LinkedList<String>(this.options);
		newOptions.addAll(Arrays.asList(options));
		return new JkJavaCompiler(newOptions, javaSourceFiles, failOnError, fork);
	}


	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with the specified options under condition.
	 * 
	 * @see #andOptions(String...)
	 */
	public JkJavaCompiler withOptionsIf(boolean condition, String... options) {
		if (condition) {
			return withOptions(options);
		}
		return this;
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with the specified classpath.
	 */
	public JkJavaCompiler withClasspath(Iterable<File> files) {
		final String classpath = JkClasspath.of(files).toString();
		return this.andOptions("-cp", classpath);
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with the specified source version.
	 */
	public JkJavaCompiler withSourceVersion(String version) {
		return andOptions("-source", version);
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with using the specified annotation classes instead of
	 * using the ones discovered by default Java 6 mechanism.
	 */
	public JkJavaCompiler withAnnotationProcessors(String ...annotationProcessorClassNames) {
		return andOptions("-processor", JkUtilsString.join(annotationProcessorClassNames, ","));
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but without annotation processing.
	 */
	public JkJavaCompiler withoutAnnotationProcessing() {
		return andOptions("-proc:none");
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but only for annotation processing (no compilation).
	 */
	public JkJavaCompiler withAnnotationProcessingOnly() {
		return andOptions("-proc:only");
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with the target version.
	 */
	public JkJavaCompiler withTargetVersion(String version) {
		return andOptions("-target", version);
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but with forking the javac process.
	 * The javac process is created using specified argument defined in {@link JkProcess#ofJavaTool(String, String...)}
	 */
	public JkJavaCompiler fork(String ... parameters) {
		return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JkProcess.ofJavaTool("javac", parameters));
	}

	/**
	 * As {@link #fork(String...)} but the fork is actually done only if the <code>fork</code> parameter is <code>true</code>.
	 */
	public JkJavaCompiler fork(boolean fork, String... parameters) {
		if (fork) {
			return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JkProcess.ofJavaTool("javac"));
		} else {
			return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, null);
		}

	}

	/**
	 * As {@link #fork(String...)} but specifying the executable for the compiler.
	 * @param executable The executable for the compiler as 'jike' or '/my/speciel/jdk/javac'
	 */
	public JkJavaCompiler forkOnCompiler(String executable, String ... parameters) {
		return new JkJavaCompiler(new LinkedList<String>(options), javaSourceFiles, failOnError, JkProcess.of(executable, parameters));
	}

	/**
	 * Creates a copy of this {@link JkJavaCompiler} but adding specified source files.
	 */
	public JkJavaCompiler andSources(Iterable<File> files) {
		final List<File> newSources = new LinkedList<File>(this.javaSourceFiles);
		for (final File file : files) {
			if (file.getName().toLowerCase().endsWith(".java")) {
				newSources.add(file);
			}
		}
		return new JkJavaCompiler(options, newSources, failOnError, fork);
	}

	public JkJavaCompiler andSourceDir(File dir) {
		return andSources(JkFileTree.of(dir));
	}



	/**
	 * Actually compile the source files to the output directory.
	 * 
	 * @return <code>false</code> if a compilation error occurred.
	 * 
	 * @throws if a compilation error occured and the 'failOnError' flag in on.
	 */
	public boolean compile() throws JkException {
		final JavaCompiler compiler = getDefaultOrFail();
		final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		JkLog.startln(("Compiling " + javaSourceFiles.size()
				+ " source files using options : " + JkUtilsString.join(options, " ")));
		if (javaSourceFiles.isEmpty()) {
			JkLog.warn("No source to compile");
			JkLog.done();
			return true;
		}
		final boolean result;
		if (this.fork == null) {
			final Iterable<? extends JavaFileObject> javaFileObjects =
					fileManager.getJavaFileObjectsFromFiles(this.javaSourceFiles);
			final CompilationTask task = compiler.getTask(new PrintWriter(JkLog.warnStream()), null, null, options, null, javaFileObjects);
			result = task.call();
		} else {
			result = runOnFork();
		}
		JkLog.done();
		if (!result) {
			if (failOnError) {
				throw new JkException("Compilation failed.");
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
		final JkProcess jkProcess = this.fork.andParameters(options).andParameters(sourcePaths);
		final int result = jkProcess.runAsync();
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
