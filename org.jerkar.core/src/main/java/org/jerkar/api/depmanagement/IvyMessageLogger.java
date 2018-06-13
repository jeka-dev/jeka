package org.jerkar.api.depmanagement;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.jerkar.api.system.JkLog;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;

class IvyMessageLogger extends AbstractMessageLogger {

    @Override
    public void log(String message, int level) {
        message = "[Ivy] " + message.trim();
        switch (level) {
        case Message.MSG_ERR:
            JkLog.error(this, message);
            break;
        case Message.MSG_WARN:
            JkLog.warn(this, message);
            break;
        case Message.MSG_INFO:
            JkLog.info(this, message);
            break;
        case Message.MSG_VERBOSE:
            JkLog.trace(this, message);
            break;
        case Message.MSG_DEBUG:
           // JkEvent.trace(this, message);
            break;
        default:
            JkLog.info(this,"[" + level + "] " + message);
        }

    }

    @Override
    public void rawlog(String msg, int level) {
        this.log(msg, level);
    }

    @Override
    public void doProgress() {
        new PrintStream(JkLog.stream()).print(".");
    }

    @Override
    public void doEndProgress(String msg) {
        new PrintStream(JkLog.stream()).print(msg);
    }

}