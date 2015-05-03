package org.jerkar.scriptsamples;

import java.io.File;

import org.jerkar.JkBuild;
import org.jerkar.JkJavaCompiler;
import org.jerkar.JkJavaProcess;
import org.jerkar.JkZipper;
import org.jerkar.builtins.javabuild.JkManifest;

/**
 * Equivalent to http://ant.apache.org/manual/index.html
 * 
 * @author Jerome Angibaud
 */
public class AntStyleBuild extends JkBuild {
	
	File src = baseDir("src");
	File buildDir = baseDir("build");
	File classesDir = new File(buildDir, "classes");
	File jarFile = new File(buildDir, "jar/" + moduleId().name() + ".jar");
	String className;
	
	@Override
	public void doDefault() {
		clean();run();
	}
	
	public void compile() {
		JkJavaCompiler.ofOutput(classesDir).andSourceDir(src).compile();
	}
	
	public void jar() {
		compile();
		JkManifest.empty().addMainClass("my.main.RunClass").writeToStandardLocation(classesDir);
		JkZipper.of(classesDir).to(jarFile);
	}	
	
	public void run() {
		jar();
		JkJavaProcess.of(jarFile).runSync();
	}
	
	public void cleanBuild() {
		clean();jar();
	}
	
}
