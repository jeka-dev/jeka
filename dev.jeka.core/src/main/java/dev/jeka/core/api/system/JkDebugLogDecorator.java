package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.api.system.JkIndentLogDecorator.LINE_SEPARATOR;
import static dev.jeka.core.api.system.JkIndentLogDecorator.MARGIN_UNIT;

/**
 * Consumer behaving like {@link JkIndentLogDecorator} but displaying the source line where each log occurs.
 */
public final class JkDebugLogDecorator extends JkLog.JkLogDecorator {

    private transient MarginStream marginOut;

    private transient MarginStream marginErr;

    private transient PrintStream out;

    private transient PrintStream err;

    protected void init(PrintStream targetOut, PrintStream tergetErr) {
        marginOut = new MarginStream(targetOut);
        marginErr = new MarginStream(tergetErr);
        out = new PrintStream(marginOut);
        err = new PrintStream(marginErr);
    }

    private void readObject(ObjectInputStream objectInputStream) {
        marginOut = new MarginStream(System.out);
        marginErr = new MarginStream(System.err);
    }

    public void handle(JkLog.JkLogEvent event) {
        JkLog.Type logType = event.getType();
        PrintStream stream = out;
        if (logType == JkLog.Type.ERROR || logType == JkLog.Type.WARN) {
            stream.flush();
            stream = err;
        }
        String message = event.getMessage();
        if (logType == JkLog.Type.END_TASK) {
        } else if (logType== JkLog.Type.START_TASK) {
            stream.print(message);
            marginOut.notifyStart();
            marginErr.notifyStart();
        } else {
            stream.println(message);
        }
    }

    @Override
    public PrintStream getOut() {
        return out;
    }

    @Override
    public PrintStream getErr() {
        return err;
    }

    static class MarginStream extends OutputStream {

        private static final int STACKTRACE_PREFIX_SIZE = 70;

        private final PrintStream delegate;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for ofSystem.err)

        private boolean pendingStart;

        private boolean endTask;

        void notifyStart() {
            flush();
            pendingStart = true;
        }

        public MarginStream(PrintStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int aByte) throws IOException {
            if (pendingStart & !endTask) {
                delegate.write(LINE_SEPARATOR);
                lastByte = LINE_SEPARATOR;
                pendingStart = false;
            }
            if (lastByte == LINE_SEPARATOR) {
                Integer level = JkLog.getCurrentNestedLevel();
                if (endTask) level++;
                delegate.write(stackTraceSuffix().getBytes(StandardCharsets.UTF_8));
                for (int j = 0; j < level; j++) {
                    delegate.write(MARGIN_UNIT);
                }
            }
            delegate.write(aByte);
            lastByte = aByte;
        }

        private static String stackTraceSuffix() {
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            StackTraceElement element = findStackElementNext(elements, JkLog.class, null);
            if (element == null) {
                element = findStackElementNext(elements, PrintStream.class, "print");
            }
            if (element != null) {
                return JkUtilsString.padEnd(format(element), STACKTRACE_PREFIX_SIZE, ' ') + ":";
            }
            return JkUtilsString.padEnd("", STACKTRACE_PREFIX_SIZE, ' ') + ":";
        }

        private static StackTraceElement findStackElementNext(StackTraceElement[] elements, Class clazz, String method) {
            boolean found = false;
            for (int i = 0; i < elements.length; i++) {
                StackTraceElement stackTraceElement = elements[i];
                String className = stackTraceElement.getClassName();
                boolean matchClass = className.equals(clazz.getName());
                boolean matchMethod = method == null ? true : method.equals(stackTraceElement.getMethodName());
                if (matchClass && matchMethod) {
                    found = true;
                    continue;
                }
                if (found && !matchClass) {
                    return stackTraceElement;
                }

            }
            return null;
        }

        private static String format(StackTraceElement stackTraceElement) {
            String line = String.format("%s.%s(line:%s)", extractClassName(stackTraceElement.getClassName()),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getLineNumber());
            if (line.length() > STACKTRACE_PREFIX_SIZE) {
                line = line.substring(0, STACKTRACE_PREFIX_SIZE);
            }
            return JkUtilsString.padEnd(line, STACKTRACE_PREFIX_SIZE, ' ');
        }

        private static String extractClassName(String fullClassName) {
            List<String> resultItems = new LinkedList<>();
            String[] items = fullClassName.split("\\.");
            for (int i = 0; i < items.length; i++) {
                String item = items[i];
                String firstLetter = item.substring(0, 1);
                if (firstLetter.equals(firstLetter.toUpperCase())) {
                    resultItems.add(item);
                } else {
                    resultItems.add(firstLetter);
                }
            }
            return String.join(".", resultItems);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

    }
}
