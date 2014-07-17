package org.jake.java;

import java.io.File;

import org.jake.JakeBaseBuild;
import org.jake.JakeLogger;
import org.jake.file.JakeDirViewSet;
import org.jake.file.JakeFileFilter;
import org.jake.file.JakeZip;
import org.jake.java.eclipse.JakeEclipse;
import org.jake.utils.JakeUtilsIterable;

public class JakeJavaBuild extends JakeBaseBuild {
	
	protected static final JakeFileFilter JAVA_SOURCE_ONLY_FILTER = JakeFileFilter.include("**/*.java");
	
	protected static final String STD_LIB_PATH = "build/libs"; 
	
	protected static final JakeFileFilter RESOURCE_FILTER = JakeFileFilter.exclude("**/*.java")
			.andExcludeAll("**/package.html").andExcludeAll("**/doc-files");
	
	/**
	 * Returns location of production source code.
	 */
	protected JakeDirViewSet sourceDirs() {
		return JakeDirViewSet.of( baseDir().sub("src/main/java") );
	}
	
	/**
	 * Returns location of production resources.
	 */
	protected JakeDirViewSet resourceDirs() {
		return sourceDirs().withFilter(RESOURCE_FILTER).and(baseDir().sub("src/main/resources"));
	} 
	
	/**
	 * Returns location of test source code.
	 */
	protected JakeDirViewSet testSourceDirs() {
		return JakeDirViewSet.of( baseDir().sub("src/test/java") );
	}
	
	/**
	 * Returns location of test resources.
	 */
	protected JakeDirViewSet testResourceDirs() {
		return JakeDirViewSet.of(baseDir().sub("src/test/resources"))
				.and(testSourceDirs().withFilter(RESOURCE_FILTER));
	} 
		
		
	protected File classDir() {
		return buildOuputDir().sub("classes").createIfNotExist().root();
	}
	
	protected File testClassDir() {
		return buildOuputDir().sub("testClasses").createIfNotExist().root();
	}
	
	protected JakeJavaDependencyResolver dependencyPath() {
		final File folder = baseDir(STD_LIB_PATH);
		if (folder.exists()) {
			return JakeLocalDependencyResolver.standard(baseDir(STD_LIB_PATH));
		} else if (JakeEclipse.isDotClasspathPresent(baseDir().root())) {
			return JakeEclipse.dependencyResolver(baseDir().root());
		} else {
			return JakeLocalDependencyResolver.empty();
		}
						
	}
	
	
	// ------------ Operations ------------
	
	protected JakeJUnit juniter() {
		return JakeJUnit.classpath(this.classDir(), this.dependencyPath().test());
	}
	
	
	protected void compile(JakeDirViewSet sources, File destination, Iterable<File> classpath) {
		JakeJavaCompiler compilation = new JakeJavaCompiler();
		JakeDirViewSet javaSources = sources.withFilter(JAVA_SOURCE_ONLY_FILTER);
		JakeLogger.start("Compiling " + javaSources.countFiles(false) + " source files to " + destination.getPath());
	    compilation.addSourceFiles(javaSources.listFiles());
	    compilation.setClasspath(classpath);
	    compilation.setOutputDirectory(destination);
	    compilation.compileOrFail();
	    JakeLogger.done();
	}
	
	/**
	 * Compiles production code.
	 */
	public void compile() {
		compile(sourceDirs(), classDir(), this.dependencyPath().compile());
	}
	
	/**
	 * Compiles test code.
	 */
	@SuppressWarnings("unchecked")
	public void compileTest() {
		compile(testSourceDirs(), testClassDir(), 
				JakeUtilsIterable.concatToList(this.classDir(), this.dependencyPath().test()));
	}
	
	/**
	 * Copies production resources in <code>class dir</code>. 
	 */
	public void copyResources() {
		JakeLogger.start("Coping resource files to " + classDir().getPath());
		int count = resourceDirs().copyTo(classDir());
		JakeLogger.done(count + " file(s) copied.");
	}
	
	/**
	 * Copies test resource in <code>test class dir</code>. 
	 */
	public void copyTestResources() {
		JakeLogger.start("Coping test resource files to " + testClassDir().getPath());
		int count = testResourceDirs().copyTo(testClassDir());
		JakeLogger.done(count + " file(s) copied.");
	}
	
	public void runUnitTests() {
		JakeLogger.start("Launching JUnit Tests");
		juniter().launchAll(this.testClassDir()).printToNotifier();
		JakeLogger.done();
	}
	
	public void javadoc() {
		JakeLogger.start("Generating Javadoc");
		File dir = buildOuputDir(projectName() + "-javadoc");
		JakeJavadoc.of(this.sourceDirs()).withClasspath(this.dependencyPath().compile()).process(dir);
		if (dir.exists()) {
			JakeZip.of(dir).create(buildOuputDir(projectName() + "-javadoc.zip"));
		}
		JakeLogger.done();
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		compile();
		copyResources();
		compileTest();
		copyTestResources();
		runUnitTests();
	}
	
	public static void main(String[] args) {
		new JakeJavaBuild().doDefault();
	}
	

}
