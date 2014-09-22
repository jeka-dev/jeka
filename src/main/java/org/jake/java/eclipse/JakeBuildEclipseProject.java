package org.jake.java.eclipse;

import java.io.File;
import java.util.List;

import org.jake.JakeDirSet;
import org.jake.JakeLocator;
import org.jake.JakeOption;
import org.jake.java.JakeJavaDependencyResolver;
import org.jake.java.build.JakeBuildJava;

public class JakeBuildEclipseProject extends JakeBuildJava {

	private static final String CONTAINERS_PATH = "eclipse/containers";

	public static boolean candidate(File baseDir) {
		final File dotClasspathFile = new File(baseDir, ".classpath");
		final File dotProject = new File(baseDir, ".project");
		return (dotClasspathFile.exists() && dotProject.exists());
	}

	@JakeOption({"Will try to resolve dependencies against the eclipse classpath",
		"but trying to segregate test from production code considering pathes name : ",
	"if path contains 'test' then this is considered as a entry source for tests."})
	protected boolean eclipseSmart = true;

	@JakeOption({"You can specify a different place for eclipse containers folder. It is used to resole dependencies declared with kind 'CON' in .classpath.",
		"If you do not specify, it will take [jakeHome]/eclipse/containers",
	"It is not reccommended to change it unless for specific testing."})
	protected String containersPath;

	private DotClasspath cachedClasspath = null;

	@Override
	public JakeDirSet sourceDirs() {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(baseDir(""), segregator).prodSources;
	}

	@Override
	public JakeDirSet testSourceDirs() {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(baseDir(""), segregator).testSources;
	}

	@Override
	public JakeDirSet resourceDirs() {
		return JakeDirSet.empty();
	}

	@Override
	public JakeDirSet testResourceDirs() {
		return JakeDirSet.empty();
	}

	@Override
	protected JakeJavaDependencyResolver baseDependencyResolver() {
		final File containersHome;
		if (containersPath == null) {
			containersHome = new File(JakeLocator.jakeHome(), CONTAINERS_PATH);
		} else {
			containersHome = new File(containersPath);
		}
		final Lib.ScopeSegregator segregator = eclipseSmart ? Lib.SMART_LIB : Lib.ALL_COMPILE;
		final List<Lib> libs = dotClasspath().libs( containersHome, baseDir().root(), segregator);
		return Lib.toDependencyResolver(libs);
	}

	private DotClasspath dotClasspath() {
		if (cachedClasspath == null) {
			final File dotClasspathFile = new File(baseDir(""), ".classpath");
			cachedClasspath = DotClasspath.from(dotClasspathFile);
		}
		return cachedClasspath;
	}

}
