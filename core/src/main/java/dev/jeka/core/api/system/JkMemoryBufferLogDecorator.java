/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class JkMemoryBufferLogDecorator extends JkLog.JkLogDecorator {

    private static JkMemoryBufferLogDecorator instance;

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
        JkUtilsAssert.state(delegate != instance, "This decorator is currently used by JkLog." +
                " Inactivate it prior rebind again." );
        instance = new JkMemoryBufferLogDecorator(delegate);
        JkLog.setDecorator(instance);
    }

    public static void inactivateOnJkLog() {
        JkUtilsAssert.state(instance != null, "This decorator is not currently activated.");
        JkLog.JkLogDecorator delegate = instance.delegate;
        delegate.init(instance.originalOut, instance.originalErr);
        instance = null;
        JkLog.setDecorator(delegate);
    }

    public static boolean isActive() {
        return instance != null;
    }

    /**
     * Flush on the target outputStream of the delegate decorator.
     */
    public static void flush() {
        JkUtilsAssert.state(isActive(),"This decorator must be activated in order to flush");
        byte[] bytes = instance.byteArrayBufferStream.toByteArray();
        JkUtilsIO.write(instance.delegate.getTargetOut(), bytes);
    }

    private static ByteArrayOutputStream byteArrayBufferStream() {
        return new ByteArrayOutputStream();
    }
}
