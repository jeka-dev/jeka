package org.jake.java.test.junit;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.java.JakeClassFilter;
import org.jake.java.JakeClassLoader;
import org.jake.java.JakeClasspath;
import org.jake.java.JakeJavaProcess;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

public final class JakeUnit {

	public enum JunitReportDetail {
		NONE, BASIC, FULL;
	}

	private static final String JUNIT4_RUNNER_CLASS_NAME = "org.junit.runner.JUnitCore";

	private static final String JUNIT3_RUNNER_CLASS_NAME = "junit.textui.TestRunner";

	private static final String JUNIT4_TEST_ANNOTATION_CLASS_NAME = "org.junit.Test";

	private static final String JUNIT3_TEST_CASE_CLASS_NAME = "junit.framework.TestCase";

	private static final String JUNIT3_TEST_SUITE_CLASS_NAME = "junit.framework.TestSuite";

	private static final String JUNIT3_TEST_RESULT_CLASS_NAME = "junit.framework.TestResult";

	private final JakeClasspath classpath;

	private final JunitReportDetail reportDetail;

	private final File reportDir;

	private final JakeJavaProcess fork;

	private final List<Runnable> postActions;

	private JakeUnit(JakeClasspath classpath, JunitReportDetail reportDetail, File reportDir, JakeJavaProcess fork, List<Runnable> runnables) {
		this.classpath = classpath;
		this.reportDetail = reportDetail;
		this.reportDir = reportDir;
		this.fork = fork;
		this.postActions = Collections.unmodifiableList(runnables);
	}

	@SuppressWarnings("unchecked")
	private JakeUnit(JakeClasspath classpath, JunitReportDetail reportDetail, File reportDir, JakeJavaProcess fork) {
		this(classpath, reportDetail, reportDir, fork, Collections.EMPTY_LIST);
	}

	public static JakeUnit ofFork(JakeJavaProcess jakeJavaProcess) {
		return new JakeUnit(null, JunitReportDetail.NONE, null, jakeJavaProcess);
	}


	public static JakeUnit ofClasspath(File file, Iterable<File> files) {
		return of(JakeClasspath.of(file).and(files));
	}

	public static JakeUnit of(JakeClasspath classpath) {
		return new JakeUnit(classpath, JunitReportDetail.NONE, null, null);
	}

	public JakeUnit withReport(JunitReportDetail reportDetail, File reportDir) {
		return new JakeUnit(this.classpath, reportDetail, reportDir, this.fork);
	}

	public JakeUnit forkKeepingSameClassPath(JakeJavaProcess process) {
		final JakeJavaProcess fork = process.withClasspath(jakeClasspath());
		return new JakeUnit(null, reportDetail, reportDir, fork);
	}

	public JakeUnit withPostAction(Runnable runnable) {
		final List<Runnable> list = new LinkedList<Runnable>(this.postActions);
		list.add(runnable);
		return new JakeUnit(classpath, reportDetail, reportDir, fork, list);
	}

	public JakeUnit fork(JakeJavaProcess process) {
		return new JakeUnit(null, reportDetail, reportDir, process);
	}


	public boolean isForked() {
		return this.fork != null;
	}

	public JakeClasspath getClasspath() {
		return classpath;
	}

	public JunitReportDetail getReportDetail() {
		return reportDetail;
	}

	public File getReportDir() {
		return reportDir;
	}

	public JakeJavaProcess getFork() {
		return fork;
	}

	public JakeTestSuiteResult launchAll(File... testClassDirs) {
		return launchAll(Arrays.asList(testClassDirs),
				JakeClassFilter.acceptAll());
	}

	@SuppressWarnings({ "rawtypes" })
	public JakeTestSuiteResult launchAll(final Iterable<File> testClassDirs, JakeClassFilter classFilter) {
		final Collection<Class> testClasses = getClassesToTest(testClassDirs, classFilter);
		return launch(testClasses);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JakeTestSuiteResult launch(Iterable<Class> classes) {
		final String name = getSuiteName(classes);

		if (!classes.iterator().hasNext()) {
			return JakeTestSuiteResult.empty((Properties) System.getProperties().clone(), name, 0);
		}
		final long start = System.nanoTime();
		final JakeClassLoader classLoader = JakeClassLoader.of(classes.iterator().next());
		JakeLog.startAndNextLine("Run JUnit tests");

		final boolean verbose = JakeOptions.isVerbose();

		final JakeTestSuiteResult result;

		if (classLoader.isDefined(JUNIT4_RUNNER_CLASS_NAME)) {
			if (this.fork != null) {
				result = JUnit4TestLauncher.launchInFork(fork, verbose, reportDetail, classes, reportDir);
			} else {
				result = JUnit4TestLauncher.launchInClassLoader(classes,  verbose, reportDetail, reportDir);
			}
		} else if (classLoader.isDefined(JUNIT3_RUNNER_CLASS_NAME)) {
			final Object suite = createJunit3TestSuite(classLoader, classes);
			final Class testResultClass = classLoader.load(JUNIT3_TEST_RESULT_CLASS_NAME);
			final Object testResult = JakeUtilsReflect.newInstance(testResultClass);
			final Method runMethod = JakeUtilsReflect.getMethod(suite.getClass(),
					"run", testResultClass);
			final Properties properties = (Properties) System.getProperties().clone();
			JakeUtilsReflect.invoke(suite, runMethod, testResult);
			final long end = System.nanoTime();
			final long duration = (end - start) / 1000000;
			result = fromJunit3Result(properties, name, testResult, duration);
		} else {
			throw new IllegalStateException("No Junit found on test classpath.");
		}

		JakeLog.info(result.toStrings());
		if (!JakeOptions.isVerbose() && result.failureCount() > 0) {
			JakeLog.info("Launch Jake in verbose mode to display failure stack traces in console.");
		}
		if (reportDetail.equals(JunitReportDetail.BASIC)) {
			JakeTestReportBuilder.of(result).writeToFileSystem(reportDir);
		}
		for (final Runnable runnable : this.postActions) {
			runnable.run();
		}
		JakeLog.done("Tests run");
		return result;
	}

	private JakeClasspath jakeClasspath() {
		if (classpath != null) {
			return classpath;
		}
		return fork.classpath();
	}



	@SuppressWarnings("rawtypes")
	private Collection<Class> getClassesToTest(final Iterable<File> testClassDirs, JakeClassFilter classFilter) {
		final Collection<Class> testClasses;
		final JakeClasspath classpath = this.jakeClasspath().andAtHead(testClassDirs);
		final JakeClassLoader classLoader = JakeClassLoader.system().parent().createChild(classpath);
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
		return effectiveClasses;
	}



	@SuppressWarnings("rawtypes")
	private static Collection<Class> getJunitTestClassesInClassLoader(
			JakeClassLoader classloader, FileFilter entryFilter) {
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



	private static boolean isJunit3Test(Class<?> candidtateClazz,
			Class<?> testCaseClass) {
		if (Modifier.isAbstract(candidtateClazz.getModifiers())) {
			return false;
		}
		return testCaseClass.isAssignableFrom(candidtateClazz);
	}

	private static boolean isJunit4Test(Class<?> candidateClass,
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


	@SuppressWarnings("rawtypes")
	private static Object createJunit3TestSuite(JakeClassLoader classLoader, Iterable<Class> testClasses) {
		final Class<?>[] classArray = JakeUtilsIterable.toArray(testClasses, Class.class);
		final Class<?> testSuiteClass = classLoader.load(JUNIT3_TEST_SUITE_CLASS_NAME);
		try {
			final Constructor constructor =  testSuiteClass.getConstructor(classArray.getClass());
			return constructor.newInstance((Object)classArray);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static JakeTestSuiteResult fromJunit3Result(Properties properties, String suiteName, Object result, long durationInMillis) {
		final Integer runCount = JakeUtilsReflect.invoke(result, "runCount");
		final Integer ignoreCount = 0;
		final Enumeration<Object> junitFailures = JakeUtilsReflect.invoke(result,
				"failures");
		final Enumeration<Object> junitErrors = JakeUtilsReflect.invoke(result,
				"errors");
		final List<JakeTestSuiteResult.Failure> failures = new ArrayList<JakeTestSuiteResult.Failure>();
		while(junitFailures.hasMoreElements()) {
			final Object junitFailure = junitFailures.nextElement();
			failures.add(JakeTestSuiteResult.fromJunit3Failure(junitFailure));
		}
		while(junitErrors.hasMoreElements()) {
			final Object junitError = junitErrors.nextElement();
			failures.add(JakeTestSuiteResult.fromJunit3Failure(junitError));
		}
		return new JakeTestSuiteResult(properties, suiteName, runCount, ignoreCount, failures, durationInMillis);

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
