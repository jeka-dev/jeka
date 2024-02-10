package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class JkMemoryBufferLogDecorator extends JkLog.JkLogDecorator {

    private static JkMemoryBufferLogDecorator jkLogInstance;

    private final JkLog.JkLogDecorator delegate;

    private final ByteArrayOutputStream byteArrayBufferStream = byteArrayBufferStream();

    private final PrintStream bufferStream = new PrintStream(byteArrayBufferStream);

    private final PrintStream originalOut;

    private final PrintStream originalErr;

    private JkMemoryBufferLogDecorator(JkLog.JkLogDecorator delegate) {
        this.delegate = delegate;
        this.originalOut = delegate.getTargetOut();
        this.originalErr = delegate.getTargetErr();
    }

    @Override
    public void init(PrintStream out, PrintStream err) {
        delegate.init(bufferStream, bufferStream);
    }

    @Override
    public PrintStream getOut() {
        return delegate.getErr();
    }

    @Override
    public PrintStream getErr() {
        return delegate.getErr();
    }

    @Override
    public void handle(JkLog.JkLogEvent event) {
        delegate.handle(event);
    }

    public static void activateOnJkLog() {
        JkLog.JkLogDecorator delegate = JkLog.getDecorator();
        JkUtilsAssert.state(delegate != jkLogInstance, "This decorator is currently used by JkLog." +
                " Inactivate it prior rebind again." );
        jkLogInstance = new JkMemoryBufferLogDecorator(delegate);
        JkLog.setDecorator(jkLogInstance);
    }

    public static void inactivateOnJkLog() {
        JkUtilsAssert.state(jkLogInstance != null, "This decorator is not currently activated.");
        JkLog.JkLogDecorator delegate = jkLogInstance.delegate;
        delegate.init(jkLogInstance.originalOut, jkLogInstance.originalErr);
        jkLogInstance = null;
        JkLog.setDecorator(delegate);
    }

    public static boolean isActive() {
        return jkLogInstance != null;
    }

    /**
     * Flush on the target outputStream of the delegate decorator.
     */
    public static void flush() {
        JkUtilsAssert.state(isActive(),"This decorator must be activated in order to flush");
        byte[] bytes = jkLogInstance.byteArrayBufferStream.toByteArray();
        JkUtilsIO.write(jkLogInstance.delegate.getTargetOut(), bytes);
    }

    private static ByteArrayOutputStream byteArrayBufferStream() {
        return new ByteArrayOutputStream();
    }
}
