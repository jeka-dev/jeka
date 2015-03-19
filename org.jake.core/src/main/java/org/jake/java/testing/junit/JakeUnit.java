package org.jake.java.testing.junit;

import java.io.File;
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

import org.jake.JakeClassLoader;
import org.jake.JakeClasspath;
import org.jake.JakeDirSet;
import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.JakeOptions;
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

	public static interface Enhancer {
		JakeUnit enhance(JakeUnit jakeUnit);
	}

	private final JakeClasspath classpath;

	private final JunitReportDetail reportDetail;

	private final File reportDir;

	private final JakeJavaProcess fork;

	private final List<Runnable> postActions;

	private final JakeDirSet classesToTest;

	private final boolean breakOnFailure;

	private JakeUnit(JakeClasspath classpath, JunitReportDetail reportDetail, File reportDir, JakeJavaProcess fork, List<Runnable> runnables, JakeDirSet testClasses, boolean crashOnFailed) {
		this.classpath = classpath;
		this.reportDetail = reportDetail;
		this.reportDir = reportDir;
		this.fork = fork;
		this.postActions = Collections.unmodifiableList(runnables);
		this.classesToTest = testClasses;
		this.breakOnFailure = crashOnFailed;
	}

	@SuppressWarnings("unchecked")
	private JakeUnit(JakeClasspath classpath, JunitReportDetail reportDetail, File reportDir, JakeJavaProcess fork, JakeDirSet testClasses, boolean crashOnFailed) {
		this(classpath, reportDetail, reportDir, fork, Collections.EMPTY_LIST, testClasses, crashOnFailed);
	}

	public static JakeUnit ofFork(JakeJavaProcess jakeJavaProcess) {
		return new JakeUnit(null, JunitReportDetail.NONE, null, jakeJavaProcess, JakeDirSet.empty(), true);
	}

	public static JakeUnit ofClasspath(File binDir, Iterable<File> classpathEntries) {
		return of(JakeClasspath.of(binDir).and(classpathEntries));
	}

	public static JakeUnit of(JakeClasspath classpath) {
		return new JakeUnit(classpath, JunitReportDetail.NONE, null, null, JakeDirSet.empty(), true);
	}

	public JakeUnit withReport(JunitReportDetail reportDetail) {
		return new JakeUnit(this.classpath, reportDetail, reportDir, this.fork, classesToTest, this.breakOnFailure);
	}

	public JakeUnit withCrashOnFailure(boolean crashOnFailure) {
		return new JakeUnit(this.classpath, reportDetail, reportDir, this.fork, classesToTest, this.breakOnFailure);
	}

	public JakeUnit withReportDir(File reportDir) {
		return new JakeUnit(this.classpath, reportDetail, reportDir, this.fork, classesToTest, this.breakOnFailure);
	}

	public JakeUnit forkKeepingSameClassPath(JakeJavaProcess process) {
		final JakeJavaProcess fork = process.withClasspath(jakeClasspath());
		return new JakeUnit(null, reportDetail, reportDir, fork, this.classesToTest, this.breakOnFailure);
	}

	public JakeUnit withPostAction(Runnable runnable) {
		final List<Runnable> list = new LinkedList<Runnable>(this.postActions);
		list.add(runnable);
		return new JakeUnit(classpath, reportDetail, reportDir, fork, list,this.classesToTest, this.breakOnFailure);
	}

	public JakeUnit enhancedWith(Enhancer enhancer) {
		return enhancer.enhance(this);
	}

	public JakeUnit enhancedWith(Iterable<Enhancer> plugins) {
		JakeUnit result = this;
		for (final Enhancer plugin : plugins) {
			result = result.enhancedWith(plugin);
		}
		return result;
	}

	public JakeUnit fork(JakeJavaProcess process) {
		return new JakeUnit(null, reportDetail, reportDir, process, this.classesToTest, this.breakOnFailure);
	}

	public JakeUnit withClassesToTest(JakeDirSet classesToTest) {
		return new JakeUnit(this.classpath, reportDetail, reportDir, fork, classesToTest, this.breakOnFailure);
	}

	public JakeUnit withClassesToTest(File ...classDirs) {
		return new JakeUnit(this.classpath, reportDetail, reportDir, fork, JakeDirSet.of(classDirs), this.breakOnFailure);
	}

	public boolean forked() {
		return this.fork != null;
	}

	public JakeClasspath classpath() {
		return classpath;
	}

	public JunitReportDetail reportDetail() {
		return reportDetail;
	}

	public File reportDir() {
		return reportDir;
	}

	public JakeJavaProcess processFork() {
		return fork;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JakeTestSuiteResult run() {
		final Collection<Class> classes = getClassesToTest();
		final String name = getSuiteName(classes);

		if (!classes.iterator().hasNext()) {
			JakeLog.warn("No test class found.");
			return JakeTestSuiteResult.empty((Properties) System.getProperties().clone(), name, 0);
		}
		final long start = System.nanoTime();
		final JakeClassLoader classLoader = JakeClassLoader.of(classes.iterator().next());
		JakeLog.startln("Run JUnit tests");

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

		if (result.failureCount() > 0) {
			if (breakOnFailure) {
				JakeLog.error(result.toStrings());
				throw new JakeException("Test failed : " + result.toString() );
			} else {
				JakeLog.warn(result.toStrings());
			}
		} else {
			JakeLog.info(result.toStrings());
		}
		if (!JakeOptions.isVerbose() && result.failureCount() > 0) {
			JakeLog.info("Launch Jake in verbose mode to display failure stack traces in console.");
		}
		if (reportDetail.equals(JunitReportDetail.BASIC)) {
			TestReportBuilder.of(result).writeToFileSystem(reportDir);
		}
		for (final Runnable runnable : this.postActions) {
			runnable.run();  // NOSONAR
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
	private Collection<Class> getClassesToTest() {
		final JakeClasspath classpath = this.jakeClasspath().andHead(this.classesToTest.roots());
		final JakeClassLoader classLoader = JakeClassLoader.system().parent().createChild(classpath);
		return getJunitTestClassesInClassLoader(classLoader, this.classesToTest);
	}

	@SuppressWarnings("rawtypes")
	private static Collection<Class> getJunitTestClassesInClassLoader(
			JakeClassLoader classloader, JakeDirSet jakeDirSet) {
		final Iterable<Class<?>> classes = classloader.loadClassesIn(jakeDirSet);
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
		final List<JakeTestSuiteResult.TestCaseFailure> failures = new ArrayList<JakeTestSuiteResult.TestCaseFailure>();
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
