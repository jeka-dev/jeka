/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.testing;

import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkException;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class JkTestResult implements Serializable {

    private static final long serialVersionUID = -855794466882934307L;

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

        private static final long serialVersionUID = -6936658324403096375L;

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
                    "tests found=" + found +
                    ", started=" + started +
                    ", skipped=" + skipped +
                    ", aborted=" + aborted +
                    ", succeed=" + succeeded +
                    ", failed=" + failed +
                    '}';
        }

        public String toReportString() {
            return "Test Found: " + found +
                    ", Run: " + started +
                    ", Fail: " + failed +
                    ", Success: " + succeeded +
                    ", Skipped: " + skipped +
                    ", Aborted: " + aborted;
        }
    }


    public void assertSuccess() {
        if (failures.isEmpty()) {
            return;
        }
        throw new JkException("Failures detected in test execution");
    }

    public static final class JkFailure implements Serializable {

        private static final long serialVersionUID = -2836260954120708050L;

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

        public String getThrowableClassname() {
            return throwableClassName;
        }

        public boolean isAssertionFailure() {
            return "org.opentest4j.AssertionFailedError".equals(throwableClassName);
        }

        public String getThrowableMessage() {
            return throwableMessage;
        }

        public StackTraceElement[] getStackTraces() {
            return stackTraces;
        }

        @Override
        public String toString() {
            return "{" +
                    "testId=" + testId +
                    ", throwableMessage='" + throwableMessage + '\'' +
                    ", stacktraces=" + Arrays.toString(stackTraces) +
                    '}';
        }


    }

    public static final class JkTestIdentifier implements Serializable {

        private static final long serialVersionUID = 6798288174695984997L;

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
