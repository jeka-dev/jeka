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

import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.JkExternalToolApi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * This decorator adds indentation for logs nested within a task.
 */
public final class JkIndentLogDecorator extends JkLog.JkLogDecorator {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String TASK = JkExternalToolApi.ansiText("@|blue Task: |@");

    private static final String WARN = JkExternalToolApi.ansiText("@|yellow WARN: |@");

    private static final String ERROR = JkExternalToolApi.ansiText("@|red ERROR: |@");

    private static final String DURATION = "Duration: ";

    static final byte LINE_SEPARATOR = 10;

    static final byte[] MARGIN_UNIT = ("      ").getBytes(UTF8);

    private transient MarginStream marginOut;

    private transient MarginStream marginErr;

    private transient PrintStream out;

    private transient PrintStream err;

    @Override
    protected void init(PrintStream targetOut, PrintStream targetErr) {
        marginOut = new MarginStream(targetOut);
        marginErr = new MarginStream(targetErr);
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
            out.flush();
            stream = err;
        } else {
            err.flush();
        }
        String message = event.getMessage();
        if (event.getType().isMessageType()) {
            if (event.getType() == JkLog.Type.WARN) {
                message = WARN + message;
            } else if (event.getType() == JkLog.Type.ERROR) {
                message = ERROR + message;
            } else {
                message = "[" + event.getType() + "] " + message;
            }
        }
        if (logType == JkLog.Type.END_TASK) {
            if (!JkUtilsString.isBlank(message)) {
                JkUtilsIO.write(stream, MARGIN_UNIT);
                stream.println(message);
            }
            if (JkLog.isShowTaskDuration()) {
                JkUtilsIO.write(stream, MARGIN_UNIT);
                stream.printf(DURATION +  "%s%n", JkUtilsTime.formatMillis(event.getDurationMs()));
            }
        } else if (logType== JkLog.Type.START_TASK) {
            marginErr.flush();
            out.println(TASK + message);
            marginOut.notifyStart();
            marginErr.notifyStart();
            marginErr.mustPrintMargin = true;
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

    private static class MarginStream extends OutputStream {

        private final PrintStream delegate;

        private volatile boolean mustPrintMargin;

        private void notifyStart() {
            flush();
        }

        public MarginStream(PrintStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int aByte) throws IOException {
            if (mustPrintMargin) {
                printMargin();
            }
            delegate.write(aByte);
            mustPrintMargin = (aByte == LINE_SEPARATOR);
        }

        void printMargin() throws IOException {
            Integer level = JkLog.getCurrentNestedLevel();
            for (int j = 0; j < level; j++) {
                delegate.write(MARGIN_UNIT);
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }

    }

}
