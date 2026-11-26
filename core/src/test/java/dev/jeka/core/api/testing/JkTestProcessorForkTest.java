package dev.jeka.core.api.testing;

import dev.jeka.core.api.java.JkJavaVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkTestProcessorForkTest {

    @Test
    void checkForkedJavaVersion_below21_failsWithClearMessage() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> JkTestProcessor.checkForkedJavaVersion(JkJavaVersion.V17));
        assertTrue(e.getMessage().contains("@project.test.fork=false"), e.getMessage());
    }

    @Test
    void checkForkedJavaVersion_21orUnknown_passes() {
        assertDoesNotThrow(() -> JkTestProcessor.checkForkedJavaVersion(JkJavaVersion.V21));
        assertDoesNotThrow(() -> JkTestProcessor.checkForkedJavaVersion(JkJavaVersion.of("25")));
        assertDoesNotThrow(() -> JkTestProcessor.checkForkedJavaVersion(null));
    }

}
