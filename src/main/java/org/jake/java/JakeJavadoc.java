package org.jake.java;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeOptions;
import org.jake.JakeLogger;
import org.jake.file.JakeDirViewSet;
import org.jake.file.utils.FileUtils;
import org.jake.java.utils.JakeUtilsClassloader;
import org.jake.java.utils.JakeUtilsJdk;
import org.jake.utils.JakeUtilsReflect;

public class JakeJavadoc {

	private static final String JAVADOC_MAIN_CLASS_NAME = "com.sun.tools.javadoc.Main";

	private final JakeDirViewSet srcDirs;

	private final String extraArgs;

	private final Class<?> doclet;

	private final Iterable<File> classpath;

	private JakeJavadoc(JakeDirViewSet srcDirs, Class<?> doclet, Iterable<File> classpath, String extraArgs) {
		this.srcDirs = srcDirs;
		this.extraArgs = extraArgs;
		this.doclet = doclet;
		this.classpath = classpath;
	}

	public static JakeJavadoc of(JakeDirViewSet sources) {
		return new JakeJavadoc(sources, null, null, "");
	}

	public JakeJavadoc withDoclet(Class<?> doclet) {
		return new JakeJavadoc(srcDirs, doclet, classpath, extraArgs);
	}

	public JakeJavadoc withClasspath(Iterable<File> classpath) {
		return new JakeJavadoc(srcDirs, doclet, classpath, extraArgs);
	}

	public void process(File outputDir) {
		final String[] args = toArguments(outputDir);
		JakeLogger.nextLine();
		execute(doclet, new PrintWriter(JakeLogger.getInfoWriter()),JakeLogger.getWarnWriter(),JakeLogger.getErrorWriter(), args);
		JakeLogger.getWarnWriter().flush();
	}

	private String[] toArguments(File outputDir) {
		final List<String> list = new LinkedList<String>();
		list.add("-sourcepath");
		list.add(FileUtils.toPathString(this.srcDirs.listRoots(), ";"));
		list.add("-d");
		list.add(outputDir.getAbsolutePath());
		if (JakeOptions.isVerbose()) {
			list.add("-verbose");
		} else {
			list.add("-quiet");
		}
		list.add("-docletpath");
		list.add(JakeUtilsJdk.toolsJar().getPath());
		if (classpath != null && classpath.iterator().hasNext()) {
			list.add("-classpath");
			list.add(FileUtils.toPathString(this.classpath, ";"));
		}
		if (!this.extraArgs.trim().isEmpty()) {
			final String[] extraArgs = this.extraArgs.split(" ");
			list.addAll(Arrays.asList(extraArgs));
		}


		for (final File sourceFile : this.srcDirs.listFiles()) {
			if (sourceFile.getPath().endsWith(".java")) {
				list.add(sourceFile.getAbsolutePath());
			}

		}
		return list.toArray(new String[0]);
	}


	private static void execute(Class<?> doclet, PrintWriter normalWriter, PrintWriter warnWriter, PrintWriter errorWriter, String[] args) {
		final String docletString = doclet != null ? doclet.getName() : "com.sun.tools.doclets.standard.Standard";
		final Class<?> mainClass = getJavadocMainClass(JakeUtilsClassloader.current());
		JakeUtilsReflect.newInstance(mainClass);
		final Method method = JakeUtilsReflect.getMethod(mainClass, "execute", String.class, PrintWriter.class, PrintWriter.class, PrintWriter.class, String.class, new String[0].getClass());
		JakeUtilsReflect.invoke(null, method, "Javadoc", errorWriter, warnWriter, normalWriter,
				docletString, args);
	}

	public static Class<?> getJavadocMainClass(URLClassLoader base) {
		Class<?> mainClass;
		try {
			mainClass = Class.forName(JAVADOC_MAIN_CLASS_NAME, true, base);
		} catch (final ClassNotFoundException e) {
			final Method method = JakeUtilsReflect.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);
			JakeUtilsReflect.invoke(base, method, FileUtils.toUrl(JakeUtilsJdk.toolsJar()));
			try {
				mainClass = Class.forName(JAVADOC_MAIN_CLASS_NAME, true, base);
			} catch (final ClassNotFoundException e1) {
				throw new RuntimeException("It seems that you are running a JRE instead of a JDK, please run Jake using a JDK.", e1);
			}
		}
		return mainClass;
	}


}
