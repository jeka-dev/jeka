package dev.jeka.core.api.testing;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.system.JkLog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class JkTestProcessorNoMatchTest {

    @Test
    void noTestMatchesIncludePattern_warn() throws Exception {
        String log = run("NoSuchTestZzz", 0);
        assertTrue(log.contains("No test matches the include patterns [NoSuchTestZzz]"), log);
    }

    @Test
    void testMatchesIncludePattern_noWarn() throws Exception {
        String log = run(".*JkModuleIdTest", -1);
        assertFalse(log.contains("No test matches"), log);
    }

    private String run(String pattern, int expectedCount) throws Exception {
        Path testClasses = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        JkTestProcessor processor = JkTestProcessor.of(
                () -> JkPathSequence.of(testClasses), () -> testClasses)
                .setForkingProcess(false)
                .setRepoSetSupplier(() -> JkRepo.ofMavenCentral().toSet());
        processor.engineBehavior.setProgressDisplayer(JkTestProcessor.JkProgressStyle.MUTE);
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        PrintStream initialOut = JkLog.getOutPrintStream();
        PrintStream initialErr = JkLog.getErrPrintStream();
        PrintStream stream = new PrintStream(log, true);
        JkLog.redirect(stream, stream);
        final JkTestResult result;
        try {
            result = processor.run(JkTestSelection.of().addIncludePatterns(pattern));
        } finally {
            JkLog.redirect(initialOut, initialErr);
        }
        if (expectedCount >= 0) {
            assertEquals(expectedCount, result.getTestCount().getFound());
        } else {
            assertTrue(result.getTestCount().getFound() > 0);
        }
        assertTrue(result.getFailures().isEmpty());
        return log.toString();
    }

}
