package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBeanTestLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;

class AppKBeanTest {

    @Test
    void ask_withClosedInput_failsWithClearMessage() {
        assertFailsWithoutTerminal(() -> AppKBean.ask("Add? [N,y]:"));
    }

    @Test
    void updateAndUninstall_noNameWithClosedInput_failWithClearMessage(@TempDir Path tempDir) throws Exception {
        AppKBean appKBean = KBeanTestLoader.load(tempDir.toRealPath(), AppKBean.class);
        appKBean.name = null;
        assertFailsWithoutTerminal(appKBean::update);
        assertFailsWithoutTerminal(appKBean::uninstall);
    }

    @Test
    void install_noRepo_doesNothing(@TempDir Path tempDir) throws Exception {
        AppKBean appKBean = KBeanTestLoader.load(tempDir.toRealPath(), AppKBean.class);
        appKBean.repo = " ";
        Assertions.assertDoesNotThrow(appKBean::install);
    }

    private static void assertFailsWithoutTerminal(Executable executable) {
        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(new byte[0]));
        try {
            JkException e = Assertions.assertThrows(JkException.class, executable);
            Assertions.assertTrue(e.getMessage().contains("interactive terminal"), e.getMessage());
        } finally {
            System.setIn(originalIn);
        }
    }

}
