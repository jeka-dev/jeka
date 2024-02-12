package dev.jeka.core.api.testing;

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class JkTestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long timeStarted;

    private final long timeFinished;

    private final JkCount containerCount;

    private final JkCount testCount;

    private final List<JkFailure> failures;

    private JkTestResult(long timeStarted, long timeFinished, JkCount containerCount, JkCount testCount, List<JkFailure> failures) {
        this.timeStarted = timeStarted;
        this.timeFinished = timeFinished;
        this.containerCount = containerCount;
        this.testCount = testCount;
        this.failures = failures;
    }

    public static JkTestResult of(long timeStarted, long timeFinished, JkCount containerCount, JkCount testCount, List<JkFailure> failures) {
        return new JkTestResult(timeStarted, timeFinished, containerCount, testCount, failures);
    }

    public static JkTestResult of() {
        return of(System.currentTimeMillis(), System.currentTimeMillis(), JkCount.of(), JkCount.of(),
                Collections.emptyList());
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public long getTimeFinished() {
        return timeFinished;
    }

    public JkCount getContainerCount() {
        return containerCount;
    }

    public JkCount getTestCount() {
        return testCount;
    }

    public List<JkFailure> getFailures() {
        return failures;
    }

    @Override
    public String toString() {
        return "{" +
                "timeStarted=" + timeStarted +
                ", timeFinished=" + timeFinished +
                ", containerCount=" + containerCount +
                ", testCount=" + testCount +
                ", failures=" + failures +
                '}';
    }

    public static class JkCount implements Serializable {

        private static final long serialVersionUID = 1L;

        private final long found;

        private final long started;

        private final long skipped;

        private final long aborted;

        private final long succeeded;

        private final long failed;

        private JkCount(long found, long started, long skipped, long aborted, long succeeded, long failed) {
            this.found = found;
            this.started = started;
            this.skipped = skipped;
            this.aborted = aborted;
            this.succeeded = succeeded;
            this.failed = failed;
        }

        public static JkCount of(long found, long started, long skipped, long aborted, long succeded, long failed) {
            return new JkCount(found, started, skipped, aborted, succeded, failed);
        }

        private static JkCount of() {
            return new JkCount(0, 0, 0, 0, 0, 0);
        }

        public long getFound() {
            return found;
        }

        public long getStarted() {
            return started;
        }

        public long getSkipped() {
            return skipped;
        }

        public long getAborted() {
            return aborted;
        }

        public long getSucceeded() {
            return succeeded;
        }

        public long getFailed() {
            return failed;
        }

        @Override
        public String toString() {
            return "{" +
                    "found=" + found +
                    ", started=" + started +
                    ", skipped=" + skipped +
                    ", aborted=" + aborted +
                    ", succeded=" + succeeded +
                    ", failed=" + failed +
                    '}';
        }
    }

    public void printFailures(PrintStream printStream) {
        for (JkFailure failure : failures) {
            failure.print(printStream);
            printStream.println();
        }
    }

    public void assertNoFailure() {
        if (failures.isEmpty()) {
            return;
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PrintStream printStream = new PrintStream(os);
            printStream.println( failures.size() + " test failures : ");
            printStream.println();
            printFailures(printStream);
            String message = os.toString("UTF8");
            throw new IllegalStateException(message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public static final class JkFailure implements Serializable {

        private static final long serialVersionUID = 1L;

        private final JkTestIdentifier testId;

        private final String throwableClassName;

        private final String throwableMessage;

        private final StackTraceElement[] stackTraces;

        private JkFailure(JkTestIdentifier testId, String throwableClassname,
                          String throwableMessage, StackTraceElement[] stacktraces) {
            this.testId = testId;
            this.throwableMessage = throwableMessage;
            this.stackTraces = stacktraces;
            this.throwableClassName = throwableClassname;
        }

        public static JkFailure of(JkTestIdentifier testId, String throwableClassname, String throwableMessage, StackTraceElement[] stacktraces) {
            return new JkFailure(testId, throwableClassname, throwableMessage, stacktraces);
        }

        public JkTestIdentifier getTestId() {
            return testId;
        }

        public String getThrowableMessage() {
            return throwableMessage;
        }

        public StackTraceElement[] getStackTraces() {
            return stackTraces;
        }

        @Override
        public String toString() {
            System.out.println();
            return "{" +
                    "testId=" + testId +
                    ", throwableMessage='" + throwableMessage + '\'' +
                    ", stacktraces=" + Arrays.toString(stackTraces) +
                    '}';
        }

        public String shortMessage(int stackTraceEl) {
            StringBuilder result = new StringBuilder();
            result.append(String.format("%s %n        %s",
                    testId.displayName,
                    JkUtilsString.wrapStringCharacterWise(throwableClassName + " : " + throwableMessage, 120)));
            for (int i=0; i<= stackTraceEl && i < stackTraces.length; i++) {
                result.append("\nat ").append(stackTraces[i]);
            }
            result.append("\n...");
            return JkUtilsString.withLeftMargin(result.toString(), "        ");
        }

        void print(PrintStream printStream) {
            printStream.print(testId + System.lineSeparator() + "-> ");
            printStream.println(throwableMessage);
            for (final StackTraceElement element : getStackTraces()) {
                printStream.println("  at " + element);
            }
        }
    }

    public static final class JkTestIdentifier implements Serializable {

        private static final long serialVersionUID = 1L;

        private final JkType type;

        private final String id;

        private final String displayName;

        private final Set<String> tags;

        private JkTestIdentifier(JkType type, String id, String displayName, Set<String> tags) {
            this.type = type;
            this.id = id;
            this.displayName = displayName;
            this.tags = tags;
        }

        public static JkTestIdentifier of(JkType type, String id, String displayName, Set<String> tags) {
            return new JkTestIdentifier(type, id, displayName, tags);
        }

        public JkType getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Set<String> getTags() {
            return tags;
        }

        @Override
        public String toString() {
            return "{" +
                    "type=" + type +
                    ", id='" + id + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", tags=" + tags +
                    '}';
        }

        public enum JkType {
            TEST, CONTAINER, CONTAINER_AND_TEST
        }

    }

}
