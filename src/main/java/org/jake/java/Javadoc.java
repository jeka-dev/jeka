package org.jake.java;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jake.BuildOption;
import org.jake.file.DirViews;
import org.jake.file.utils.FileUtils;

public class Javadoc {
	
	public final DirViews srcDirs;
	
	public final String extraArgs;
	
	private Javadoc(DirViews srcDirs, String extraArgs) {
		this.srcDirs = srcDirs;
		this.extraArgs = extraArgs;
	}
	
	public static Javadoc of(DirViews sources) {
		return new Javadoc(sources, "");
	}
	
	public void process(File outputDir) {
		String[] args = toArguments(outputDir);
		com.sun.tools.javadoc.Main.execute(args);
	}
	
	private String[] toArguments(File outputDir) {
		final List<String> list = new LinkedList<String>();
		list.add("-sourcepath");
		list.add(FileUtils.asPath(this.srcDirs.listRoots(), ";"));
		list.add("-d");
		list.add(outputDir.getAbsolutePath());
		if (BuildOption.isVerbose()) {
			list.add("-verbose");
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

}
