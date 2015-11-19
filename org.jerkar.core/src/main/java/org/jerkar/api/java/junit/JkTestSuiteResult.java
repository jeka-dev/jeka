package org.jerkar.api.java.junit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsReflect;

public class JkTestSuiteResult implements Serializable {

    private static final long serialVersionUID = -5353195584286473050L;

    private final String suiteName;
    private final List<? extends TestCaseResult> testCaseResults;
    private final int runCount;
    private final int ignoreCount;
    private final long durationInMilis;
    private final Properties systemProperties;

    public JkTestSuiteResult(Properties properties, String suiteName, int totaltestCount,
            int ignoreCount, Iterable<? extends TestCaseResult> testCaseResult,
            long durationInMillis) {
        this.systemProperties = properties;
        this.suiteName = suiteName;
        this.runCount = totaltestCount;
        this.ignoreCount = ignoreCount;
        this.testCaseResults = JkUtilsIterable.listOf(testCaseResult);
        this.durationInMilis = durationInMillis;
    }

    @SuppressWarnings("unchecked")
    public static JkTestSuiteResult empty(Properties properties, String name, long durationInMillis) {
        return new JkTestSuiteResult(properties, name, 0, 0, Collections.EMPTY_LIST,
                durationInMillis);
    }

    public List<? extends TestCaseResult> testCaseResults() {
        return testCaseResults;
    }

    public List<TestCaseFailure> failures() {
        final List<TestCaseFailure> result = new LinkedList<JkTestSuiteResult.TestCaseFailure>();
        for (final TestCaseResult caseResult : this.testCaseResults) {
            if (caseResult instanceof TestCaseFailure) {
                result.add((TestCaseFailure) caseResult);
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
        return failures().size();
    }

    public String suiteName() {
        return suiteName;
    }

    public int assertErrorCount() {
        int result = 0;
        for (final TestCaseFailure failure : failures()) {
            if (failure.getExceptionDescription().isAssertError()) {
                result++;
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

    public List<String> toStrings(boolean showStackTrace) {
        final List<String> lines = new LinkedList<String>();
        if (failureCount() == 0) {
            lines.add(toString());
        } else {
            lines.add(toString());
        }
        int i = 0;
        for (final TestCaseResult testCaseResult : this.testCaseResults) {
            if (testCaseResult instanceof TestCaseFailure) {
                final TestCaseFailure failure = (TestCaseFailure) testCaseResult;
                lines.add("-> " + failure.getClassName() + "." + failure.getTestName() + " : "
                        + failure.getExceptionDescription().message);
                if (showStackTrace || i < 3) {
                    lines.addAll(failure.exceptionDescription.stackTracesAsStrings());
                    lines.add("");
                }
                i++;
            }

        }
        return lines;
    }

    @Override
    public String toString() {
        return "" + runCount + " test(s) run, " + failureCount() + " failure(s), " + ignoreCount
                + " ignored. In " + durationInMilis + " milliseconds.";
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

    public static class TestCaseFailure extends TestCaseResult implements Serializable {

        private static final long serialVersionUID = 7089021299483181605L;

        private final ExceptionDescription exceptionDescription;

        public TestCaseFailure(String className, String testName, float duration,
                ExceptionDescription exception) {
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
                result.addAll(stack.subList(1, stack.size() - 1));
            } else {
                result.add(intro);
            }
            return result;
        }

    }

    public static class IgnoredCase extends TestCaseResult implements Serializable {

        private static final long serialVersionUID = 1L;

        public IgnoredCase(String className, String testName) {
            super(className, testName, 0);
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

    static JkTestSuiteResult fromJunit4Result(Properties properties, String suiteName,
            Object result, long durationInMillis) {
        final Integer runCount = JkUtilsReflect.invoke(result, "getRunCount");
        final Integer ignoreCount = JkUtilsReflect.invoke(result, "getIgnoreCount");
        final List<Object> junitFailures = JkUtilsReflect.invoke(result, "getFailures");
        final List<JkTestSuiteResult.TestCaseFailure> failures = new ArrayList<JkTestSuiteResult.TestCaseFailure>(
                junitFailures.size());
        for (final Object junitFailure : junitFailures) {
            failures.add(fromJunit4Failure(junitFailure));
        }
        return new JkTestSuiteResult(properties, suiteName, runCount, ignoreCount, failures,
                durationInMillis);

    }

    private static JkTestSuiteResult.TestCaseFailure fromJunit4Failure(Object junit4failure) {
        final Object junit4Description = JkUtilsReflect.invoke(junit4failure, "getDescription");
        final String testClassName = JkUtilsReflect.invoke(junit4Description, "getClassName");
        final String testMethodName = JkUtilsReflect.invoke(junit4Description, "getMethodName");
        final Throwable exception = JkUtilsReflect.invoke(junit4failure, "getException");
        final ExceptionDescription description = new ExceptionDescription(exception);
        return new JkTestSuiteResult.TestCaseFailure(testClassName, testMethodName, -1, description);
    }

    static JkTestSuiteResult.TestCaseFailure fromJunit3Failure(Object junit3failure) {
        final Object failedTest = JkUtilsReflect.invoke(junit3failure, "failedTest");
        final Throwable exception = JkUtilsReflect.invoke(junit3failure, "thrownException");
        final ExceptionDescription description = new ExceptionDescription(exception);
        final String failedTestName = failedTest.toString();
        final int firstParenthesisIndex = failedTestName.indexOf("(");
        final String methodName = failedTestName.substring(0, firstParenthesisIndex);
        return new JkTestSuiteResult.TestCaseFailure(failedTest.getClass().getName(), methodName,
                -1, description);
    }

}
