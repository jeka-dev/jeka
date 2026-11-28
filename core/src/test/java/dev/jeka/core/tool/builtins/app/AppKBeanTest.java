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
    void updateAndUninstall_noNameAnsweredWithUnknownApp_fail(@TempDir Path tempDir) throws Exception {
        AppKBean appKBean = KBeanTestLoader.load(tempDir.toRealPath(), AppKBean.class);
        appKBean.name = null;
        withInput("no-such-app-for-jeka-test\n", () -> assertUnknownApp(appKBean::update));
        withInput("no-such-app-for-jeka-test\n", () -> assertUnknownApp(appKBean::uninstall));
    }

    @Test
    void uninstall_unknownAppWithForce_doesNotFail(@TempDir Path tempDir) throws Exception {
        AppKBean appKBean = KBeanTestLoader.load(tempDir.toRealPath(), AppKBean.class);
        appKBean.name = "no-such-app-for-jeka-test";
        appKBean.force = true;
        Assertions.assertDoesNotThrow(appKBean::uninstall);
    }

    @Test
    void splitTagFragment_ok() {
        String url = "https://github.com/jeka-dev/demo-cowsay";
        Assertions.assertArrayEquals(new String[] {url, null}, AppKBean.splitTagFragment(" " + url + " ", null));
        Assertions.assertArrayEquals(new String[] {url, "0.0.9"}, AppKBean.splitTagFragment(url, "0.0.9"));
        Assertions.assertArrayEquals(new String[] {url, "0.0.9"}, AppKBean.splitTagFragment(url + "#0.0.9", null));
        Assertions.assertArrayEquals(new String[] {url, "0.0.9"}, AppKBean.splitTagFragment(url + "#0.0.9", " 0.0.9"));
        Assertions.assertArrayEquals(new String[] {url, "1.0"}, AppKBean.splitTagFragment(url + "#", "1.0"));
        Assertions.assertArrayEquals(new String[] {"cowsay@my-catalog", "0.0.9"},
                AppKBean.splitTagFragment("cowsay@my-catalog#0.0.9", ""));
    }

    @Test
    void splitTagFragment_conflictingTag_fails() {
        JkException e = Assertions.assertThrows(JkException.class,
                () -> AppKBean.splitTagFragment("https://github.com/jeka-dev/demo-cowsay#0.0.9", "0.0.8"));
        Assertions.assertTrue(e.getMessage().contains("'#0.0.9' while 'tag' is '0.0.8'"), e.getMessage());
    }

    private static void assertUnknownApp(Executable executable) {
        JkException e = Assertions.assertThrows(JkException.class, executable);
        Assertions.assertTrue(e.getMessage().contains("No app named 'no-such-app-for-jeka-test'"), e.getMessage());
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
