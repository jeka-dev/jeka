package dev.jeka.core.api.system;

import org.junit.Test;

public class JkLogTest {

    @Test
    public void testMultithread() throws Exception {
        JkLog.registerHierarchicalConsoleHandler();
        JkLog.info("toto");
        Runnable runnable = () -> {JkLog.startTask("new thread");};
        Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
    }
}
