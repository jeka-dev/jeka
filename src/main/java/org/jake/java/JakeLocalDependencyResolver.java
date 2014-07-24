package org.jake.java;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jake.file.JakeDirView;
import org.jake.file.JakeDirViewSet;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIterable;

public final class JakeLocalDependencyResolver extends JakeJavaDependencyResolver {
	
	private final List<File> compileAndRuntimeLibs;
	
	private final List<File> runtimeOnlyLibs;
	
	private final List<File> testLibs;
	
	private final List<File> compileOnlyLibs;

	public JakeLocalDependencyResolver(List<File> compileAndRuntimeLibs,
			List<File> runtimeOnlyLibs, List<File> testLibs,
			List<File> providedLibs) {
		super();
		this.compileAndRuntimeLibs = compileAndRuntimeLibs;
		this.runtimeOnlyLibs = runtimeOnlyLibs;
		this.testLibs = testLibs;
		this.compileOnlyLibs = providedLibs;
	}
	
	@SuppressWarnings("unchecked")
	public static JakeLocalDependencyResolver empty() {
		return new JakeLocalDependencyResolver(Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST
				, Collections.EMPTY_LIST);
	}
	
	public static JakeLocalDependencyResolver standard(File libDirectory) {
		final JakeDirView libDir = JakeDirView.of(libDirectory); 
		return JakeLocalDependencyResolver
			.compileAndRuntime(libDir.include("/*.jar") )
			.withCompileOnly(  libDir.include("compile-only/*.jar"))
			.withRuntimeOnly(  libDir.include("runtime-only/*.jar"))
			.withTest(         libDir.include("test/*.jar", "tests/*.jar"));
	}
	
	public static JakeLocalDependencyResolver standardIfExist(File libDirectory) {
		if (libDirectory.exists()) {
			return standard(libDirectory);
		}
		return empty();
	}
	

	@SuppressWarnings("unchecked")
	public static JakeLocalDependencyResolver compileAndRuntime(JakeDirView dirView) {
		return new JakeLocalDependencyResolver(dirView.listFiles(), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}
	
	@SuppressWarnings("unchecked")
	public static JakeLocalDependencyResolver compileAndRuntime(JakeDirViewSet dirViews) {
		return new JakeLocalDependencyResolver(dirViews.listFiles(), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}
	
	public JakeLocalDependencyResolver withRuntimeOnly(JakeDirViewSet dirViews) {
		return withRuntimeOnly(dirViews.listFiles());
	}
	
	public JakeLocalDependencyResolver withTest(JakeDirViewSet dirViews) {
		return withTest(dirViews.listFiles());
	}
	
	public JakeLocalDependencyResolver withCompileOnly(JakeDirViewSet dirViews) {
		return withCompileOnly(dirViews.listFiles());
	}
	
	public JakeLocalDependencyResolver withCompileAndRuntime(JakeDirViewSet dirViews) {
		return withCompileAndRuntime(dirViews.listFiles());
	}
	
	public JakeLocalDependencyResolver withRuntimeOnly(File file) {
		return withRuntimeOnly(JakeUtilsIterable.single(file));
	}
	
	public JakeLocalDependencyResolver withTest(File file) {
		return withTest(JakeUtilsIterable.single(file));
	}
	
	public JakeLocalDependencyResolver withCompileOnly(File file) {
		return withCompileOnly(JakeUtilsIterable.single(file));
	}
	
	public JakeLocalDependencyResolver withCompileAndRuntime(File file) {
		return withCompileAndRuntime(JakeUtilsIterable.single(file));
	}
	
	@SuppressWarnings("unchecked")
	public JakeLocalDependencyResolver withRuntimeOnly(Iterable<File> files) {
		return new JakeLocalDependencyResolver(this.compileAndRuntimeLibs, 
				JakeUtilsIterable.concatLists(this.runtimeOnlyLibs, files)
				, this.testLibs, this.compileOnlyLibs);
	}
	
	@SuppressWarnings("unchecked")
	public JakeLocalDependencyResolver withTest(Iterable<File> files) {
		return new JakeLocalDependencyResolver(this.compileAndRuntimeLibs, this.runtimeOnlyLibs,
				JakeUtilsIterable.concatLists(this.testLibs, files)
				,  this.compileOnlyLibs);
	}
	
	
	@SuppressWarnings("unchecked")
	public JakeLocalDependencyResolver withCompileOnly(Iterable<File> files) {
		return new JakeLocalDependencyResolver(this.compileAndRuntimeLibs, this.runtimeOnlyLibs, this.testLibs,
				JakeUtilsIterable.concatLists(this.compileOnlyLibs, files)
				);
	}
	
	@SuppressWarnings("unchecked")
	public JakeLocalDependencyResolver withCompileAndRuntime(Iterable<File> files) {
		return new JakeLocalDependencyResolver( JakeUtilsIterable.concatLists(this.compileAndRuntimeLibs, files), 
				this.runtimeOnlyLibs, this.testLibs, this.compileOnlyLibs);
	}
	
	
	
	/**
	 * libraries files needed to run production code but not needed for compiling.
	 */
	public List<File> getRuntimeOnlyDependencies() {
		return runtimeOnlyLibs;
	}

	/**
	 * libraries files needed to compile and run test code.
	 */
	public List<File> getTestDependencies() {
		return testLibs;
	}

	/**
	 * libraries files needed to compile production code but provided by the container running 
	 * the code... thus not part of the delivery.
	 */
	public List<File> getCompileOnlyDependencies() {
		return compileOnlyLibs;
	}
	
	/**
	 * @see org.jake.java.JakeJavaDependencyResolver#compile()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<File> compile() {
		return JakeUtilsIterable.concatLists(compileAndRuntimeLibs, compileOnlyLibs);
	}
	
	/**
	 * @see org.jake.java.JakeJavaDependencyResolver#testLibs(java.io.File)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<File> test() {
		return JakeUtilsIterable.concatLists( compileAndRuntimeLibs, runtimeOnlyLibs, testLibs);
	}
	
	/**
	 * @see org.jake.java.JakeJavaDependencyResolver#runtime()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<File> runtime() {
		return JakeUtilsIterable.concatLists(compileAndRuntimeLibs, runtimeOnlyLibs);
	}
	
	public static String asString(Iterable<File> files) {
		return JakeUtilsFile.toPathString(files, ";");
	}
	

}
