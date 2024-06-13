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

import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.PrintStream;

public class JkBusyIndicator {

    private static final long WAIT_TIME = 50L;

    private static JkBusyIndicator instance;

    private final String text;

    private boolean stopped;

    private Thread thread;

    private final PrintStream printStream;

    private JkBusyIndicator(String text, PrintStream printStream) {
        this.text = text;
        this.printStream = printStream;
    }

    public static void start(PrintStream printStream, String message) {
        if (!JkLog.isAnimationAccepted()) {
            return;
        }
        if (instance != null) {
            throw new IllegalStateException("A running instance of Busy Indicator is already running. " +
                    "Stop it prior starting a new one.");
        }
        instance = new JkBusyIndicator(message, printStream);
        instance.printStream.print(instance.text + " ");
        instance.printStream.flush();
        instance.thread = new Thread(instance::round);
        instance.thread.setPriority(Thread.MIN_PRIORITY);
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
        char[] ticSequence = new char[] {'/', '-', '\\', '|'};
        while (!stopped) {
            for (char ticChar : ticSequence) {
                tic(ticChar);
                if (stopped) {
                    break;
                }
            }
        }
        printStream.write(' ');  // On some consoles, '\b' does only a cursor left move without deleting the content
        printStream.write('\b');
        for (int i = 0; i <= instance.text.length(); i++) {
            printStream.write('\b');
            printStream.write(' ');
            printStream.write('\b');
        }
        printStream.flush();
    }

    private void tic(char character) {
        printStream.write(character);
        printStream.flush();
        JkUtilsSystem.sleep(WAIT_TIME);
        printStream.write('\b');
    }

}
