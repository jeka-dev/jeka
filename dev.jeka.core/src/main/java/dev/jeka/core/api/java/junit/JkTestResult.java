package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.system.JkException;

import java.io.*;
import java.util.Arrays;
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

        private final long succeded;

        private final long failed;

        private JkCount(long found, long started, long skipped, long aborted, long succeded, long failed) {
            this.found = found;
            this.started = started;
            this.skipped = skipped;
            this.aborted = aborted;
            this.succeded = succeded;
            this.failed = failed;
        }

        public static JkCount of(long found, long started, long skipped, long aborted, long succeded, long failed) {
            return new JkCount(found, started, skipped, aborted, succeded, failed);
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

        public long getSucceded() {
            return succeded;
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
                    ", succeded=" + succeded +
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
            printStream.println("" + failures.size() + " test failures : ");
            printStream.println();
            printFailures(printStream);
            String message = os.toString("UTF8");
            throw new JkException(message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public static final class JkFailure implements Serializable {

        private static final long serialVersionUID = 1L;

        private final JkTestIdentifier testId;

        private final String throwableMessage;

        private final StackTraceElement[] tacktraces;

        private JkFailure(JkTestIdentifier testId, String throwableMessage, StackTraceElement[] tacktraces) {
            this.testId = testId;
            this.throwableMessage = throwableMessage;
            this.tacktraces = tacktraces;
        }

        public static JkFailure of(JkTestIdentifier testId, String throwableMessage, StackTraceElement[] stacktraces) {
            return new JkFailure(testId, throwableMessage, stacktraces);
        }

        public JkTestIdentifier getTestId() {
            return testId;
        }

        public String getThrowableMessage() {
            return throwableMessage;
        }

        public StackTraceElement[] getTacktraces() {
            return tacktraces;
        }

        @Override
        public String toString() {
            System.out.println();
            return "{" +
                    "testId=" + testId +
                    ", throwableMessage='" + throwableMessage + '\'' +
                    ", tacktraces=" + Arrays.toString(tacktraces) +
                    '}';
        }

        void print(PrintStream printStream) {
            printStream.print(testId.displayName + " : ");
            printStream.println(throwableMessage);
            for (final StackTraceElement element : getTacktraces()) {
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
