package dev.jeka.core.api.system;

import java.io.Console;
import java.io.PrintStream;

public class JkConsoleSpinner {

    private final String message;

    private String alternativeMassage;

    private JkConsoleSpinner(String message, String alternativeMassage) {
        this.message = message;
        this.alternativeMassage = alternativeMassage;
    }

    public static JkConsoleSpinner of(String message) {
        return new JkConsoleSpinner(message, null);
    }

    public JkConsoleSpinner setAlternativeMassage(String alternativeMassage) {
        this.alternativeMassage = alternativeMassage;
        return this;
    }

    public void run(Runnable runnable) {
        Console console = System.console();
        if (console == null || JkLog.isVerbose() || !JkLog.isAnimationAccepted()
                || JkMemoryBufferLogDecorator.isActive()) {
            if (alternativeMassage != null) {
                JkLog.info(alternativeMassage);
            }
            runnable.run();
            return;
        }
        PrintStream printStream = JkLog.getErrPrintStream();
        JkMemoryBufferLogDecorator.activateOnJkLog();
        JkBusyIndicator.start(printStream, message);
        try {
            runnable.run();
        } catch (Throwable t) {
            JkMemoryBufferLogDecorator.flush();
            throw t;
        } finally {
            JkBusyIndicator.stop();
            JkMemoryBufferLogDecorator.inactivateOnJkLog();
        }
    }



}
