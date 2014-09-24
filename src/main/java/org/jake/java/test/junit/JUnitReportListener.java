package org.jake.java.test.junit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jake.java.test.junit.JakeTestSuiteResult.ExceptionDescription;
import org.jake.java.test.junit.JakeTestSuiteResult.IgnoredCase;
import org.jake.java.test.junit.JakeTestSuiteResult.TestCaseFailure;
import org.jake.java.test.junit.JakeTestSuiteResult.TestCaseResult;
import org.jake.utils.JakeUtilsTime;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitReportListener extends RunListener {

	private List<JakeTestSuiteResult.TestCaseResult> cases;

	private Properties properties;

	private Class<?> currentClass;

	private long suiteTimeNano;

	private long testTimeNano;

	private int ignoreCount;

	private final File folder;

	private String currentTestName;

	private boolean failureFlag;

	public JUnitReportListener(File folder) {
		super();
		this.folder = folder;
	}

	@Override
	public void testStarted(Description description) throws Exception {
		final Class<?> clazz = description.getTestClass();
		if (!clazz.equals(currentClass)) {
			dump();
			init(clazz);
		}
		testTimeNano = System.nanoTime();
		currentTestName = description.getMethodName();
		failureFlag = false;
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		ignoreCount++;
		final IgnoredCase ignoredCase = new IgnoredCase(currentClass.getName(), currentTestName);
		this.cases.add(ignoredCase);
	}

	@Override
	public void testFinished(Description description) throws Exception {
		final float duration = (JakeUtilsTime.durationInMillis(testTimeNano))/1000f;
		if (!failureFlag) {
			final TestCaseResult result = new TestCaseResult(currentClass.getName(), currentTestName, duration);
			cases.add(result);
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		failureFlag = true;
		final float duration = (JakeUtilsTime.durationInMillis(testTimeNano))/1000f;
		final TestCaseFailure caseFailure = new TestCaseFailure(currentClass.getName(), currentTestName,
				duration, new ExceptionDescription(	failure.getException()));
		cases.add(caseFailure);
	}





	@Override
	public void testRunFinished(Result result) throws Exception {
		dump();
	}

	private void init(Class<?> clazz) {
		this.currentClass = clazz;
		this.properties = new Properties();
		this.properties.putAll(System.getProperties());
		this.suiteTimeNano = System.nanoTime();
		this.cases = new LinkedList<JakeTestSuiteResult.TestCaseResult>();
		this.ignoreCount = 0;
	}

	private void dump() {
		if (currentClass == null) {
			return;
		}
		final long duration = JakeUtilsTime.durationInMillis(suiteTimeNano);
		final String suiteName = currentClass.getName();
		final int count = cases.size();
		final JakeTestSuiteResult result = new JakeTestSuiteResult(properties, suiteName, count, ignoreCount, cases, duration);
		JakeTestReportBuilder.of(result).writeToFileSystem(folder);
	}

}
