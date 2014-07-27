package org.jake.java;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jake.JakeOption;
import org.jake.JakeOptions;
import org.jake.file.utils.JakeUtilsFile;

public class JakeJavaOptions extends JakeOptions {

	private static final JakeJavaOptions INSTANCE = new JakeJavaOptions();

	@JakeOption({"Mention if you want to add extra lib in your 'compile' scope but not in your 'runtime' scope. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile path but won't be embedded in war files or fat jars.",
	"Example : -OextraProvidedPath=C:\\libs\\mylib.jar;libs/others/**/*.jar"})
	private String extraProvidedPath;

	@JakeOption({"Mention if you want to add extra lib in your 'runtime' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the runtime path.",
	"Example : -OextraRuntimePath=C:\\libs\\mylib.jar;libs/others/**/*.jar"})
	private String extraRuntimePath;

	@JakeOption({"Mention if you want to add extra lib in your 'compile' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile and runtime path.",
	"Example : -OextraCompilePath=C:\\libs\\mylib.jar;libs/others/**/*.jar"})
	private String extraCompilePath;

	@JakeOption({"Mention if you want to add extra lib in your 'test' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile and runtime path.",
	"Example : -OextraTestPath=C:\\libs\\mylib.jar;libs/others/**/*.jar"})
	private String extraTestPath;

	protected JakeJavaOptions() {
	}

	public static JakeLocalDependencyResolver extraPath(File baseDir) {
		return INSTANCE.computeExtraPath(baseDir);
	}

	private JakeLocalDependencyResolver computeExtraPath(File baseDir) {
		final List<File> extraProvidedPathList = toPath(baseDir, extraProvidedPath);
		final List<File> extraCompilePathList = toPath(baseDir, extraCompilePath);
		final List<File> extraRuntimePathList = toPath(baseDir, extraRuntimePath);
		final List<File> extraTestPathList = toPath(baseDir, extraTestPath);
		return new JakeLocalDependencyResolver(extraCompilePathList, extraRuntimePathList,
				extraTestPathList, extraProvidedPathList);
	}

	private static final List<File> toPath(File workingDir, String pathAsString) {
		if (pathAsString == null) {
			return Collections.emptyList();
		}
		return JakeUtilsFile.toPath(pathAsString, ";", workingDir);
	}


}
