package org.jake.java.test;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLog;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeClassFilter;
import org.jake.java.test.JakeTestResult.ExceptionDescription;
import org.jake.java.utils.JakeUtilsClassloader;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

public class JakeJUnit {

	private static final String JUNIT4_RUNNER_CLASS_NAME = "org.junit.runner.JUnitCore";

	private static final String JUNIT3_RUNNER_CLASS_NAME = "junit.textui.TestRunner";

	private static final String JUNIT4_TEST_ANNOTATION_CLASS_NAME = "org.junit.Test";

	private static final String JUNIT3_TEST_CASE_CLASS_NAME = "junit.framework.TestCase";

	private static final String JUNIT3_TEST_SUITE_CLASS_NAME = "junit.framework.TestSuite";

	private static final String JUNIT3_TEST_RESULT_CLASS_NAME = "junit.framework.TestResult";

	private static final Class<?> ARRAY_OF_CLASSES_TYPE = new Class[0]
			.getClass();

	private final List<File> classpath;

	private JakeJUnit(Iterable<File> classpath) {
		this.classpath = JakeUtilsIterable.toList(classpath);
	}

	@SuppressWarnings("unchecked")
	public static JakeJUnit ofClasspath(File dir, Iterable<File> dirs) {
		return new JakeJUnit(JakeUtilsIterable.concatToList(dir, dirs));
	}

	public static JakeJUnit ofCclasspath(Iterable<File> dirs) {
		return new JakeJUnit(dirs);
	}

	public JakeJUnit withExtraLibsInClasspath(File ...files) {
		return new JakeJUnit(JakeUtilsIterable.chain(this.classpath, files));
	}

	@SuppressWarnings("unchecked")
	public JakeJUnit withExtraLibsInClasspath(Iterable<File> files) {
		return new JakeJUnit(JakeUtilsIterable.concatLists(this.classpath, files));
	}

	public JakeTestResult launchAll(File... testClassDirs) {
		return launchAll(Arrays.asList(testClassDirs), JakeUtilsFile.acceptAll(),
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
		testClasses = getJunitTestClassesInClassLoader(classLoader, fileFilter);

		final Collection<Class> effectiveClasses = new LinkedList<Class>();
		for (final Class clazz : testClasses) {
			if (classFilter.accept(clazz)) {
				effectiveClasses.add(clazz);
			}
		}
		return launch(effectiveClasses);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JakeTestResult launch(Iterable<Class> classes) {
		JakeLog.flush();

		if (!classes.iterator().hasNext()) {
			return JakeTestResult.empty(0);
		}
		final long start = System.nanoTime();
		final ClassLoader classLoader = classes.iterator().next()
				.getClassLoader();
		if (isJunit4In(classLoader)) {
			final Class<?>[] classArray = JakeUtilsIterable.toArray(classes, Class.class);
			final Class<?> junitCoreClass = JakeUtilsClassloader.loadClass(classLoader,
					JUNIT4_RUNNER_CLASS_NAME);
			final Method runClassesMethod = JakeUtilsReflect.getMethod(junitCoreClass,
					"runClasses", ARRAY_OF_CLASSES_TYPE);
			final Object junit4Result = JakeUtilsReflect.invoke(null, runClassesMethod,
					(Object) classArray);
			final long end = System.nanoTime();
			final long duration = (end - start) / 1000000;
			return fromJunit4Result(junit4Result, duration);
		} else if (isJunit3In(classLoader)) {
			final Object suite = createJunit3TestSuite(classLoader, classes);
			final Class testResultClass = JakeUtilsClassloader.loadClass(classLoader, JUNIT3_TEST_RESULT_CLASS_NAME);
			final Object testResult = JakeUtilsReflect.newInstance(testResultClass);
			final Method runMethod = JakeUtilsReflect.getMethod(suite.getClass(),
					"run", testResultClass);
			JakeUtilsReflect.invoke(suite, runMethod, testResult);
			final long end = System.nanoTime();
			final long duration = (end - start) / 1000000;
			return fromJunit3Result(testResult, duration);
		}
		throw new IllegalStateException("No Junit found on test classpath.");
	}

	private static JakeTestResult fromJunit4Result(Object result, long durationInMillis) {
		final Integer runCount = JakeUtilsReflect.invoke(result, "getRunCount");
		final Integer ignoreCount = JakeUtilsReflect.invoke(result,
				"getIgnoreCount");
		final List<Object> junitFailures = JakeUtilsReflect.invoke(result,
				"getFailures");
		final List<JakeTestResult.Failure> failures = new ArrayList<JakeTestResult.Failure>(
				junitFailures.size());
		for (final Object junitFailure : junitFailures) {
			failures.add(fromJunit4Failure(junitFailure));
		}
		return new JakeTestResult(runCount, ignoreCount, failures, durationInMillis);

	}

	private static JakeTestResult.Failure fromJunit4Failure(Object junit4failure) {
		final Object junit4Description = JakeUtilsReflect.invoke(junit4failure,
				"getDescription");
		final String testClassName = JakeUtilsReflect.invoke(junit4Description,
				"getClassName");
		final String testMethodName = JakeUtilsReflect.invoke(junit4Description,
				"getMethodName");
		final Throwable exception = JakeUtilsReflect
				.invoke(junit4failure, "getException");
		final ExceptionDescription description = new ExceptionDescription(exception);
		return new JakeTestResult.Failure(testClassName, testMethodName,
				description);
	}

	private static JakeTestResult.Failure fromJunit3Failure(Object junit3failure) {
		final Object failedTest = JakeUtilsReflect.invoke(junit3failure,
				"failedTest");
		final Throwable exception = JakeUtilsReflect.invoke(junit3failure,
				"thrownException");
		final ExceptionDescription description = new ExceptionDescription(exception);
		final String failedTestName = failedTest.toString();
		final int firstParenthesisIndex = failedTestName.indexOf("(");
		final String methodName = failedTestName.substring(0, firstParenthesisIndex);
		return new JakeTestResult.Failure(failedTest.getClass().getName(), methodName,
				description);
	}

	@SuppressWarnings("rawtypes")
	public static Collection<Class> getJunitTestClassesInClassLoader(
			URLClassLoader classLoader, FileFilter entryFilter) {
		final Iterable<Class<?>> classes = JakeUtilsClassloader.getAllTopLevelClasses(
				classLoader, entryFilter);
		final List<Class> testClasses = new LinkedList<Class>();
		if (isJunit4In(classLoader)) {
			final Class<Annotation> testAnnotation = load(classLoader,
					JUNIT4_TEST_ANNOTATION_CLASS_NAME);
			final Class<?> testCaseClass = load(classLoader,
					JUNIT3_TEST_CASE_CLASS_NAME);
			for (final Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass)
						|| isJunit4Test(clazz, testAnnotation)) {
					testClasses.add(clazz);
				}
			}
		} else if (isJunit3In(classLoader)) {
			final Class<?> testCaseClass = load(classLoader,
					JUNIT3_TEST_CASE_CLASS_NAME);
			for (final Class clazz : classes) {
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
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	private static boolean isJunit3In(ClassLoader classLoader) {
		try {
			classLoader.loadClass(JUNIT3_RUNNER_CLASS_NAME);
			return true;
		} catch (final ClassNotFoundException e) {
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
		for (final Method method : candidateClass.getMethods()) {
			final int modifiers = method.getModifiers();
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
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	private static Object createJunit3TestSuite(ClassLoader classLoader, Iterable<Class> testClasses) {
		final Class<?>[] classArray = JakeUtilsIterable.toArray(testClasses, Class.class);
		final Class<?> testSuiteClass = JakeUtilsClassloader.loadClass(classLoader,
				JUNIT3_TEST_SUITE_CLASS_NAME);
		try {
			final Constructor constructor =  testSuiteClass.getConstructor(classArray.getClass());
			return constructor.newInstance((Object)classArray);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static JakeTestResult fromJunit3Result(Object result, long durationInMillis) {
		final Integer runCount = JakeUtilsReflect.invoke(result, "runCount");
		final Integer ignoreCount = 0;
		final Enumeration<Object> junitFailures = JakeUtilsReflect.invoke(result,
				"failures");
		final Enumeration<Object> junitErrors = JakeUtilsReflect.invoke(result,
				"errors");
		final List<JakeTestResult.Failure> failures = new ArrayList<JakeTestResult.Failure>();
		while(junitFailures.hasMoreElements()) {
			final Object junitFailure = junitFailures.nextElement();
			failures.add(fromJunit3Failure(junitFailure));
		}
		while(junitErrors.hasMoreElements()) {
			final Object junitError = junitErrors.nextElement();
			failures.add(fromJunit3Failure(junitError));
		}
		return new JakeTestResult(runCount, ignoreCount, failures, durationInMillis);

	}

}
