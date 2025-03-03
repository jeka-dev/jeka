package dev.jeka.core.api.system;

import org.junit.Ignore;
import org.junit.Test;

public class JkLogTest {

    @Test
    @Ignore
    public void testMultiThread() throws Exception {
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
