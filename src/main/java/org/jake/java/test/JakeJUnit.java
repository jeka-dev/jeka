package org.jake.java.test;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.java.JakeClassFilter;
import org.jake.java.JakeClassloader;
import org.jake.java.JakeClasspath;
import org.jake.java.test.JakeTestSuiteResult.ExceptionDescription;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

public final class JakeJUnit {

	public enum JunitReportDetail {
		NONE, BASIC, FULL;
	}

	private static final String JUNIT4_RUNNER_CLASS_NAME = "org.junit.runner.JUnitCore";

	private static final String JUNIT3_RUNNER_CLASS_NAME = "junit.textui.TestRunner";

	private static final String JUNIT4_TEST_ANNOTATION_CLASS_NAME = "org.junit.Test";

	private static final String JUNIT3_TEST_CASE_CLASS_NAME = "junit.framework.TestCase";

	private static final String JUNIT3_TEST_SUITE_CLASS_NAME = "junit.framework.TestSuite";

	private static final String JUNIT3_TEST_RESULT_CLASS_NAME = "junit.framework.TestResult";

	private static final Class<?> ARRAY_OF_CLASSES_TYPE = new Class[0]
			.getClass();

	private final JakeClasspath classpath;

	private final JunitReportDetail reportDetail;

	private final File reportDir;

	private JakeJUnit(Iterable<File> classpath, JunitReportDetail reportDetail, File reportDir) {
		this.classpath = JakeClasspath.of(classpath);
		this.reportDetail = reportDetail;
		this.reportDir = reportDir;
	}

	@SuppressWarnings("unchecked")
	public static JakeJUnit ofClasspath(File dir, Iterable<File> dirs) {
		return new JakeJUnit(JakeUtilsIterable.concatToList(dir, dirs), JunitReportDetail.NONE, null);
	}

	public static JakeJUnit ofCclasspath(Iterable<File> dirs) {
		return new JakeJUnit(dirs, JunitReportDetail.NONE, null);
	}

	public JakeJUnit withExtraLibsInClasspath(File ...files) {
		return withExtraLibsInClasspath(Arrays.asList(files));
	}

	public JakeJUnit withExtraLibsInClasspath(Iterable<File> files) {
		return new JakeJUnit(JakeClasspath.of(this.classpath).with(files), this.reportDetail, this.reportDir);
	}

	public JakeJUnit withReport(JunitReportDetail reportDetail, File reportDir) {
		return new JakeJUnit(this.classpath, reportDetail, reportDir);
	}

	public JakeTestSuiteResult launchAll(File... testClassDirs) {
		return launchAll(Arrays.asList(testClassDirs),
				JakeClassFilter.acceptAll());
	}

	@SuppressWarnings({ "rawtypes" })
	public JakeTestSuiteResult launchAll(final Iterable<File> testClassDirs, JakeClassFilter classFilter) {
		final Collection<Class> testClasses;
		final JakeClasspath classpath = this.classpath.withFirst(testClassDirs);
		final JakeClassloader classLoader = JakeClassloader.system().parent().createChild(classpath);
		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				for (final File testClassDir : testClassDirs ) {
					if (pathname.equals(testClassDir)) {
						return true;
					}
				}
				return false;
			}
		};
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
	public JakeTestSuiteResult launch(Iterable<Class> classes) {
		final String name = getSuiteName(classes);

		if (!classes.iterator().hasNext()) {
			return JakeTestSuiteResult.empty(name, 0);
		}
		final long start = System.nanoTime();
		final JakeClassloader classLoader = JakeClassloader.of(classes.iterator().next());
		JakeLog.startAndNextLine("Run JUnit tests");

		if (JakeOptions.isVerbose()) {
			JakeLog.info("-------------------------------------> Here starts the test output in console.");
		} else {
			System.setOut(JakeUtilsIO.nopPrintStream());
			System.setErr(JakeUtilsIO.nopPrintStream());
		}
		final JakeTestSuiteResult result;
		try {
			if (classLoader.isDefined(JUNIT4_RUNNER_CLASS_NAME)) {
				final Class<?>[] classArray = JakeUtilsIterable.toArray(classes, Class.class);
				final Class<?> junitCoreClass = classLoader.load(JUNIT4_RUNNER_CLASS_NAME);
				final Method runClassesMethod = JakeUtilsReflect.getMethod(junitCoreClass,
						"runClasses", ARRAY_OF_CLASSES_TYPE);
				final Object junit4Result = JakeUtilsReflect.invoke(null, runClassesMethod,
						(Object) classArray);
				final long end = System.nanoTime();
				final long duration = (end - start) / 1000000;
				result = fromJunit4Result(name, junit4Result, duration);
			} else if (classLoader.isDefined(JUNIT3_RUNNER_CLASS_NAME)) {
				final Object suite = createJunit3TestSuite(classLoader, classes);
				final Class testResultClass = classLoader.load(JUNIT3_TEST_RESULT_CLASS_NAME);
				final Object testResult = JakeUtilsReflect.newInstance(testResultClass);
				final Method runMethod = JakeUtilsReflect.getMethod(suite.getClass(),
						"run", testResultClass);
				JakeUtilsReflect.invoke(suite, runMethod, testResult);
				final long end = System.nanoTime();
				final long duration = (end - start) / 1000000;
				result = fromJunit3Result(name, testResult, duration);
			} else {
				throw new IllegalStateException("No Junit found on test classpath.");
			}

		} finally {
			if (JakeOptions.isVerbose()) {
				JakeLog.info("-------------------------------------> Here stops the test output in console.");
				JakeLog.nextLine();
			} else {
				System.setOut(null);
				System.setErr(null);
			}
		}

		JakeLog.info(result.toStrings(JakeOptions.isVerbose()));
		if (!JakeOptions.isVerbose() && result.failureCount() > 0) {
			JakeLog.info("Launch Jake in verbose mode to display failure stack traces in console.");
		}
		if (reportDetail != JunitReportDetail.NONE) {
			JakeTestReportBuilder.of(result).writeToFileSystem(reportDir);
		}
		JakeLog.done("Tests run");
		return result;
	}

	private static JakeTestSuiteResult fromJunit4Result(String suiteName, Object result, long durationInMillis) {
		final Integer runCount = JakeUtilsReflect.invoke(result, "getRunCount");
		final Integer ignoreCount = JakeUtilsReflect.invoke(result,
				"getIgnoreCount");
		final List<Object> junitFailures = JakeUtilsReflect.invoke(result,
				"getFailures");
		final List<JakeTestSuiteResult.Failure> failures = new ArrayList<JakeTestSuiteResult.Failure>(
				junitFailures.size());
		for (final Object junitFailure : junitFailures) {
			failures.add(fromJunit4Failure(junitFailure));
		}
		return new JakeTestSuiteResult(suiteName, runCount, ignoreCount, failures, durationInMillis);

	}

	private static JakeTestSuiteResult.Failure fromJunit4Failure(Object junit4failure) {
		final Object junit4Description = JakeUtilsReflect.invoke(junit4failure,
				"getDescription");
		final String testClassName = JakeUtilsReflect.invoke(junit4Description,
				"getClassName");
		final String testMethodName = JakeUtilsReflect.invoke(junit4Description,
				"getMethodName");
		final Throwable exception = JakeUtilsReflect
				.invoke(junit4failure, "getException");
		final ExceptionDescription description = new ExceptionDescription(exception);
		return new JakeTestSuiteResult.Failure(testClassName, testMethodName,
				description);
	}

	private static JakeTestSuiteResult.Failure fromJunit3Failure(Object junit3failure) {
		final Object failedTest = JakeUtilsReflect.invoke(junit3failure,
				"failedTest");
		final Throwable exception = JakeUtilsReflect.invoke(junit3failure,
				"thrownException");
		final ExceptionDescription description = new ExceptionDescription(exception);
		final String failedTestName = failedTest.toString();
		final int firstParenthesisIndex = failedTestName.indexOf("(");
		final String methodName = failedTestName.substring(0, firstParenthesisIndex);
		return new JakeTestSuiteResult.Failure(failedTest.getClass().getName(), methodName,
				description);
	}

	@SuppressWarnings("rawtypes")
	private static Collection<Class> getJunitTestClassesInClassLoader(
			JakeClassloader classloader, FileFilter entryFilter) {
		final Iterable<Class<?>> classes = classloader.getAllTopLevelClasses(entryFilter);
		final List<Class> testClasses = new LinkedList<Class>();
		if (classloader.isDefined(JUNIT4_RUNNER_CLASS_NAME)) {
			final Class<Annotation> testAnnotation = classloader.load(JUNIT4_TEST_ANNOTATION_CLASS_NAME);
			final Class<?> testCaseClass = classloader.load(JUNIT3_TEST_CASE_CLASS_NAME);
			for (final Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass)
						|| isJunit4Test(clazz, testAnnotation)) {
					testClasses.add(clazz);
				}
			}
		} else if (classloader.isDefined(JUNIT3_RUNNER_CLASS_NAME)) {
			final Class<?> testCaseClass = classloader.load(JUNIT3_TEST_CASE_CLASS_NAME);
			for (final Class clazz : classes) {
				if (isJunit3Test(clazz, testCaseClass)) {
					testClasses.add(clazz);
				}
			}
		}
		return testClasses;
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
	private static Object createJunit3TestSuite(JakeClassloader classLoader, Iterable<Class> testClasses) {
		final Class<?>[] classArray = JakeUtilsIterable.toArray(testClasses, Class.class);
		final Class<?> testSuiteClass = classLoader.load(JUNIT3_TEST_SUITE_CLASS_NAME);
		try {
			final Constructor constructor =  testSuiteClass.getConstructor(classArray.getClass());
			return constructor.newInstance((Object)classArray);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static JakeTestSuiteResult fromJunit3Result(String suiteName, Object result, long durationInMillis) {
		final Integer runCount = JakeUtilsReflect.invoke(result, "runCount");
		final Integer ignoreCount = 0;
		final Enumeration<Object> junitFailures = JakeUtilsReflect.invoke(result,
				"failures");
		final Enumeration<Object> junitErrors = JakeUtilsReflect.invoke(result,
				"errors");
		final List<JakeTestSuiteResult.Failure> failures = new ArrayList<JakeTestSuiteResult.Failure>();
		while(junitFailures.hasMoreElements()) {
			final Object junitFailure = junitFailures.nextElement();
			failures.add(fromJunit3Failure(junitFailure));
		}
		while(junitErrors.hasMoreElements()) {
			final Object junitError = junitErrors.nextElement();
			failures.add(fromJunit3Failure(junitError));
		}
		return new JakeTestSuiteResult(suiteName, runCount, ignoreCount, failures, durationInMillis);

	}

	@SuppressWarnings("rawtypes")
	private static String getSuiteName(Iterable<Class> classes) {
		final Iterator<Class> it = classes.iterator();
		if (!it.hasNext()) {
			return "";
		}
		final Class<?> firstClass = it.next();
		if (!it.hasNext()) {
			return firstClass.getName();
		}
		String[] result = firstClass.getPackage().getName().split("\\.");
		while (it.hasNext()) {
			final String[] packageName = it.next().getPackage().getName().split("\\.");
			final int min = Math.min(result.length, packageName.length);
			for (int i = 0; i < min; i++ ) {
				if (!result[i].equals(packageName[i])) {
					if (i == 0) {
						return "ALL";
					}
					result = Arrays.copyOf(result, i);
					break;
				}
			}
		}
		return JakeUtilsString.toString(Arrays.asList(result), ".");
	}

}
