package org.jake.java;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLogger;
import org.jake.JakeOptions;
import org.jake.utils.JakeUtilsIterable;

public class JakeTestResult {

	private final List<Failure> failures;
	private final int runCount;
	private final int ignoreCount;
	private final long durationInMilis;


	public JakeTestResult(int totaltestCount, int ignoreCount, Iterable<Failure> failures, long durationInMillis) {
		this.runCount = totaltestCount;
		this.ignoreCount = ignoreCount;
		this.failures = JakeUtilsIterable.toList(failures);
		this.durationInMilis = durationInMillis;
	}

	@SuppressWarnings("unchecked")
	public static JakeTestResult empty(long durationInMillis) {
		return new JakeTestResult(0,0, Collections.EMPTY_LIST, durationInMillis);
	}

	public List<Failure> failures() {
		return failures;
	}

	public int runCount() {
		return runCount;
	}

	public int ignoreCount() {
		return ignoreCount;
	}

	public int failureCount() {
		return failures.size();
	}

	public long durationInMillis() {
		return durationInMilis;
	}

	public void printToNotifier() {
		JakeLogger.nextLine();
		if (failureCount() == 0) {
			JakeLogger.info(toString());
		} else {
			JakeLogger.info(toString());
		}
		if (JakeOptions.isVerbose()) {
			JakeLogger.nextLine();
		}
		for (final Failure failure : failures) {
			for (final String string : failure.toStrings(JakeOptions.isVerbose())) {
				JakeLogger.info(string);
			}
			if (JakeOptions.isVerbose()) {
				JakeLogger.nextLine();
			}

		}
		if (!JakeOptions.isVerbose() && !failures.isEmpty()) {
			JakeLogger.info("Launch Jake in verbose mode to display failure stack traces.");
		}
	}

	@Override
	public String toString() {
		return "" + runCount + " test(s) run, " + failureCount() + " failure(s), " + ignoreCount + " ignored. In " + durationInMilis + " milliseconds." ;
	}

	public static class Failure {
		private final String className;
		private final String testName;
		private final ExceptionDescription exceptionDescription;

		public Failure(String className, String testName, ExceptionDescription exception) {
			super();
			this.className = className;
			this.testName = testName;
			this.exceptionDescription = exception;
		}

		public String getClassName() {
			return className;
		}

		public String getTestName() {
			return testName;
		}

		public ExceptionDescription getExceptionDescription() {
			return exceptionDescription;
		}


		public List<String> toStrings(boolean withStackTrace) {
			final List<String> result = new LinkedList<String>();
			result.add(className + "#" + testName + " > " + exceptionDescription.getClassName()
					+ messageOrEmpty(exceptionDescription.getMessage()));
			if (withStackTrace) {
				result.addAll(exceptionDescription.stackTracesAsStrings());
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

	public static class ExceptionDescription {
		private final String className;
		private final String message;
		private final StackTraceElement[] stackTrace;
		private final ExceptionDescription cause;

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


		public List<String> stackTracesAsStrings() {
			return stackTracesAsStrings(null);
		}

		private List<String> stackTracesAsStrings(String prefix) {
			final List<String> result = new LinkedList<String>();
			if (prefix != null) {
				result.add(prefix);
			}
			for (final StackTraceElement element : stackTrace) {
				result.add("  " + element);
			}
			if (cause != null) {
				result.addAll(cause.stackTracesAsStrings("Caused by : " + cause.getClassName()
						+ messageOrEmpty(cause.getMessage())));
			}
			return result;
		}


	}



}
