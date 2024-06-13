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

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.PrintStream;

/**
 * This decorator shows minimalist task decoration...
 */
public final class JkFlatLogDecorator extends JkLog.JkLogDecorator {

    private static final String HEADER_LINE = JkUtilsString.repeat("=", 120);

    private transient PrintStream out;

    private transient PrintStream err;

    @Override
    protected void init(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    PrintStream getOut() {
        return out;
    }

    @Override
    PrintStream getErr() {
        return err;
    }


    public void handle(JkLog.JkLogEvent event) {
        JkLog.Type logType = event.getType();
        PrintStream out = getOut();
        PrintStream err = getErr();
        PrintStream stream = getOut();
        if (logType == JkLog.Type.ERROR || logType == JkLog.Type.WARN) {
            out.flush();
            stream = err;
        } else {
            err.flush();
        }
        String message = event.getMessage();
        if (logType == JkLog.Type.END_TASK) {
            if (!event.getMessage().isEmpty()) {
                err.flush();
                out.println(event.getMessage());
            }
        } else if (logType == JkLog.Type.START_TASK) {
            err.flush();
            out.println();
            out.println("Task: " + message);
        } else {
            stream.println(message);
        }
    }











}
