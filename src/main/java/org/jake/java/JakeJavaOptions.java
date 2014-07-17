package org.jake.java;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jake.JakeOptions;
import org.jake.JakeOptions.PropertyCollector;
import org.jake.file.utils.JakeUtilsFile;

public class JakeJavaOptions extends JakeOptions {

	private static final JakeJavaOptions instance;

	static {
		instance = new JakeJavaOptions(PropertyCollector.systemProps());
	}

	private final JakeLocalDependencyResolver extraPaths;


	protected JakeJavaOptions(PropertyCollector props) {
		super(props);
		final File workingDir = JakeUtilsFile.workingDir();

		final List<File> extraProvidedPath = toPath(workingDir, props.stringOr("jake.extraProvidedPath", null,
				"Mention id you want to add extra lib in your 'provided' scope path. It can be absolute or relative to the working dir." +
						"These libs will be added to the compile path but won't be embedded in war files of fat jars. " +
				"Example : -Djake.extraProvidedPath=C:\\libs\\mylib.jar;libs/others/**/*.jar"));

		this.extraPaths = JakeLocalDependencyResolver.empty().withCompileOnly(extraProvidedPath);

	}

	public static JakeLocalDependencyResolver extraPath() {
		return instance.extraPaths;
	}

	private static final List<File> toPath(File workingDir, String pathAsString) {
		if (pathAsString == null) {
			return Collections.emptyList();
		}
		return JakeUtilsFile.toPath(pathAsString, ";", workingDir);
	}


}
