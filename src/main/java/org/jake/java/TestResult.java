package org.jake.java;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jake.Notifier;
import org.jake.utils.IterableUtils;

public class TestResult {
	
	private final List<Failure> failures;
	private final int runCount;
	private final int ignoreCount;
	
	
	public TestResult(int totaltestCount, int ignoreCount, Iterable<Failure> failures) {
		this.runCount = totaltestCount;
		this.ignoreCount = ignoreCount;
		this.failures = IterableUtils.toList(failures);
	}
	
	@SuppressWarnings("unchecked")
	public static TestResult empty() {
		return new TestResult(0,0, Collections.EMPTY_LIST);
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
	
	public void printToNotifier() {
		Notifier.getInfoWriter().flush();
		if (failureCount() == 0) {
			Notifier.info(toString());
		} else {
			Notifier.warn(toString());
		}
		for (Failure failure : failures) {
			for (String string : failure.toStrings()) {
				Notifier.warn(string);
			}
		}
	}
	
	@Override
	public String toString() {
		return "" + runCount + " test(s) run, " + failureCount() + " failure(s), " + ignoreCount + " ignored." ;
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
		
		
		public List<String> toStrings() {
			List<String> result = new LinkedList<String>();
			result.add("");
			result.add(className + "#" + testName + " > " + exceptionDescription.getClassName() 
					+ messageOrEmpty(exceptionDescription.getMessage()));
			result.addAll(exceptionDescription.stackTracesAsStrings());
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
			for (int i = 0; i < stackTrace.length; i++) {
				result.add("  " + stackTrace[i]);
			}
			if (cause != null) {
				result.addAll(cause.stackTracesAsStrings("Caused by : " + cause.getClassName() 
						+ messageOrEmpty(cause.getMessage())));
			}
			return result;
		}
		
		
	}
	
	

}
