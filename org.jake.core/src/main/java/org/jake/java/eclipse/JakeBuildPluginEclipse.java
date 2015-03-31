package org.jake.java.eclipse;

import java.io.File;
import java.util.List;

import org.jake.JakeBuild;
import org.jake.JakeDirSet;
import org.jake.JakeDoc;
import org.jake.JakeOption;
import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;

@JakeDoc({"Add capabilities for getting project information as source location and dependencies "
		+ "directly form the Eclipse files (.project, .classspath).",
		" This plugin allow also to genetare eclipse files from the Jake build class."
})
public class JakeBuildPluginEclipse extends JakeJavaBuildPlugin {

	static final String OPTION_VAR_PREFIX = "eclipse.var.";

	private JakeJavaBuild javaBuild;

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
	public JakeDirSet alterSourceDirs(JakeDirSet original) {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).prodSources;
	}

	@Override
	public JakeDirSet alterTestSourceDirs(JakeDirSet original) {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).testSources;
	}

	@Override
	public JakeDirSet alterResourceDirs(JakeDirSet original) {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).prodSources.andFilter(JakeJavaBuild.RESOURCE_FILTER);
	}

	@Override
	public JakeDirSet alterTestResourceDirs(JakeDirSet original) {
		final Sources.TestSegregator segregator = eclipseSmart ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).testSources.andFilter(JakeJavaBuild.RESOURCE_FILTER);
	}

	@Override
	protected JakeDependencies alterDependencies(JakeDependencies original) {
		final ScopeResolver scopeResolver = scopeResolver();
		final List<Lib> libs = dotClasspath().libs(javaBuild.baseDir().root(), scopeResolver);
		return Lib.toDependencies(this.javaBuild, libs, scopeResolver);
	}

	private ScopeResolver scopeResolver() {
		if (eclipseSmart) {
			if (WstCommonComponent.existIn(javaBuild.baseDir().root())) {
				final WstCommonComponent wstCommonComponent = WstCommonComponent.of(javaBuild.baseDir().root());
				return new ScopeResolverSmart(wstCommonComponent);
			}
			return null;
		}
		return new ScopeResolverAllCompile();
	}

	private DotClasspath dotClasspath() {
		if (cachedClasspath == null) {
			final File dotClasspathFile = new File(javaBuild.baseDir(""), ".classpath");
			cachedClasspath = DotClasspath.from(dotClasspathFile);
		}
		return cachedClasspath;
	}

	@Override
	public void configure(JakeBuild build) {
		this.javaBuild = (JakeJavaBuild) build;
	}

}
