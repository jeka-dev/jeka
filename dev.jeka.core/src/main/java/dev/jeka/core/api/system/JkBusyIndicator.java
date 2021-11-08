package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.PrintStream;

public class JkBusyIndicator {

    private static final long WAIT_TIME = 40L;

    private static JkBusyIndicator instance;

    private final String text;

    private boolean stopped;

    private Thread thread;

    private final PrintStream printStream;

    private JkBusyIndicator(String text, PrintStream printStream) {
        this.text = text;
        this.printStream = printStream;
    }

    public static void start(String message) {
        if (instance != null) {
            throw new IllegalStateException("A running instance of Busy Indicator is already running. " +
                    "Stop it prior starting a new one.");
        }
        instance = new JkBusyIndicator(message, JkLog.getOutPrintStream());
        instance.printStream.print(instance.text + " ");
        instance.printStream.flush();
        instance.thread = new Thread(instance::round);
        instance.thread.setName("Jeka-busyIndicator");
        instance.thread.start();
    }

    public synchronized static void stop() {
        if (instance == null) {
            return;
        }
        instance.stopped = true;
        JkUtilsSystem.join(instance.thread);
        instance = null;
    }

    private void round() {
        while (!stopped) {
            printStream.print('/');
            printStream.flush();
            JkUtilsSystem.sleep(WAIT_TIME);
            printStream.print('\b');
            printStream.print('-');
            printStream.flush();
            JkUtilsSystem.sleep(WAIT_TIME);
            printStream.print('\b');
            printStream.print('\\');
            printStream.flush();
            JkUtilsSystem.sleep(WAIT_TIME);
            printStream.print('\b');
            printStream.print('|');
            printStream.flush();
            JkUtilsSystem.sleep(WAIT_TIME);
            printStream.print('\b');
            printStream.flush();
        }
        printStream.print(' ');  // On some consoles, '\b' does only a cursor left move without deleting the content
        printStream.print('\b');
        for (int i = 0; i <= instance.text.length(); i++) {
            printStream.print('\b');
            printStream.print(' ');
            printStream.print('\b');
        }
        printStream.flush();
    }

}
