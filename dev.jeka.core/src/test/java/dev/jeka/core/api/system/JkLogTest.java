package dev.jeka.core.api.system;

public class JkLogTest {

    //@Test
    public void testMultithread() throws Exception {
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
