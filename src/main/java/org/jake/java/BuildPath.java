package org.jake.java;

import java.io.File;

import org.jake.DirView;
import org.jake.DirViews;
import org.jake.utils.FileUtils;
import org.jake.utils.IterableUtils;

public final class BuildPath {
	
	private final Iterable<File> compileLibs;
	
	private final Iterable<File> runtimeLibs;
	
	private final Iterable<File> testLibs;
	
	private final Iterable<File> providedLibs;

	public BuildPath(Iterable<File> compileLibs,
			Iterable<File> runtimeLibs, Iterable<File> testLibs,
			Iterable<File> providedLibs) {
		super();
		this.compileLibs = compileLibs;
		this.runtimeLibs = runtimeLibs;
		this.testLibs = testLibs;
		this.providedLibs = providedLibs;
	}
	
	public static BuildPath of() {
		return new BuildPath(IterableUtils.emptyFile(), 
				IterableUtils.emptyFile(), IterableUtils.emptyFile(), IterableUtils.emptyFile());
	}
	
	public static BuildPath compile(DirView dirView) {
		return new BuildPath(dirView, 
				IterableUtils.emptyFile(), IterableUtils.emptyFile(), IterableUtils.emptyFile());
	}
	
	public static BuildPath compile(DirViews dirViews) {
		return new BuildPath(dirViews.listFiles(), 
				IterableUtils.emptyFile(), IterableUtils.emptyFile(), IterableUtils.emptyFile());
	}
	
	public BuildPath andRuntime(DirViews dirViews) {
		return andRuntime(dirViews.listFiles());
	}
	
	public BuildPath andTest(DirViews dirViews) {
		return andTest(dirViews.listFiles());
	}
	
	public BuildPath andProvided(DirViews dirViews) {
		return andProvided(dirViews.listFiles());
	}
	
	public BuildPath andCompile(DirViews dirViews) {
		return andCompile(dirViews.listFiles());
	}
	
	public BuildPath andRuntime(File file) {
		return andRuntime(IterableUtils.single(file));
	}
	
	public BuildPath andTest(File file) {
		return andTest(IterableUtils.single(file));
	}
	
	public BuildPath andProvided(File file) {
		return andProvided(IterableUtils.single(file));
	}
	
	public BuildPath andCompile(File file) {
		return andCompile(IterableUtils.single(file));
	}
	
	@SuppressWarnings("unchecked")
	public BuildPath andRuntime(Iterable<File> files) {
		return new BuildPath(this.compileLibs, 
				IterableUtils.chain(this.runtimeLibs, files)
				, this.testLibs, this.providedLibs);
	}
	
	@SuppressWarnings("unchecked")
	public BuildPath andTest(Iterable<File> files) {
		return new BuildPath(this.compileLibs, this.runtimeLibs,
				IterableUtils.chain(this.testLibs, files)
				,  this.providedLibs);
	}
	
	
	@SuppressWarnings("unchecked")
	public BuildPath andProvided(Iterable<File> files) {
		return new BuildPath(this.compileLibs, this.runtimeLibs, this.testLibs,
				IterableUtils.chain(this.providedLibs, files)
				);
	}
	
	@SuppressWarnings("unchecked")
	public BuildPath andCompile(Iterable<File> files) {
		return new BuildPath( IterableUtils.chain(this.compileLibs, files), 
				this.runtimeLibs, this.testLibs, this.providedLibs);
	}
	
	
	/**
	 * libraries files needed to compile and run production code.
	 */
	public Iterable<File> getCompileLibs() {
		return compileLibs;
	}

	/**
	 * libraries files needed to run production code but not needed for compiling.
	 */
	public Iterable<File> getRuntimeLibs() {
		return runtimeLibs;
	}

	/**
	 * libraries files needed to compile and run test code.
	 */
	public Iterable<File> getTestLibs() {
		return testLibs;
	}

	/**
	 * libraries files needed to compile production code but provided by the container running 
	 * the code... thus not part of the delivery.
	 */
	public Iterable<File> getProvidedLibs() {
		return providedLibs;
	}
	
	/**
	 * libraries finally used to compile the production code.
	 */
	@SuppressWarnings("unchecked")
	public Iterable<File> getComputedCompileLibs() {
		return IterableUtils.chain(compileLibs, providedLibs);
	}
	
	/**
	 * Libraries finally used both for compile and run test. 
	 */
	@SuppressWarnings("unchecked")
	public Iterable<File> getComputedTestLibs(File compiledClassesDir) {
		return IterableUtils.chain(compiledClassesDir, compileLibs, providedLibs, runtimeLibs, testLibs);
	}
	
	/**
	 * Libraries finally to be embedded in some delivery (as .war files). 
	 */
	@SuppressWarnings("unchecked")
	public Iterable<File> getComputedRuntimeLibs() {
		return IterableUtils.chain(compileLibs, runtimeLibs);
	}
	
	public static String asString(Iterable<File> files) {
		return FileUtils.asPath(files, ";");
	}
	

}
