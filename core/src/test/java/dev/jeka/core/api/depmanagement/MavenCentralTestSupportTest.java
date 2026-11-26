package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenCentralTestSupportTest {

    @Test
    void runOrSkipIfCentralDown_passingCode_passes() {
        assertDoesNotThrow(() -> MavenCentralTestSupport.runOrSkipIfCentralDown(() -> {}));
    }

    @Test
    void runOrSkipIfDown_failingCodeAndRepoDown_isSkipped() {

        // Nothing listens on port 9
        assertThrows(TestAbortedException.class, () -> MavenCentralTestSupport.runOrSkipIfDown(
                "http://127.0.0.1:9/maven-metadata.xml", MavenCentralTestSupportTest::fail));
    }

    @Test
    void runOrSkipIfCentralDown_failingCode_failsOrIsSkippedDependingOnCentral() {
        Class<? extends Throwable> expected = MavenCentralTestSupport.probe(
                JkRepo.MAVEN_CENTRAL_URL + "/dev/jeka/jeka-core/maven-metadata.xml") == null
                ? AssertionFailedError.class : TestAbortedException.class;
        assertThrows(expected, () -> MavenCentralTestSupport.runOrSkipIfCentralDown(MavenCentralTestSupportTest::fail));
    }

    private static void fail() {
        throw new AssertionFailedError("simulated failure");
    }

}
