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
import org.jake.java.TestResult.ExceptionDescription;
import org.jake.java.utils.ClassloaderUtils;
import org.jake.utils.IterableUtils;
import org.jake.utils.ReflectUtils;

public class JUniter {

	private static final String JUNIT4_RUNNER_CLASS_NAME = "org.junit.runner.JUnitCore";

	private static final String JUNIT3_RUNNER_CLASS_NAME = "junit.textui.TestRunner";

	private static final String JUNIT4_TEST_ANNOTATION_CLASS_NAME = "org.junit.Test";

	private static final String JUNIT3_TEST_CASE_CLASS_NAME = "junit.framework.TestCase";

	private static final Class<?> ARRAY_OF_CLASSES_TYPE = new Class[0]
			.getClass();

	private final List<File> classpath;

	private JUniter(Iterable<File> classpath) {
		this.classpath = IterableUtils.toList(classpath);
	}

	@SuppressWarnings("unchecked")
	public static JUniter classpath(File dir, Iterable<File> dirs) {
		return new JUniter(IterableUtils.concatToList(dir, dirs));
	}

	public static JUniter classpath(Iterable<File> dirs) {
		return new JUniter(dirs);
	}

	@SuppressWarnings("unchecked")
	public static JUniter noClasspath() {
		return new JUniter(Collections.EMPTY_LIST);
	}

	public TestResult launchAll(File... testClassDirs) {
		return launchAll(Arrays.asList(testClassDirs), FileUtils.acceptAll(),
				ClassFilter.acceptAll());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TestResult launchAll(Iterable<File> testClassDirs,
			FileFilter fileFilter, ClassFilter classFilter) {
		final Iterable<File> urls = IterableUtils.concatLists(testClassDirs,
				this.classpath);
		final Collection<Class> testClasses;

		final URLClassLoader classLoader = ClassloaderUtils.createFrom(urls,
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
	public TestResult launch(Iterable<Class> classes) {
		if (!classes.iterator().hasNext()) {
			return TestResult.empty();
		}
		final ClassLoader classLoader = classes.iterator().next()
				.getClassLoader();
		if (isJunit4In(classLoader)) {
			Class<?>[] classArray = IterableUtils.toArray(classes, Class.class);
			Class<?> junitCoreClass = ClassloaderUtils.loadClass(classLoader,
					JUNIT4_RUNNER_CLASS_NAME);
			Method runClassesMethod = ReflectUtils.getMethod(junitCoreClass,
					"runClasses", ARRAY_OF_CLASSES_TYPE);
			Object junit4Result = ReflectUtils.invoke(null, runClassesMethod,
					(Object) classArray);
			return fromJunit4Result(junit4Result);
		}
		return TestResult.empty();
	}

	private static TestResult fromJunit4Result(Object result) {
		final Integer runCount = ReflectUtils.invoke(result, "getRunCount");
		final Integer ignoreCount = ReflectUtils.invoke(result,
				"getIgnoreCount");
		final List<Object> junitFailures = ReflectUtils.invoke(result,
				"getFailures");
		final List<TestResult.Failure> failures = new ArrayList<TestResult.Failure>(
				junitFailures.size());
		for (Object junitFailure : junitFailures) {
			failures.add(fromJunit4Failure(junitFailure));
		}
		return new TestResult(runCount, ignoreCount, failures);

	}

	private static TestResult.Failure fromJunit4Failure(Object junit4failure) {
		Object junit4Description = ReflectUtils.invoke(junit4failure,
				"getDescription");
		String testClassName = ReflectUtils.invoke(junit4Description,
				"getClassName");
		String testMethodName = ReflectUtils.invoke(junit4Description,
				"getMethodName");
		Throwable exception = ReflectUtils
				.invoke(junit4failure, "getException");
		ExceptionDescription description = new ExceptionDescription(exception);
		return new TestResult.Failure(testClassName, testMethodName,
				description);
	}

	@SuppressWarnings("rawtypes")
	public static Collection<Class> getJunitTestClassesInClassLoader(
			URLClassLoader classLoader, FileFilter entryFilter,
			boolean onlyFolder) {
		final Iterable<Class> classes = ClassloaderUtils.getAllTopLevelClasses(
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
