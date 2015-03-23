package org.jake.java.eclipse;

import java.io.File;
import java.util.List;

import org.jake.JakeDirSet;
import org.jake.JakeLocator;
import org.jake.JakeOption;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class JakeEclipseBuild extends JakeJavaBuild {

	private static final String CONTAINERS_PATH = "eclipse/containers";

	static final String OPTION_VAR_PREFIX = "eclipse.var.";

	public static boolean candidate(File baseDir) {
		final File dotClasspathFile = new File(baseDir, ".classpath");
		final File dotProject = new File(baseDir, ".project");
		return (dotClasspathFile.exists() && dotProject.exists());
	}

	@JakeOption({"Will try to resolve dependencies against the eclipse classpath",
		"but trying to segregate test from production code considering path names : ",
	"if path contains 'test' then this is considered as an entry source for scope 'test'."})
	protected boolean eclipseSmart = true;

	@JakeOption({"You can specify a different place for eclipse containers folder. It is used to resolve dependencies declared with kind 'CON' in .classpath.",
		"If you do not specify, it will take [jakeHome]/eclipse/containers",
	"It is not reccommended to change it unless for specific testing."})
	protected String containersPath;

	private DotClasspath cachedClasspath = null;

	@Override
	public JakeDirSet sourceDirs() {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(baseDir(""), segregator).prodSources;//.andFilter(RESOURCE_FILTER.reverse());
	}

	@Override
	public JakeDirSet testSourceDirs() {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(baseDir(""), segregator).testSources;
	}

	@Override
	public JakeDirSet resourceDirs() {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(baseDir(""), segregator).prodSources.andFilter(RESOURCE_FILTER);
	}

	@Override
	public JakeDirSet testResourceDirs() {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(baseDir(""), segregator).testSources.andFilter(RESOURCE_FILTER);
	}

	@Override
	protected JakeDependencies dependencies() {
		final File containersHome;
		if (containersPath == null) {
			containersHome = new File(JakeLocator.jakeHome(), CONTAINERS_PATH);
		} else {
			containersHome = new File(containersPath);
		}
		final Lib.ScopeSegregator segregator = eclipseSmart ? Lib.SMART_LIB : Lib.ALL_COMPILE;
		final List<Lib> libs = dotClasspath().libs( containersHome, baseDir().root(), segregator);
		return Lib.toDependencies(libs);
	}

	private DotClasspath dotClasspath() {
		if (cachedClasspath == null) {
			final File dotClasspathFile = new File(baseDir(""), ".classpath");
			cachedClasspath = DotClasspath.from(dotClasspathFile);
		}
		return cachedClasspath;
	}

}
