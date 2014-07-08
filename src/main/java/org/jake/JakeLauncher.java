package org.jake;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.file.DirView;
import org.jake.file.utils.FileUtils;
import org.jake.java.JavaCompilation;
import org.jake.java.utils.ClassloaderUtils;
import org.jake.utils.IterableUtils;
import org.jake.utils.ReflectUtils;

public class JakeLauncher {
	
	private static final String BUILD_SOURCE_DIR = "build/spec";
	
	private static final String BUILD_LIB_DIR = "build/libs/build";
	
	private static final String BUILD_BIN_DIR = "build/output/build-bin";
	
	public static void main(String[] args) {
		BuildOption.set(args);
		JakeLauncher launcher = new JakeLauncher();
		Iterable<File> compileClasspath = launcher.resolveBuildCompileClasspath();
		launcher.compileBuild(compileClasspath);
		launcher.launch(compileClasspath);
	}
	
	private void compileBuild(Iterable<File> classpath) {
		DirView buildSource = DirView.of(new File(BUILD_SOURCE_DIR));
		if (!buildSource.exists()) {
			throw new IllegalStateException(BUILD_SOURCE_DIR + " directory has not been found in this project. " +
					" This directory is supposed to contains build scripts (as form of java source");
		}
		JavaCompilation javaCompilation = new JavaCompilation();
		javaCompilation.addSourceFiles(buildSource.include("**/*.java"));
		javaCompilation.setClasspath(classpath);
		File buildBinDir = new File(BUILD_BIN_DIR);
		if (!buildBinDir.exists()) {
			buildBinDir.mkdirs();
		}
		javaCompilation.setOutputDirectory(buildBinDir);
		Notifier.start("Compiling build sources to " + buildBinDir.getAbsolutePath());
		boolean result = javaCompilation.compile();
		Notifier.done();
		if (result == false) {
			Notifier.error("Build script can't be compiled.");
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void launch(Iterable<File> compileClasspath) {
		File buildBin = new File(BUILD_BIN_DIR);
		Iterable<File> runtimeClassPath = IterableUtils.concatToList(buildBin, compileClasspath); 
		URLClassLoader classLoader = ClassloaderUtils.createFrom(runtimeClassPath, JakeLauncher.class.getClassLoader());
		
		// Find the Build class
		Class<?> jakeBaseBuildClass = ClassloaderUtils.loadClass(classLoader, JakeBaseBuild.class.getName());
		Set<Class> classes = ClassloaderUtils.getAllTopLevelClasses(classLoader, FileUtils.acceptAll(), true);
		List<Class> buildClasses = new LinkedList<Class>();
		for (Class clazz : classes) {
			if (jakeBaseBuildClass.isAssignableFrom(clazz)) {
				buildClasses.add(clazz);
			}
		}
		if (buildClasses.isEmpty()) {
			throw new IllegalStateException("No build class found.");
		}
		
		Class buildClass = buildClasses.get(0);
		
		Object build = ReflectUtils.newInstance(buildClass);
		String command = "doDefault";
		Notifier.info("Use Build class " + buildClass.getCanonicalName() + " with operation " + command);
		ReflectUtils.invoke(build, command);	
	}
	
	public List<File> resolveBuildCompileClasspath() {
		final URL[] urls = ClassloaderUtils.current().getURLs();
		final List<File> result = FileUtils.toFiles(urls);
		final File buildLibDir = new File(BUILD_LIB_DIR);
		if (buildLibDir.exists() && buildLibDir.isDirectory()) {
			final List<File> libs = FileUtils.filesOf(buildLibDir, FileUtils.endingBy(".jar"), true);
			for (File file : libs) {
				result.add(file);
			}
		}
		final File jakeJarFile = getJakeJarFile();
		final File extLibDir = new File(jakeJarFile.getParentFile(), "ext");
		if (extLibDir.exists() && extLibDir.isDirectory()) {
			result.addAll(FileUtils.filesOf(extLibDir, FileUtils.endingBy(".jar"), false));
		}
		return result;
	}
	
	private static File getJakeJarFile() {
		URL[] urls = ClassloaderUtils.current().getURLs();
		for (URL url : urls) {
			File file = new File(url.getFile());
			URLClassLoader classLoader = ClassloaderUtils.createFrom(IterableUtils.single(file), 
					ClassLoader.getSystemClassLoader().getParent());
			try {
				classLoader.loadClass(JakeLauncher.class.getName());
				return file;
			} catch (ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("JakeLauncher not found in classpath");
	}
	
}
