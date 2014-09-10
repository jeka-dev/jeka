package org.jake.java;

import java.io.File;
import java.util.Arrays;

import org.jake.file.JakeDir;
import org.jake.file.JakeDirSet;

public final class JakeLocalDependencyResolver extends
JakeJavaDependencyResolver {

	private final JakeClasspath compileAndRuntimeLibs;

	private final JakeClasspath runtimeOnlyLibs;

	private final JakeClasspath testLibs;

	private final JakeClasspath compileOnlyLibs;

	public JakeLocalDependencyResolver(JakeClasspath compileAndRuntimeLibs,
			JakeClasspath runtimeOnlyLibs, JakeClasspath testLibs,
			JakeClasspath providedLibs) {
		super();
		this.compileAndRuntimeLibs = compileAndRuntimeLibs;
		this.runtimeOnlyLibs = runtimeOnlyLibs;
		this.testLibs = testLibs;
		this.compileOnlyLibs = providedLibs;
	}

	public static JakeLocalDependencyResolver empty() {
		return new JakeLocalDependencyResolver(JakeClasspath.of(),
				JakeClasspath.of(), JakeClasspath.of(), JakeClasspath.of());
	}

	public static JakeLocalDependencyResolver standard(File libDirectory) {
		final JakeDir libDir = JakeDir.of(libDirectory);
		return JakeLocalDependencyResolver
				.compile(libDir.include("*.jar", "compile/*.jar"))
				.provided(libDir.include("provided/*.jar"))
				.runtime(libDir.include("runtime/*.jar"))
				.test(libDir.include("test/*.jar"));
	}

	public static JakeLocalDependencyResolver standardIfExist(File libDirectory) {
		if (libDirectory.exists()) {
			return standard(libDirectory);
		}
		return empty();
	}

	public static JakeLocalDependencyResolver compile(JakeDir dirView) {
		return new JakeLocalDependencyResolver(JakeClasspath.of(dirView),
				JakeClasspath.of(), JakeClasspath.of(), JakeClasspath.of());
	}

	public static JakeLocalDependencyResolver compileAndRuntime(
			JakeDirSet dirViews) {
		return new JakeLocalDependencyResolver(JakeClasspath.of(dirViews),
				JakeClasspath.of(), JakeClasspath.of(), JakeClasspath.of());
	}

	public JakeLocalDependencyResolver withRuntimeOnly(File... files) {
		return runtime(Arrays.asList(files));
	}

	public JakeLocalDependencyResolver withTest(File... files) {
		return test(Arrays.asList(files));
	}

	public JakeLocalDependencyResolver withCompileOnly(File... files) {
		return provided(Arrays.asList(files));
	}

	public JakeLocalDependencyResolver withCompileAndRuntime(File ... files) {
		return withCompileAndRuntime(Arrays.asList(files));
	}

	public JakeLocalDependencyResolver runtime(Iterable<File> files) {
		return new JakeLocalDependencyResolver(this.compileAndRuntimeLibs,
				this.runtimeOnlyLibs.and(files), this.testLibs,
				this.compileOnlyLibs);
	}

	public JakeLocalDependencyResolver test(Iterable<File> files) {
		return new JakeLocalDependencyResolver(this.compileAndRuntimeLibs,
				this.runtimeOnlyLibs, this.testLibs.and(files),
				this.compileOnlyLibs);
	}

	public JakeLocalDependencyResolver provided(Iterable<File> files) {
		return new JakeLocalDependencyResolver(this.compileAndRuntimeLibs,
				this.runtimeOnlyLibs, this.testLibs,
				this.compileOnlyLibs.and(files));
	}

	public JakeLocalDependencyResolver withCompileAndRuntime(
			Iterable<File> files) {
		return new JakeLocalDependencyResolver(
				this.compileAndRuntimeLibs.and(files), this.runtimeOnlyLibs,
				this.testLibs, this.compileOnlyLibs);
	}

	/**
	 * libraries files needed to run production code but not needed for
	 * compiling.
	 */
	public JakeClasspath getRuntimeOnlyDependencies() {
		return runtimeOnlyLibs;
	}

	/**
	 * libraries files needed to compile and run test code.
	 */
	public JakeClasspath getTestDependencies() {
		return testLibs;
	}

	/**
	 * libraries files needed to compile production code but provided by the
	 * container running the code... thus not part of the delivery.
	 */
	public JakeClasspath getCompileOnlyDependencies() {
		return compileOnlyLibs;
	}

	/**
	 * @see org.jake.java.JakeJavaDependencyResolver#compile()
	 */
	@Override
	public JakeClasspath compile() {
		return compileAndRuntimeLibs.and(compileOnlyLibs);
	}

	/**
	 * @see org.jake.java.JakeJavaDependencyResolver#testLibs(java.io.File)
	 */
	@Override
	public JakeClasspath test() {
		return compileAndRuntimeLibs.and(runtimeOnlyLibs).and(testLibs)
				.and(compileOnlyLibs);
	}

	/**
	 * @see org.jake.java.JakeJavaDependencyResolver#runtime()
	 */
	@Override
	public JakeClasspath runtime() {
		return compileAndRuntimeLibs.and(runtimeOnlyLibs);
	}



}
