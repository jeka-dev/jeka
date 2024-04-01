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
