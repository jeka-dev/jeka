package org.jake.java;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jake.file.DirView;
import org.jake.file.DirViews;
import org.jake.file.utils.FileUtils;
import org.jake.utils.IterableUtils;

public final class LocalDependencyResolver extends DependencyResolver {
	
	private final List<File> compileAndRuntimeLibs;
	
	private final List<File> runtimeOnlyLibs;
	
	private final List<File> testLibs;
	
	private final List<File> compileOnlyLibs;

	public LocalDependencyResolver(List<File> compileLibs,
			List<File> runtimeLibs, List<File> testLibs,
			List<File> providedLibs) {
		super();
		this.compileAndRuntimeLibs = compileLibs;
		this.runtimeOnlyLibs = runtimeLibs;
		this.testLibs = testLibs;
		this.compileOnlyLibs = providedLibs;
	}
	
	@SuppressWarnings("unchecked")
	public static LocalDependencyResolver of() {
		return new LocalDependencyResolver(Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST
				, Collections.EMPTY_LIST);
	}
	
	public static LocalDependencyResolver standard(File libDirectory) {
		final DirView libDir = DirView.of(libDirectory); 
		return LocalDependencyResolver
			.compileAndRuntime(libDir.include("/*.jar") )
			.withCompileOnly(  libDir.include("compile-only/*.jar"))
			.withRuntimeOnly(  libDir.include("runtime-only/*.jar"))
			.withTest(         libDir.include("test/*.jar", "tests/*.jar"));
	}
	

	@SuppressWarnings("unchecked")
	public static LocalDependencyResolver compileAndRuntime(DirView dirView) {
		return new LocalDependencyResolver(dirView.listFiles(), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}
	
	@SuppressWarnings("unchecked")
	public static LocalDependencyResolver compileAndRuntime(DirViews dirViews) {
		return new LocalDependencyResolver(dirViews.listFiles(), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}
	
	public LocalDependencyResolver withRuntimeOnly(DirViews dirViews) {
		return withRuntimeOnly(dirViews.listFiles());
	}
	
	public LocalDependencyResolver withTest(DirViews dirViews) {
		return withTest(dirViews.listFiles());
	}
	
	public LocalDependencyResolver withCompileOnly(DirViews dirViews) {
		return withCompileOnly(dirViews.listFiles());
	}
	
	public LocalDependencyResolver withCompileAndRuntime(DirViews dirViews) {
		return withCompileAndRuntime(dirViews.listFiles());
	}
	
	public LocalDependencyResolver withRuntimeOnly(File file) {
		return withRuntimeOnly(IterableUtils.single(file));
	}
	
	public LocalDependencyResolver withTest(File file) {
		return withTest(IterableUtils.single(file));
	}
	
	public LocalDependencyResolver withCompileOnly(File file) {
		return withCompileOnly(IterableUtils.single(file));
	}
	
	public LocalDependencyResolver withCompileAndRuntime(File file) {
		return withCompileAndRuntime(IterableUtils.single(file));
	}
	
	@SuppressWarnings("unchecked")
	public LocalDependencyResolver withRuntimeOnly(Iterable<File> files) {
		return new LocalDependencyResolver(this.compileAndRuntimeLibs, 
				IterableUtils.concatLists(this.runtimeOnlyLibs, files)
				, this.testLibs, this.compileOnlyLibs);
	}
	
	@SuppressWarnings("unchecked")
	public LocalDependencyResolver withTest(Iterable<File> files) {
		return new LocalDependencyResolver(this.compileAndRuntimeLibs, this.runtimeOnlyLibs,
				IterableUtils.concatLists(this.testLibs, files)
				,  this.compileOnlyLibs);
	}
	
	
	@SuppressWarnings("unchecked")
	public LocalDependencyResolver withCompileOnly(Iterable<File> files) {
		return new LocalDependencyResolver(this.compileAndRuntimeLibs, this.runtimeOnlyLibs, this.testLibs,
				IterableUtils.concatLists(this.compileOnlyLibs, files)
				);
	}
	
	@SuppressWarnings("unchecked")
	public LocalDependencyResolver withCompileAndRuntime(Iterable<File> files) {
		return new LocalDependencyResolver( IterableUtils.concatLists(this.compileAndRuntimeLibs, files), 
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
	 * @see org.jake.java.DependencyResolver#compile()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<File> compile() {
		return IterableUtils.concatLists(compileAndRuntimeLibs, compileOnlyLibs);
	}
	
	/**
	 * @see org.jake.java.DependencyResolver#testLibs(java.io.File)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<File> test() {
		return IterableUtils.concatLists( compileAndRuntimeLibs, runtimeOnlyLibs, testLibs);
	}
	
	/**
	 * @see org.jake.java.DependencyResolver#runtime()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<File> runtime() {
		return IterableUtils.concatLists(compileAndRuntimeLibs, runtimeOnlyLibs);
	}
	
	public static String asString(Iterable<File> files) {
		return FileUtils.asPath(files, ";");
	}
	

}
