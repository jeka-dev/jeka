package org.jake.java.eclipse;

import java.io.File;
import java.util.List;

import org.jake.JakeDirSet;
import org.jake.JakeOption;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;

public class JakeEclipseBuild extends JakeJavaBuild {

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
		final Lib.ScopeSegregator segregator = eclipseSmart ? Lib.SMART_LIB : Lib.ALL_COMPILE;
		final List<Lib> libs = dotClasspath().libs(baseDir().root(), segregator);
		return Lib.toDependencies(this, libs, segregator);
	}

	private DotClasspath dotClasspath() {
		if (cachedClasspath == null) {
			final File dotClasspathFile = new File(baseDir(""), ".classpath");
			cachedClasspath = DotClasspath.from(dotClasspathFile);
		}
		return cachedClasspath;
	}

}
