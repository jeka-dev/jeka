package dev.jeka.core.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @Test
    void errorMessage_noMessage_showsExceptionAndCause() {
        Throwable t = new ExceptionInInitializerError(new IllegalStateException("Enum init failed"));

        assertEquals("java.lang.ExceptionInInitializerError caused by java.lang.IllegalStateException: "
                + "Enum init failed", Main.errorMessage(t));
        assertEquals("java.lang.NullPointerException", Main.errorMessage(new NullPointerException()));
        assertEquals("Bad value", Main.errorMessage(new IllegalArgumentException("Bad value")));
    }

}
