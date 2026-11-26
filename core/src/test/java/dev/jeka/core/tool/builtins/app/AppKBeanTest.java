package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.tool.JkException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

class AppKBeanTest {

    @Test
    void ask_withClosedInput_failsWithClearMessage() {
        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(new byte[0]));
        try {
            JkException e = Assertions.assertThrows(JkException.class, () -> AppKBean.ask("Add? [N,y]:"));
            Assertions.assertTrue(e.getMessage().contains("interactive terminal"), e.getMessage());
        } finally {
            System.setIn(originalIn);
        }
    }

}
