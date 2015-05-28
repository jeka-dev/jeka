package org.jerkar.builtins.javabuild.testing.junit;

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

import org.jerkar.JkClassLoader;
import org.jerkar.JkClasspath;
import org.jerkar.JkException;
import org.jerkar.JkJavaProcess;
import org.jerkar.JkLog;
import org.jerkar.JkOptions;
import org.jerkar.file.JkFileTree;
import org.jerkar.file.JkFileTreeSet;
import org.jerkar.utils.JkUtilsIterable;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;

/**
 * Convenient class to launch Junit tests.
 * 
 * @author Jerome Angibaud
 */
public final class JkUnit {

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
		JkUnit enhance(JkUnit jkUnit);
	}

	private final JkClasspath classpath;

	private final JunitReportDetail reportDetail;

	private final File reportDir;

	private final JkJavaProcess forkedProcess;

	private final List<Runnable> postActions;

	private final JkFileTreeSet classesToTest;

	private final boolean breakOnFailure;

	private JkUnit(JkClasspath classpath, JunitReportDetail reportDetail, File reportDir, JkJavaProcess fork, List<Runnable> runnables, JkFileTreeSet testClasses, boolean crashOnFailed) {
		this.classpath = classpath;
		this.reportDetail = reportDetail;
		this.reportDir = reportDir;
		this.forkedProcess = fork;
		this.postActions = Collections.unmodifiableList(runnables);
		this.classesToTest = testClasses;
		this.breakOnFailure = crashOnFailed;
	}

	@SuppressWarnings("unchecked")
	private JkUnit(JkClasspath classpath, JunitReportDetail reportDetail, File reportDir, JkJavaProcess fork, JkFileTreeSet testClasses, boolean crashOnFailed) {
		this(classpath, reportDetail, reportDir, fork, Collections.EMPTY_LIST, testClasses, crashOnFailed);
	}

	public static JkUnit ofFork(JkJavaProcess jkJavaProcess) {
		return new JkUnit(null, JunitReportDetail.NONE, null, jkJavaProcess, JkFileTreeSet.empty(), true);
	}

	public static JkUnit ofFork(JkClasspath classpath) {
		return ofFork(JkJavaProcess.of().withClasspath(classpath));
	}


	public static JkUnit ofClasspath(File binDir, Iterable<File> classpathEntries) {
		return of(JkClasspath.of(binDir).and(classpathEntries));
	}

	public static JkUnit of(JkClasspath classpath) {
		return new JkUnit(classpath, JunitReportDetail.NONE, null, null, JkFileTreeSet.empty(), true);
	}

	public JkUnit withReport(JunitReportDetail reportDetail) {
		return new JkUnit(this.classpath, reportDetail, reportDir, this.forkedProcess, classesToTest, this.breakOnFailure);
	}

	public JkUnit withBreakOnFailure(boolean crashOnFailure) {
		return new JkUnit(this.classpath, reportDetail, reportDir, this.forkedProcess, classesToTest, this.breakOnFailure);
	}

	public JkUnit withReportDir(File reportDir) {
		return new JkUnit(this.classpath, reportDetail, reportDir, this.forkedProcess, classesToTest, this.breakOnFailure);
	}

	public JkUnit forkKeepingSameClassPath(JkJavaProcess process) {
		final JkJavaProcess fork = process.withClasspath(jkClasspath());
		return new JkUnit(null, reportDetail, reportDir, fork, this.classesToTest, this.breakOnFailure);
	}

	public JkUnit withPostAction(Runnable runnable) {
		final List<Runnable> list = new LinkedList<Runnable>(this.postActions);
		list.add(runnable);
		return new JkUnit(classpath, reportDetail, reportDir, forkedProcess, list,this.classesToTest, this.breakOnFailure);
	}

	public JkUnit enhancedWith(Enhancer enhancer) {
		return enhancer.enhance(this);
	}

	public JkUnit enhancedWith(Iterable<Enhancer> plugins) {
		JkUnit result = this;
		for (final Enhancer plugin : plugins) {
			result = result.enhancedWith(plugin);
		}
		return result;
	}

	public JkUnit forked(JkJavaProcess process) {
		return new JkUnit(null, reportDetail, reportDir, process, this.classesToTest, this.breakOnFailure);
	}

	/**
	 * Creates an identical JkUnit to this one but specifying the forked mode. If the forked mode is <code>true<code> then the specified
	 * {@link JkJavaProcess} is used to run the tests..
	 */
	public JkUnit forked(boolean fork, JkJavaProcess process) {
		if (fork && !forked()) {
			return forked(process);
		}
		if (!fork && forked()) {
			return new JkUnit(forkedProcess.classpath(), reportDetail, reportDir, null, this.classesToTest, this.breakOnFailure);
		}
		return this;
	}

	/**
	 * Creates an identical JkUnit to this one but specifying the forked mode. If the forked mode is <code>true<code> then
	 * default {@link JkJavaProcess} is used to run the tests (java process launched without any option).
	 */
	public JkUnit forked(boolean fork) {
		return forked(fork, JkJavaProcess.of());
	}

	public JkUnit withClassesToTest(JkFileTreeSet classesToTest) {
		return new JkUnit(this.classpath, reportDetail, reportDir, forkedProcess, classesToTest, this.breakOnFailure);
	}

	public JkUnit withClassesToTest(JkFileTree classesToTest) {
		return new JkUnit(this.classpath, reportDetail, reportDir, forkedProcess, JkFileTreeSet.of(classesToTest), this.breakOnFailure);
	}


	public JkUnit withClassesToTest(File ...classDirs) {
		return new JkUnit(this.classpath, reportDetail, reportDir, forkedProcess, JkFileTreeSet.of(classDirs), this.breakOnFailure);
	}

	public boolean forked() {
		return this.forkedProcess != null;
	}

	public JkClasspath classpath() {
		return classpath;
	}

	public JunitReportDetail reportDetail() {
		return reportDetail;
	}

	public File reportDir() {
		return reportDir;
	}

	public JkJavaProcess processFork() {
		return forkedProcess;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JkTestSuiteResult run() {
		final Collection<Class> classes = getClassesToTest();
		final String name = getSuiteName(classes);

		if (!classes.iterator().hasNext()) {
			JkLog.warn("No test class found.");
			return JkTestSuiteResult.empty((Properties) System.getProperties().clone(), name, 0);
		}
		final long start = System.nanoTime();
		final JkClassLoader classLoader = JkClassLoader.of(classes.iterator().next());
		JkLog.startln("Run JUnit tests");

		final boolean verbose = JkOptions.isVerbose();

		final JkTestSuiteResult result;

		if (classLoader.isDefined(JUNIT4_RUNNER_CLASS_NAME)) {
			if (this.forkedProcess != null) {
				result = JUnit4TestLauncher.launchInFork(forkedProcess, verbose, reportDetail, classes, reportDir);
			} else {
				result = JUnit4TestLauncher.launchInClassLoader(classes,  verbose, reportDetail, reportDir);
			}
		} else if (classLoader.isDefined(JUNIT3_RUNNER_CLASS_NAME)) {
			final Object suite = createJunit3TestSuite(classLoader, classes);
			final Class testResultClass = classLoader.load(JUNIT3_TEST_RESULT_CLASS_NAME);
			final Object testResult = JkUtilsReflect.newInstance(testResultClass);
			final Method runMethod = JkUtilsReflect.getMethod(suite.getClass(),
					"run", testResultClass);
			final Properties properties = (Properties) System.getProperties().clone();
			JkUtilsReflect.invoke(suite, runMethod, testResult);
			final long end = System.nanoTime();
			final long duration = (end - start) / 1000000;
			result = fromJunit3Result(properties, name, testResult, duration);
		} else {
			throw new IllegalStateException("No Junit found on test classpath.");
		}

		if (result.failureCount() > 0) {
			if (breakOnFailure) {
				JkLog.error(result.toStrings(verbose));
				throw new JkException("Test failed : " + result.toString() );
			} else {
				JkLog.warn(result.toStrings(verbose));
			}
		} else {
			JkLog.info(result.toStrings(verbose));
		}
		if (!JkOptions.isVerbose() && result.failureCount() > 0) {
			JkLog.info("Launch Jerkar in verbose mode to display failure stack traces in console.");
		}
		if (reportDetail.equals(JunitReportDetail.BASIC)) {
			TestReportBuilder.of(result).writeToFileSystem(reportDir);
		}
		for (final Runnable runnable : this.postActions) {
			runnable.run();  // NOSONAR
		}
		JkLog.done("Tests run");
		return result;
	}

	private JkClasspath jkClasspath() {
		if (classpath != null) {
			return classpath;
		}
		return forkedProcess.classpath();
	}

	@SuppressWarnings("rawtypes")
	private Collection<Class> getClassesToTest() {
		final JkClasspath classpath = this.jkClasspath().andHead(this.classesToTest.roots());
		final JkClassLoader classLoader = JkClassLoader.system().parent().child(classpath).loadAllServices();
		return getJunitTestClassesInClassLoader(classLoader, this.classesToTest);
	}

	@SuppressWarnings("rawtypes")
	private static Collection<Class> getJunitTestClassesInClassLoader(
			JkClassLoader classloader, JkFileTreeSet jkFileTreeSet) {
		final Iterable<Class<?>> classes = classloader.loadClassesIn(jkFileTreeSet);
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
	private static Object createJunit3TestSuite(JkClassLoader classLoader, Iterable<Class> testClasses) {
		final Class<?>[] classArray = JkUtilsIterable.arrayOf(testClasses, Class.class);
		final Class<?> testSuiteClass = classLoader.load(JUNIT3_TEST_SUITE_CLASS_NAME);
		try {
			final Constructor constructor =  testSuiteClass.getConstructor(classArray.getClass());
			return constructor.newInstance((Object)classArray);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static JkTestSuiteResult fromJunit3Result(Properties properties, String suiteName, Object result, long durationInMillis) {
		final Integer runCount = JkUtilsReflect.invoke(result, "runCount");
		final Integer ignoreCount = 0;
		final Enumeration<Object> junitFailures = JkUtilsReflect.invoke(result,
				"failures");
		final Enumeration<Object> junitErrors = JkUtilsReflect.invoke(result,
				"errors");
		final List<JkTestSuiteResult.TestCaseFailure> failures = new ArrayList<JkTestSuiteResult.TestCaseFailure>();
		while(junitFailures.hasMoreElements()) {
			final Object junitFailure = junitFailures.nextElement();
			failures.add(JkTestSuiteResult.fromJunit3Failure(junitFailure));
		}
		while(junitErrors.hasMoreElements()) {
			final Object junitError = junitErrors.nextElement();
			failures.add(JkTestSuiteResult.fromJunit3Failure(junitError));
		}
		return new JkTestSuiteResult(properties, suiteName, runCount, ignoreCount, failures, durationInMillis);

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
		return JkUtilsString.join(Arrays.asList(result), ".");
	}

}
