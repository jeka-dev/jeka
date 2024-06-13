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

package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.system.JkLog;
import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;

import java.io.PrintStream;

final class IvyMessageLogger extends AbstractMessageLogger {

    @Override
    public void log(String message, int level) {
        message = "[Ivy] " + message.trim();
        switch (level) {
        case Message.MSG_ERR:
            JkLog.error(message);
            break;
        case Message.MSG_WARN:
            JkLog.warn(message);
            break;
        case Message.MSG_INFO:
            JkLog.verbose(message);  // This should not appear at jeka info level
            break;
        case Message.MSG_VERBOSE:
            if (JkLog.Verbosity.DEBUG.equals(JkLog.verbosity())) {
                JkLog.verbose(message);
            }
            break;
        case Message.MSG_DEBUG:
            if (JkLog.Verbosity.DEBUG.equals(JkLog.verbosity())) {
                JkLog.verbose(message);
            }
            break;
        default:
            JkLog.info("[" + level + "] " + message);
        }

    }

    @Override
    public void rawlog(String msg, int level) {
        this.log(msg, level);
    }

    @Override
    public void doProgress() {
        new PrintStream(JkLog.getOutPrintStream()).print(".");
    }

    @Override
    public void doEndProgress(String msg) {
        new PrintStream(JkLog.getOutPrintStream()).print(msg);
    }

}