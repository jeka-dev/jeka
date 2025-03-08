package dev.jeka.core.api.system;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class JkLogTest {

    @Test
    @Disabled
    void testMultiThread() throws Exception {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.info("toto");
        Runnable runnable = () -> {
            JkLog.startTask("new thread");
            JkLog.endTask();
        };
        Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
    }
}
