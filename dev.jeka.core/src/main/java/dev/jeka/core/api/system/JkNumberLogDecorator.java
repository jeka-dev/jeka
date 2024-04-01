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
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * This decorator adds task numbering
 * s: 1
 * s: 1.1
 * s: 1.1.1
 * e:
 * s: 1.1.2
 */
public final class JkNumberLogDecorator extends JkLog.JkLogDecorator {

    private static final String HEADER_LINE = JkUtilsString.repeat("=", 120);

    private transient PrintStream out;

    private transient PrintStream err;

    private final LinkedList<Integer> stack = new LinkedList<>();

    private int next = 1;

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
            out.println();
            endTask();

        } else if (logType == JkLog.Type.START_TASK) {
            startNewTask();
            String title = String.format("Task %s: %s", toNumber(), message);
            err.flush();
            out.println();
            out.println(title);
        } else {
            stream.println(message);
        }
    }

    private void startNewTask() {
        stack.addLast(next);
    }

    private void endTask() {
        int last = stack.pollLast();
        next = last + 1;
    }

    private String toNumber() {
        return stack.stream().map(i -> Integer.toString(i)).collect(Collectors.joining("."));
    }








}
