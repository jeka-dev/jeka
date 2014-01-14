package org.jake.java;

import java.io.File;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.jake.JakeBaseBuild;
import org.jake.utils.FileUtils;

/**
 * This build is meant to be executed by the IDE (as Eclipse) in the project context.
 * This means that the the classes are supposed to be already generated and the 
 * class loader under this build is executed already holds every required dependencies. 
 *  
 * @author Jerome Angibaud
 */
public class JakeIdeBuild extends JakeBaseBuild {
	
	protected String jarBaseName() {
		return projectName();
	}
	
	protected int zipLevel() {
		return ZipOutputStream.DEFLATED;
	}
	
	public void simpleJar() {
		logger().info("Creating jar file");
		List<File> classDirs = ClasspathUtils.getClassEntryInsideProject(baseDir().getBase());
		File jarFile = buildOuputDir().file(jarBaseName() + ".jar");
		FileUtils.zipDir(jarFile, zipLevel(), classDirs);
		logger().info(jarFile.getPath() + " created");
	}
	
	public void test() {
		logger().info("Launching tests ...");
		int count = TestUtils.launchJunitTests(ClasspathUtils.getRunningJakeClassLoader(), baseDir().getBase());
		logger().info(count + " test(s) Lauched.");
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		test();
		simpleJar();
	}
	
	public static void main(String[] args) {
		new JakeIdeBuild().withArgs(args).doDefault();
	}

}
