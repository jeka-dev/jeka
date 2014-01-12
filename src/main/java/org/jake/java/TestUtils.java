package org.jake.java;

import java.io.File;

import org.jake.utils.IterableUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

@SuppressWarnings("rawtypes")
public final class TestUtils {
	
	
	public static Result launchJunit(Iterable<Class> classes) {
		Class[] classArray = IterableUtils.asArray(classes, Class.class);
		return launchJunit(classArray);
	}
	
	
	public static Result launchJunit(Class... classes) {
		return JUnitCore.runClasses(classes);
	}
	
	public static Result launchJunitOnAllClasses(ClassLoader classLoader, File projectDir) {
		File entry = ClasspathUtils.getClassEntryInsideProject(projectDir).get(0);
		Class[] classes = ClasspathUtils.getAllTopLevelClassesAsArray(classLoader, entry);
		return launchJunit(classes);
	}
	
	

}
