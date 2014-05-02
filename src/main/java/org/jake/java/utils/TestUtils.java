package org.jake.java.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.jake.java.ClassFilter;
import org.jake.utils.IterableUtils;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class TestUtils {
	
	private static final String JUNIT4_RUNNER_CLASS_NAME = "org.junit.runner.JUnitCore";
	
	private static final String JUNIT3_RUNNER_CLASS_NAME = "junit.textui.TestRunner";
	
	private static final String JUNIT3_TEST_CASE_CLASS_NAME = "junit.framework.TestCase";
	
	private static final String JUNIT4_TEST_ANNOTATION_CLASS_NAME = "org.junit.Test";
	
	
	
	public static boolean isJunit4InClasspath(ClassLoader classLoader)  {
		try {
			classLoader.loadClass(JUNIT4_RUNNER_CLASS_NAME);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public static boolean isJunit3InClassPath(ClassLoader classLoader) {
		try {
			classLoader.loadClass(JUNIT3_RUNNER_CLASS_NAME);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public static int launchJunitTests(ClassLoader classLoader, File projectDir) {
		return launchJunitTests(classLoader, projectDir, ClassFilter.acceptAll());
	}
	
	public static int launchJunitTests(ClassLoader classLoader, File projectDir, ClassFilter classFilter) {
		Collection<Class> classes = getJunitTestClassesInProject(classLoader, projectDir);
		Collection<Class> effectiveClasses = new LinkedList<Class>();
		for (Class clazz : classes) {
			if (classFilter.accept(clazz)) {
				effectiveClasses.add(clazz);
			}
		}
		return launchJunitTests(classLoader, effectiveClasses);
	}
	
	/**
	 *  @return The count of test run.
	 */
	public static int launchJunitTests(ClassLoader classLoader, Collection<Class> classes) {
				
		if (isJunit4InClasspath(classLoader)) {
			Class[] classArray = IterableUtils.toArray(classes, Class.class);
			Result result = JUnitCore.runClasses(classArray);
			return result.getRunCount();
		
		} 
		if (isJunit3InClassPath(classLoader)) {
			int i = 0;
			for (Class clazz : classes) {
				System.out.println(clazz);
				i = i + TestRunner.run(new TestSuite(clazz)).runCount();
			}
			return i;
		}
		return 0;
	}
	
	public static Collection<Class> getJunitTestClassesInProject(ClassLoader classLoader, File projectDir) {
		File entry = ClasspathUtils.getClassEntryInsideProject(projectDir).get(0);
		Iterable<Class> classes = ClasspathUtils.getAllTopLevelClasses(classLoader, entry);
		List<Class> testClasses = new LinkedList<Class>();
		if (isJunit4InClasspath(classLoader)) {
			Class<Test> testAnnotation = load(classLoader, JUNIT4_TEST_ANNOTATION_CLASS_NAME);
			Class<TestCase> testCaseClass = load(classLoader, JUNIT3_TEST_CASE_CLASS_NAME);
			for (Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass) || isJunit4Test(clazz, testAnnotation)) {
					testClasses.add(clazz);
				}
			}
		} else if (isJunit3InClassPath(classLoader)) {
			Class<TestCase> testCaseClass = load(classLoader, JUNIT3_TEST_CASE_CLASS_NAME);
			for (Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass)) {
					testClasses.add(clazz);
				}
			}
		}
		return testClasses;
	}
	
	public static boolean isJunit3Test(Class candidtateClazz, Class<TestCase> testCaseClass) {
		if (Modifier.isAbstract(candidtateClazz.getModifiers())) {
			return false;
		}
		return testCaseClass.isAssignableFrom(candidtateClazz);
	}
	
	public static boolean isJunit4Test(Class candidateClass, Class<Test> testAnnotation) {
		if (Modifier.isAbstract(candidateClass.getModifiers())) {
			return false;
		}
		return hasConcreteTestMethods(candidateClass, testAnnotation);
	}
	
	private static <T> Class<T> load(ClassLoader classLoader, String name) {
		try {
			return (Class<T>) classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static boolean hasConcreteTestMethods(Class candidateClass, Class<Test> testAnnotation) {
		for (Method method : candidateClass.getMethods()) {
			int modifiers = method.getModifiers();
			if (!Modifier.isAbstract(modifiers) 
					&& Modifier.isPublic(modifiers)
					&& method.getAnnotation(testAnnotation) != null) {
				return true;
			}
		}
		return false;
	}
	

}
