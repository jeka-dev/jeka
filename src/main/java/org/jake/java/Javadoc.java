package org.jake.java;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jake.BuildOption;
import org.jake.Notifier;
import org.jake.file.DirViews;
import org.jake.file.utils.FileUtils;

public class Javadoc {
	
	private final DirViews srcDirs;
	
	private final String extraArgs;
	
	private final Class<?> doclet;
	
	private final Iterable<File> classpath;
	
	private Javadoc(DirViews srcDirs, Class<?> doclet, Iterable<File> classpath, String extraArgs) {
		this.srcDirs = srcDirs;
		this.extraArgs = extraArgs;
		this.doclet = doclet;
		this.classpath = classpath;
	}
	
	public static Javadoc of(DirViews sources) {
		return new Javadoc(sources, null, null, "");
	}
	
	public Javadoc withDoclet(Class<?> doclet) {
		return new Javadoc(srcDirs, doclet, classpath, extraArgs);
	}
	
	public Javadoc withClasspath(Iterable<File> classpath) {
		return new Javadoc(srcDirs, doclet, classpath, extraArgs);
	}
	
	public void process(File outputDir) {
		String[] args = toArguments(outputDir);
		Notifier.nextLine();
		execute(doclet, new PrintWriter(Notifier.getInfoWriter()),Notifier.getWarnWriter(),Notifier.getErrorWriter(), args);
		Notifier.getWarnWriter().flush();
	}
	
	private String[] toArguments(File outputDir) {
		final List<String> list = new LinkedList<String>();
		list.add("-sourcepath");
		list.add(FileUtils.asPath(this.srcDirs.listRoots(), ";"));
		list.add("-d");
		list.add(outputDir.getAbsolutePath());
		if (classpath != null) {
			list.add("-classpath");
			list.add(FileUtils.asPath(this.classpath, ";"));
		}
		if (BuildOption.isVerbose()) {
			list.add("-verbose");
		} else {
			list.add("-quiet");
		}
		if (!this.extraArgs.trim().isEmpty()) {
			String[] extraArgs = this.extraArgs.split(" ");
			list.addAll(Arrays.asList(extraArgs));
		}
		
		
		for (File sourceFile : this.srcDirs.listFiles()) {
			if (sourceFile.getPath().endsWith(".java")) {
				list.add(sourceFile.getAbsolutePath());
			}
			
		}
		return list.toArray(new String[0]);
	}
	
	
	private static void execute(Class<?> doclet, PrintWriter normalWriter, PrintWriter warnWriter, PrintWriter errorWriter, String[] args) {
		String docletString = doclet != null ? doclet.getName() : "com.sun.tools.doclets.standard.Standard";
		com.sun.tools.javadoc.Main.execute("javadoc", errorWriter, warnWriter, normalWriter, 
				docletString, args);
	}

}
