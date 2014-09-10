package org.jake.java.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

public class JakeTestSuiteResult implements Serializable {

	private static final long serialVersionUID = -5353195584286473050L;

	private final String suiteName;
	private final List<? extends TestCaseResult> testCaseResults;
	private final int runCount;
	private final int ignoreCount;
	private final long durationInMilis;
	private final Properties systemProperties;

	public JakeTestSuiteResult(Properties properties, String suiteName, int totaltestCount, int ignoreCount, Iterable<? extends TestCaseResult> testCaseResult, long durationInMillis) {
		this.systemProperties = properties;
		this.suiteName = suiteName;
		this.runCount = totaltestCount;
		this.ignoreCount = ignoreCount;
		this.testCaseResults = JakeUtilsIterable.toList(testCaseResult);
		this.durationInMilis = durationInMillis;
	}

	@SuppressWarnings("unchecked")
	public static JakeTestSuiteResult empty(Properties properties, String name, long durationInMillis) {
		return new JakeTestSuiteResult(properties, name, 0,0, Collections.EMPTY_LIST, durationInMillis);
	}

	public List<? extends TestCaseResult> testCaseResults() {
		return testCaseResults;
	}

	public List<Failure> failures() {
		final List<Failure> result = new LinkedList<JakeTestSuiteResult.Failure>();
		for (final TestCaseResult caseResult : this.testCaseResults) {
			if (caseResult instanceof Failure) {
				result.add((Failure)caseResult);
			}
		}
		return result;
	}

	public int runCount() {
		return runCount;
	}

	public int ignoreCount() {
		return ignoreCount;
	}

	public int failureCount() {
		return testCaseResults.size();
	}

	public String suiteName() {
		return suiteName;
	}

	public int assertErrorCount() {
		int result = 0;
		for (final Failure failure : failures()) {
			if (failure.getExceptionDescription().isAssertError()) {
				result ++;
			}
		}
		return result;
	}

	public int errorCount() {
		return failureCount() - assertErrorCount();
	}


	public long durationInMillis() {
		return durationInMilis;
	}

	public List<String> toStrings(boolean verbose) {
		final List<String> lines = new LinkedList<String>();
		if (failureCount() == 0) {
			lines.add(toString());
		} else {
			lines.add(toString());
		}
		if (verbose) {
			lines.add("");
		}
		for (final Failure failure : failures()) {
			for (final String string : failure.toStrings(verbose)) {
				lines.add(string);
			}
			if (verbose) {
				lines.add("");
			}

		}
		return lines;
	}

	@Override
	public String toString() {
		return "" + runCount + " test(s) run, " + failureCount() + " failure(s), " + ignoreCount + " ignored. In " + durationInMilis + " milliseconds." ;
	}

	@SuppressWarnings("serial")
	public static class TestCaseResult implements Serializable {
		private final String className;
		private final String testName;
		private final float durationInSecond;

		public TestCaseResult(String className, String testName, float durationSec) {
			super();
			this.className = className;
			this.testName = testName;
			this.durationInSecond = durationSec;
		}

		public String getClassName() {
			return className;
		}

		public String getTestName() {
			return testName;
		}

		public float getDurationInSecond() {
			return durationInSecond;
		}

	}

	public static class Failure extends TestCaseResult implements Serializable {

		private static final long serialVersionUID = 7089021299483181605L;


		private final ExceptionDescription exceptionDescription;


		public Failure(String className, String testName, float duration, ExceptionDescription exception) {
			super(className, testName, duration);
			this.exceptionDescription = exception;
		}



		public ExceptionDescription getExceptionDescription() {
			return exceptionDescription;
		}


		public List<String> toStrings(boolean withStackTrace) {
			final List<String> result = new LinkedList<String>();
			final String intro = this.getClassName() + "#" + this.getTestName();

			if (withStackTrace) {
				final List<String> stack = exceptionDescription.stackTracesAsStrings();
				result.add(intro + " > " + stack.get(0));
				result.addAll(stack.subList(1, stack.size()-1));
			} else {
				result.add(intro);
			}
			return result;
		}

	}

	private static String messageOrEmpty(String exceptionMessage) {
		if (exceptionMessage == null) {
			return "";
		}
		return " : " + exceptionMessage;
	}

	public Properties getSystemProperties() {
		return systemProperties;
	}

	public static class ExceptionDescription implements Serializable {

		private static final long serialVersionUID = -8619868712236132763L;

		private final String className;
		private final String message;
		private final StackTraceElement[] stackTrace;
		private final ExceptionDescription cause;
		private final boolean assertError;

		public ExceptionDescription(Throwable throwable) {
			super();
			this.className = throwable.getClass().getName();
			this.message = throwable.getMessage();
			this.stackTrace = throwable.getStackTrace();
			if (throwable.getCause() != null) {
				this.cause = new ExceptionDescription(throwable.getCause());
			} else {
				this.cause = null;
			}
			assertError = AssertionError.class.isAssignableFrom(throwable.getClass());

		}

		public String getClassName() {
			return className;
		}

		public String getMessage() {
			return message;
		}

		public StackTraceElement[] getStackTrace() {
			return stackTrace;
		}

		public ExceptionDescription getCause() {
			return cause;
		}

		public boolean isAssertError() {
			return assertError;
		}


		public List<String> stackTracesAsStrings() {
			return stackTracesAsStrings(className + ": " + message);
		}

		private List<String> stackTracesAsStrings(String prefix) {
			final List<String> result = new LinkedList<String>();
			if (prefix != null) {
				result.add(prefix);
			}
			for (final StackTraceElement element : stackTrace) {
				result.add("  at " + element);
			}
			if (cause != null) {
				result.addAll(cause.stackTracesAsStrings("Caused by : " + cause.getClassName()
						+ ": " + messageOrEmpty(cause.getMessage())));
			}
			return result;
		}

	}

	static JakeTestSuiteResult fromJunit4Result(Properties properties, String suiteName, Object result, long durationInMillis) {
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
		return new JakeTestSuiteResult(properties, suiteName, runCount, ignoreCount, failures, durationInMillis);

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
		return new JakeTestSuiteResult.Failure(testClassName, testMethodName, -1,
				description);
	}

	static JakeTestSuiteResult.Failure fromJunit3Failure(Object junit3failure) {
		final Object failedTest = JakeUtilsReflect.invoke(junit3failure,
				"failedTest");
		final Throwable exception = JakeUtilsReflect.invoke(junit3failure,
				"thrownException");
		final ExceptionDescription description = new ExceptionDescription(exception);
		final String failedTestName = failedTest.toString();
		final int firstParenthesisIndex = failedTestName.indexOf("(");
		final String methodName = failedTestName.substring(0, firstParenthesisIndex);
		return new JakeTestSuiteResult.Failure(failedTest.getClass().getName(), methodName,-1,
				description);
	}




}
