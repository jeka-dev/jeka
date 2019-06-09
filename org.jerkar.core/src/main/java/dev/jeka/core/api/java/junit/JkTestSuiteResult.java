package dev.jeka.core.api.java.junit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsReflect;

/**
 * A result of a test suite execution. It contains both overall information about suite execution
 * and details about each test case execution.
 */
public class JkTestSuiteResult implements Serializable {

    private static final long serialVersionUID = -5353195584286473050L;

    private final String suiteName;
    private final List<? extends JkTestCaseResult> testCaseResults;
    private final int runCount;
    private final int ignoreCount;
    private final long durationInMilis;
    private final Properties systemProperties;

    /**
     * Constructs a test suite execution result according specified information.
     */
    JkTestSuiteResult(Properties properties, String suiteName, int totaltestCount,
            int ignoreCount, Iterable<? extends JkTestCaseResult> testCaseResult,
            long durationInMillis) {
        this.systemProperties = properties;
        this.suiteName = suiteName;
        this.runCount = totaltestCount;
        this.ignoreCount = ignoreCount;
        this.testCaseResults = JkUtilsIterable.listOf(testCaseResult);
        this.durationInMilis = durationInMillis;
    }

    @SuppressWarnings("unchecked")
    static JkTestSuiteResult ofEmpty(Properties properties, String name, long durationInMillis) {
        return new JkTestSuiteResult(properties, name, 0, 0, Collections.EMPTY_LIST,
                durationInMillis);
    }

    List<? extends JkTestCaseResult> testCaseResults() {
        return testCaseResults;
    }

    List<JkTestCaseFailure> failures() {
        final List<JkTestCaseFailure> result = new LinkedList<>();
        for (final JkTestCaseResult caseResult : this.testCaseResults) {
            if (caseResult instanceof JkTestCaseFailure) {
                result.add((JkTestCaseFailure) caseResult);
            }
        }
        return result;
    }

    /**
     * Returns how many test has been run.
     */
    public int getRunCount() {
        return runCount;
    }

    /**
     * Returns how many test has been ignored.
     */
    public int getIgnoreCount() {
        return ignoreCount;
    }

    /**
     * Returns how many test has failed.
     */
    public int getFailureCount() {
        return failures().size();
    }

    /**
     * Returns the suite name.
     */
    public String getSuiteName() {
        return suiteName;
    }

    /**
     * Returns assertion failed count.
     */
    public int assertErrorCount() {
        int result = 0;
        for (final JkTestCaseFailure failure : failures()) {
            if (failure.getExceptionDescription().isAssertError()) {
                result++;
            }
        }
        return result;
    }

    /**
     * Returns error count (without counting assertion failures).
     */
    public int getErrorCount() {
        return getFailureCount() - assertErrorCount();
    }

    /**
     * Returns duration of the suite execution.
     */
    public long getDurationInMillis() {
        return durationInMilis;
    }

    /**
     * Returns a multi line string representation of the suite execution result;
     */
    public List<String> toStrings(boolean showStackTrace) {
        final List<String> lines = new LinkedList<>();
        lines.add(toString());
        int i = 0;
        for (final JkTestCaseResult testCaseResult : this.testCaseResults) {
            if (testCaseResult instanceof JkTestCaseFailure) {
                final JkTestCaseFailure failure = (JkTestCaseFailure) testCaseResult;
                lines.add("-> " + failure.getClassName() + "." + failure.getTestName() + " : "
                        + failure.getExceptionDescription().message);
                if (showStackTrace || i < 3) {
                    lines.addAll(failure.jkExceptionDescription.getStackTraceAsStrings());
                    lines.add("");
                }
                i++;
            }
        }
        return lines;
    }

    @Override
    public String toString() {
        return "" + runCount + " test(s) run, " + getFailureCount() + " failure(s), " + ignoreCount
                + " ignored. In " + durationInMilis + " milliseconds.";
    }

    /**
     * A result for a single test case execution in case of success.
     */
    @SuppressWarnings("serial")
    public static class JkTestCaseResult implements Serializable {
        private final String className;
        private final String testName;
        private final float durationInSecond;

        private JkTestCaseResult(String className, String testName, float durationSec) {
            super();
            this.className = className;
            this.testName = testName;
            this.durationInSecond = durationSec;
        }

        /**
         * Constructs a test case result.
         */
        public static JkTestCaseResult of(String className, String testName, float durationSec) {
            return new JkTestCaseResult(className, testName, durationSec);
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
    public static class JkTestCaseFailure extends JkTestCaseResult implements Serializable {

        private static final long serialVersionUID = 7089021299483181605L;

        private final JkExceptionDescription jkExceptionDescription;

        private JkTestCaseFailure(String className, String testName, float duration,
                                 JkExceptionDescription exception) {
            super(className, testName, duration);
            this.jkExceptionDescription = exception;
        }

        /**
         * Constructs a test case result.
         */
        public static JkTestCaseFailure of(String className, String testName, float duration,
                                 JkExceptionDescription exception) {
            return new JkTestCaseFailure(className, testName, duration, exception);
        }

        /**
         * Returns the description of the failure.
         */
        public JkExceptionDescription getExceptionDescription() {
            return jkExceptionDescription;
        }

    }

    /**
     * A result for a single test case execution in case of ignore.
     */
    public static class JkIgnoredCase extends JkTestCaseResult implements Serializable {

        private static final long serialVersionUID = 1L;

        private JkIgnoredCase(String className, String testName) {
            super(className, testName, 0);
        }

        /**
         * Constructs an ignored test case result.
         */
        public static JkIgnoredCase of(String className, String testName) {
            return new JkIgnoredCase(className, testName);
        }

    }

    private static String messageOrEmpty(String exceptionMessage) {
        if (exceptionMessage == null) {
            return "";
        }
        return " : " + exceptionMessage;
    }

    /**
     * Returns the ofSystem properties in use during the test suite execution.
     */
    public Properties getSystemProperties() {
        return systemProperties;
    }

    /**
     * Description of an execption occured during the test suite execution.
     */
    public static class JkExceptionDescription implements Serializable {

        private static final long serialVersionUID = -8619868712236132763L;

        private final String className;
        private final String message;
        private final StackTraceElement[] stackTrace;
        private final JkExceptionDescription cause;
        private final boolean assertError;


        private JkExceptionDescription(Throwable throwable) {
            super();
            this.className = throwable.getClass().getName();
            this.message = throwable.getMessage();
            this.stackTrace = throwable.getStackTrace();
            if (throwable.getCause() != null) {
                this.cause = new JkExceptionDescription(throwable.getCause());
            } else {
                this.cause = null;
            }
            assertError = AssertionError.class.isAssignableFrom(throwable.getClass());

        }

        /**
         * Constructs an exception description.
         */
        public static JkExceptionDescription of(Throwable throwable) {
            return new JkExceptionDescription(throwable);
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
        public JkExceptionDescription getCause() {
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
        public List<String> getStackTraceAsStrings() {
            return getStackTraceAsStrings(className + ": " + message);
        }

        private List<String> getStackTraceAsStrings(String prefix) {
            final List<String> result = new LinkedList<>();
            if (prefix != null) {
                result.add(prefix);
            }
            for (final StackTraceElement element : stackTrace) {
                result.add("  at " + element);
            }
            if (cause != null) {
                result.addAll(cause.getStackTraceAsStrings("Caused by : " + cause.getClassName()
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
        final List<JkTestCaseFailure> failures = new ArrayList<>(
                junitFailures.size());
        for (final Object junitFailure : junitFailures) {
            failures.add(fromJunit4Failure(junitFailure));
        }
        return new JkTestSuiteResult(properties, suiteName, runCount, ignoreCount, failures,
                durationInMillis);

    }

    private static JkTestCaseFailure fromJunit4Failure(Object junit4failure) {
        final Object junit4Description = JkUtilsReflect.invoke(junit4failure, "getDescription");
        final String testClassName = JkUtilsReflect.invoke(junit4Description, "getClassName");
        final String testMethodName = JkUtilsReflect.invoke(junit4Description, "getMethodName");
        final Throwable exception = JkUtilsReflect.invoke(junit4failure, "getException");
        final JkExceptionDescription description = new JkExceptionDescription(exception);
        return new JkTestCaseFailure(testClassName, testMethodName, -1, description);
    }

    static JkTestCaseFailure fromJunit3Failure(Object junit3failure) {
        final Object failedTest = JkUtilsReflect.invoke(junit3failure, "failedTest");
        final Throwable exception = JkUtilsReflect.invoke(junit3failure, "thrownException");
        final JkExceptionDescription description = new JkExceptionDescription(exception);
        final String failedTestName = failedTest.toString();
        final int firstParenthesisIndex = failedTestName.indexOf("(");
        final String methodName = failedTestName.substring(0, firstParenthesisIndex);
        return new JkTestCaseFailure(failedTest.getClass().getName(), methodName,
                -1, description);
    }

}
