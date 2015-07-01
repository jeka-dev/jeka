package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkException;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;

/**
 * Plugin for Eclipse IDE.
 * Add capabilities for reading/wroting project information as source location and dependencies
 * directly form the Eclipse files (.project, .classspath).
 * 
 * @author Jerome Angibaud
 */
@JkDoc({"Add capabilities for getting project information as source location and dependencies "
		+ "directly form the Eclipse files (.project, .classspath).",
		" This plugin also features method to genetate eclipse files from build class."
})
public class JkBuildPluginEclipse extends JkJavaBuildPlugin {

	static final String OPTION_VAR_PREFIX = "eclipse.var.";

	private JkBuild javaBuild;

	public static boolean candidate(File baseDir) {
		final File dotClasspathFile = new File(baseDir, ".classpath");
		final File dotProject = new File(baseDir, ".project");
		return (dotClasspathFile.exists() && dotProject.exists());
	}

	@JkDoc({"Flag for resolving dependencies against the eclipse classpath",
		"but trying to segregate test from production code considering path names : ",
	"if path contains 'test' then this is considered as an entry source for scope 'test'."})
	public boolean smartScope = true;

	@JkDoc({"If not null, this value will be used as the JRE container path when generating .classpath file."})
	public String jreContainer = null;

	private DotClasspath cachedClasspath = null;

	@JkDoc("Generates Eclipse .classpath file according project dependencies.")
	public void generateFiles() {
		final File dotClasspathFile = this.javaBuild.baseDir(".classpath");
		try {
			DotClasspath.generate(this.javaBuild, dotClasspathFile, jreContainer);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		final File dotProject = this.javaBuild.baseDir(".project");
		if (!dotProject.exists()) {
			Project.ofJavaNature(this.javaBuild().moduleId().fullName()).writeTo(dotProject);
		}
	}

	@Override
	public JkFileTreeSet alterSourceDirs(JkFileTreeSet original) {
		final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).prodSources;
	}

	@Override
	public JkFileTreeSet alterTestSourceDirs(JkFileTreeSet original) {
		final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).testSources;
	}

	@Override
	public JkFileTreeSet alterResourceDirs(JkFileTreeSet original) {
		final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).prodSources.andFilter(JkJavaBuild.RESOURCE_FILTER);
	}

	@Override
	public JkFileTreeSet alterTestResourceDirs(JkFileTreeSet original) {
		final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
		return dotClasspath().sourceDirs(javaBuild.baseDir(""), segregator).testSources.andFilter(JkJavaBuild.RESOURCE_FILTER);
	}

	@Override
	protected JkDependencies alterDependencies(JkDependencies original) {
		final ScopeResolver scopeResolver = scopeResolver();
		final List<Lib> libs = dotClasspath().libs(javaBuild.baseDir().root(), scopeResolver);
		return Lib.toDependencies(this.javaBuild(), libs, scopeResolver);
	}

	private ScopeResolver scopeResolver() {
		if (smartScope) {
			if (WstCommonComponent.existIn(javaBuild.baseDir().root())) {
				final WstCommonComponent wstCommonComponent = WstCommonComponent.of(javaBuild.baseDir().root());
				return new ScopeResolverSmart(wstCommonComponent);
			}
			return new ScopeResolverSmart(null);
		}
		return new ScopeResolverAllCompile();
	}

	private DotClasspath dotClasspath() {
		if (cachedClasspath == null) {
			final File dotClasspathFile = new File(javaBuild.baseDir(""), ".classpath");
			if (!dotClasspathFile.exists()) {
				throw new JkException(".classpath file not found");
			}
			cachedClasspath = DotClasspath.from(dotClasspathFile);
		}
		return cachedClasspath;
	}

	@Override
	public void configure(JkBuild build) {
		this.javaBuild = build;
	}

	private JkJavaBuild javaBuild() {
		return (JkJavaBuild) this.javaBuild;
	}

}
