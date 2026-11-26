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
    void ask_withAnswer_returnsIt() {
        withInput("y\n", () -> Assertions.assertEquals("y", AppKBean.ask("Add? [N,y]:")));
    }

    @Test
    void updateAndUninstall_noNameAnsweredWithUnknownApp_doNothing(@TempDir Path tempDir) throws Exception {
        AppKBean appKBean = KBeanTestLoader.load(tempDir.toRealPath(), AppKBean.class);
        appKBean.name = null;
        withInput("no-such-app-for-jeka-test\n", () -> Assertions.assertDoesNotThrow(appKBean::update));
        withInput("no-such-app-for-jeka-test\n", () -> Assertions.assertDoesNotThrow(appKBean::uninstall));
    }

    @Test
    void repoAndTagFromOption_ok() {
        TagBucket tagBucket = TagBucket.ofValues(java.util.List.of("1.0", "1.1"));

        Assertions.assertEquals("1.0",
                AppKBean.repoAndTagFromOption(" 1.0 ", "https://x/y.git", tagBucket, "main").tag);
        Assertions.assertNull(AppKBean.repoAndTagFromOption("@", "https://x/y.git", tagBucket, "main").tag);
        JkException e = Assertions.assertThrows(JkException.class,
                () -> AppKBean.repoAndTagFromOption("2.0", "https://x/y.git", tagBucket, "main"));
        Assertions.assertTrue(e.getMessage().contains("1.1"), e.getMessage());
    }

    private static void withInput(String input, Runnable runnable) {
        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        try {
            runnable.run();
        } finally {
            System.setIn(originalIn);
        }
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
