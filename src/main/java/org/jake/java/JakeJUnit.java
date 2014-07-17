package org.jake.java;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jake.file.utils.FileUtils;
import org.jake.java.JakeTestResult.ExceptionDescription;
import org.jake.java.utils.JakeUtilsClassloader;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

public class JakeJUnit {

	private static final String JUNIT4_RUNNER_CLASS_NAME = "org.junit.runner.JUnitCore";

	private static final String JUNIT3_RUNNER_CLASS_NAME = "junit.textui.TestRunner";

	private static final String JUNIT4_TEST_ANNOTATION_CLASS_NAME = "org.junit.Test";

	private static final String JUNIT3_TEST_CASE_CLASS_NAME = "junit.framework.TestCase";

	private static final Class<?> ARRAY_OF_CLASSES_TYPE = new Class[0]
			.getClass();

	private final List<File> classpath;

	private JakeJUnit(Iterable<File> classpath) {
		this.classpath = JakeUtilsIterable.toList(classpath);
	}

	@SuppressWarnings("unchecked")
	public static JakeJUnit classpath(File dir, Iterable<File> dirs) {
		return new JakeJUnit(JakeUtilsIterable.concatToList(dir, dirs));
	}

	public static JakeJUnit classpath(Iterable<File> dirs) {
		return new JakeJUnit(dirs);
	}

	@SuppressWarnings("unchecked")
	public static JakeJUnit noClasspath() {
		return new JakeJUnit(Collections.EMPTY_LIST);
	}

	public JakeTestResult launchAll(File... testClassDirs) {
		return launchAll(Arrays.asList(testClassDirs), FileUtils.acceptAll(),
				JakeClassFilter.acceptAll());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JakeTestResult launchAll(Iterable<File> testClassDirs,
			FileFilter fileFilter, JakeClassFilter classFilter) {
		final Iterable<File> urls = JakeUtilsIterable.concatLists(testClassDirs,
				this.classpath);
		final Collection<Class> testClasses;

		final URLClassLoader classLoader = JakeUtilsClassloader.createFrom(urls,
				ClassLoader.getSystemClassLoader().getParent());
		testClasses = getJunitTestClassesInClassLoader(classLoader, fileFilter,
				true);

		final Collection<Class> effectiveClasses = new LinkedList<Class>();
		for (Class clazz : testClasses) {
			if (classFilter.accept(clazz)) {
				effectiveClasses.add(clazz);
			}
		}
		return launch(effectiveClasses);
	}

	@SuppressWarnings("rawtypes")
	public JakeTestResult launch(Iterable<Class> classes) {
		if (!classes.iterator().hasNext()) {
			return JakeTestResult.empty();
		}
		final ClassLoader classLoader = classes.iterator().next()
				.getClassLoader();
		if (isJunit4In(classLoader)) {
			Class<?>[] classArray = JakeUtilsIterable.toArray(classes, Class.class);
			Class<?> junitCoreClass = JakeUtilsClassloader.loadClass(classLoader,
					JUNIT4_RUNNER_CLASS_NAME);
			Method runClassesMethod = JakeUtilsReflect.getMethod(junitCoreClass,
					"runClasses", ARRAY_OF_CLASSES_TYPE);
			Object junit4Result = JakeUtilsReflect.invoke(null, runClassesMethod,
					(Object) classArray);
			return fromJunit4Result(junit4Result);
		}
		return JakeTestResult.empty();
	}

	private static JakeTestResult fromJunit4Result(Object result) {
		final Integer runCount = JakeUtilsReflect.invoke(result, "getRunCount");
		final Integer ignoreCount = JakeUtilsReflect.invoke(result,
				"getIgnoreCount");
		final List<Object> junitFailures = JakeUtilsReflect.invoke(result,
				"getFailures");
		final List<JakeTestResult.Failure> failures = new ArrayList<JakeTestResult.Failure>(
				junitFailures.size());
		for (Object junitFailure : junitFailures) {
			failures.add(fromJunit4Failure(junitFailure));
		}
		return new JakeTestResult(runCount, ignoreCount, failures);

	}

	private static JakeTestResult.Failure fromJunit4Failure(Object junit4failure) {
		Object junit4Description = JakeUtilsReflect.invoke(junit4failure,
				"getDescription");
		String testClassName = JakeUtilsReflect.invoke(junit4Description,
				"getClassName");
		String testMethodName = JakeUtilsReflect.invoke(junit4Description,
				"getMethodName");
		Throwable exception = JakeUtilsReflect
				.invoke(junit4failure, "getException");
		ExceptionDescription description = new ExceptionDescription(exception);
		return new JakeTestResult.Failure(testClassName, testMethodName,
				description);
	}

	@SuppressWarnings("rawtypes")
	public static Collection<Class> getJunitTestClassesInClassLoader(
			URLClassLoader classLoader, FileFilter entryFilter,
			boolean onlyFolder) {
		final Iterable<Class> classes = JakeUtilsClassloader.getAllTopLevelClasses(
				classLoader, entryFilter, onlyFolder);
		List<Class> testClasses = new LinkedList<Class>();
		if (isJunit4In(classLoader)) {
			Class<Annotation> testAnnotation = load(classLoader,
					JUNIT4_TEST_ANNOTATION_CLASS_NAME);
			Class<?> testCaseClass = load(classLoader,
					JUNIT3_TEST_CASE_CLASS_NAME);
			for (Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass)
						|| isJunit4Test(clazz, testAnnotation)) {
					testClasses.add(clazz);
				}
			}
		} else if (isJunit3In(classLoader)) {
			Class<?> testCaseClass = load(classLoader,
					JUNIT3_TEST_CASE_CLASS_NAME);
			for (Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass)) {
					testClasses.add(clazz);
				}
			}
		}
		return testClasses;
	}

	private static boolean isJunit4In(ClassLoader classLoader) {
		try {
			classLoader.loadClass(JUNIT4_RUNNER_CLASS_NAME);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static boolean isJunit3In(ClassLoader classLoader) {
		try {
			classLoader.loadClass(JUNIT3_RUNNER_CLASS_NAME);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static boolean isJunit3Test(Class<?> candidtateClazz,
			Class<?> testCaseClass) {
		if (Modifier.isAbstract(candidtateClazz.getModifiers())) {
			return false;
		}
		return testCaseClass.isAssignableFrom(candidtateClazz);
	}

	public static boolean isJunit4Test(Class<?> candidateClass,
			Class<Annotation> testAnnotation) {
		if (Modifier.isAbstract(candidateClass.getModifiers())) {
			return false;
		}
		return hasConcreteTestMethods(candidateClass, testAnnotation);
	}

	private static boolean hasConcreteTestMethods(Class<?> candidateClass,
			Class<Annotation> testAnnotation) {
		for (Method method : candidateClass.getMethods()) {
			int modifiers = method.getModifiers();
			if (!Modifier.isAbstract(modifiers) && Modifier.isPublic(modifiers)
					&& method.getAnnotation(testAnnotation) != null) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> load(ClassLoader classLoader, String name) {
		try {
			return (Class<T>) classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
