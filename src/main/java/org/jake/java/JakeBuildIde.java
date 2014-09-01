package org.jake.java;

import java.io.File;
import java.io.FileFilter;
import java.net.URLClassLoader;
import java.util.List;
import java.util.zip.Deflater;

import org.jake.JakeBuildBase;
import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.utils.JakeUtilsClassloader;

/**
 * This build is meant to be executed by the IDE (as Eclipse) in the project context.
 * This means that the the classes are supposed to be already generated and the
 * class loader under this build is executed already holds every required dependencies.
 * 
 * @author Jerome Angibaud
 */
public class JakeBuildIde extends JakeBuildBase {

	protected String jarBaseName() {
		return projectName();
	}

	protected int zipLevel() {
		return Deflater.DEFAULT_COMPRESSION;
	}

	public void simpleJar() {
		JakeLog.start("Creating jar file");
		final List<File> classDirs = getProjectCompiledClasses();
		final File jarFile = ouputDir().file(jarBaseName() + ".jar");
		JakeUtilsFile.zipDir(jarFile, zipLevel(), classDirs);
		JakeLog.done(jarFile.getPath() + " created");
	}

	public void test() {
		JakeLog.start("Launching test(s)");
		//int count = TestUtils.launchJunitTests(ClassloaderUtils.current(), getProjectFileFilter());
		JakeLog.done(0 + " test(s) Launched.");
	}

	@Override
	public void base() {
		super.base();
		test();
		simpleJar();
	}

	public static void main(String[] args) {
		new JakeBuildIde().base();
	}

	protected final FileFilter getProjectFileFilter() {
		return new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return JakeUtilsFile.isAncestor(baseDir().root(), pathname);
			}
		};
	}

	protected List<File> getProjectCompiledClasses() {
		return JakeUtilsClassloader.getFolderClassEntriesUnder(baseDir().root(),
				(URLClassLoader) JakeBuildIde.class.getClassLoader());
	}

}
