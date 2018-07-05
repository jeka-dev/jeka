package org.jerkar.api.java.junit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * A result of a test suite execution. It contains both overall information about suite execution
 * and details about each test case execution.
 */
public class JkTestSuiteResult implements Serializable {

    private static final long serialVersionUID = -5353195584286473050L;

    private final String suiteName;
    private final List<? extends TestCaseResult> testCaseResults;
    private final int runCount;
    private final int ignoreCount;
    private final long durationInMilis;
    private final Properties systemProperties;

    /**
     * Constructs a test suite execution result according specified information.
     */
    JkTestSuiteResult(Properties properties, String suiteName, int totaltestCount,
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
    static JkTestSuiteResult empty(Properties properties, String name, long durationInMillis) {
        return new JkTestSuiteResult(properties, name, 0, 0, Collections.EMPTY_LIST,
                durationInMillis);
    }

    List<? extends TestCaseResult> testCaseResults() {
        return testCaseResults;
    }

    List<TestCaseFailure> failures() {
        final List<TestCaseFailure> result = new LinkedList<>();
        for (final TestCaseResult caseResult : this.testCaseResults) {
            if (caseResult instanceof TestCaseFailure) {
                result.add((TestCaseFailure) caseResult);
            }
        }
        return result;
    }

    /**
     * Returns how many test has been run.
     */
    public int runCount() {
        return runCount;
    }

    /**
     * Returns how many test has been ignored.
     */
    public int ignoreCount() {
        return ignoreCount;
    }

    /**
     * Returns how many test has failed.
     */
    public int failureCount() {
        return failures().size();
    }

    /**
     * Returns the suite name.
     */
    public String suiteName() {
        return suiteName;
    }

    /**
     * Returns assertion failed count.
     */
    public int assertErrorCount() {
        int result = 0;
        for (final TestCaseFailure failure : failures()) {
            if (failure.getExceptionDescription().isAssertError()) {
                result++;
            }
        }
        return result;
    }

    /**
     * Returns error count (without counting assertion failures).
     */
    public int errorCount() {
        return failureCount() - assertErrorCount();
    }

    /**
     * Returns duration of the suite execution.
     */
    public long durationInMillis() {
        return durationInMilis;
    }

    /**
     * Returns a multi line string representation of the suite execution result;
     */
    public List<String> toStrings(boolean showStackTrace) {
        final List<String> lines = new LinkedList<>();
        lines.add(toString());
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

    /**
     * A result for a single test case execution in case of success.
     */
    @SuppressWarnings("serial")
    public static class TestCaseResult implements Serializable {
        private final String className;
        private final String testName;
        private final float durationInSecond;

        /**
         * Constructs a test case result.
         */
        public TestCaseResult(String className, String testName, float durationSec) {
            super();
            this.className = className;
            this.testName = testName;
            this.durationInSecond = durationSec;
        }

        /**
         * Returns the class name which the test belong to.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Returns the test name (generally the method value);
         */
        public String getTestName() {
            return testName;
        }

        /**
         * Returns duration of the suite execution.
         */
        public float getDurationInSecond() {
            return durationInSecond;
        }

    }

    /**
     * A result for a single test case execution in case of failure.
     */
    public static class TestCaseFailure extends TestCaseResult implements Serializable {

        private static final long serialVersionUID = 7089021299483181605L;

        private final ExceptionDescription exceptionDescription;

        /**
         * Constructs a test case result.
         */
        public TestCaseFailure(String className, String testName, float duration,
                ExceptionDescription exception) {
            super(className, testName, duration);
            this.exceptionDescription = exception;
        }

        /**
         * Returns the description of the failure.
         */
        public ExceptionDescription getExceptionDescription() {
            return exceptionDescription;
        }

        /**
         * Returns a multi line string representation of the test case execution result;
         */
        public List<String> toStrings(boolean withStackTrace) {
            final List<String> result = new LinkedList<>();
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

    /**
     * A result for a single test case execution in case of ignore.
     */
    public static class IgnoredCase extends TestCaseResult implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs an ignored test case result.
         */
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

    /**
     * Returns the system properties in use during the test suite execution.
     */
    public Properties getSystemProperties() {
        return systemProperties;
    }

    /**
     * Description of an execption occured during the test suite execution.
     */
    public static class ExceptionDescription implements Serializable {

        private static final long serialVersionUID = -8619868712236132763L;

        private final String className;
        private final String message;
        private final StackTraceElement[] stackTrace;
        private final ExceptionDescription cause;
        private final boolean assertError;

        /**
         * Constructs an exception description.
         */
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

        /**
         * Returns the name of the exception.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Returns the message of the exception.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the stack trace of the exception.
         */
        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        /**
         * Returns the cause of the exception.
         */
        public ExceptionDescription getCause() {
            return cause;
        }

        /**
         * Returns either this exception is an assertion failure or not.
         */
        public boolean isAssertError() {
            return assertError;
        }

        /**
         * Returns a multi-line representation of the stack trace.
         */
        public List<String> stackTracesAsStrings() {
            return stackTracesAsStrings(className + ": " + message);
        }

        private List<String> stackTracesAsStrings(String prefix) {
            final List<String> result = new LinkedList<>();
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
        final List<JkTestSuiteResult.TestCaseFailure> failures = new ArrayList<>(
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
