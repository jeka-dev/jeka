package dev.jeka.core.api.depmanagement;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import dev.jeka.core.api.system.JkLog;

import java.io.PrintStream;

class IvyMessageLogger extends AbstractMessageLogger {

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
            JkLog.info(message);
            break;
        case Message.MSG_VERBOSE:
            JkLog.trace(message);
            break;
        case Message.MSG_DEBUG:
            if (JkLog.Verbosity.QUITE_VERBOSE.equals(JkLog.verbosity())) {
                JkLog.trace(message);
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
        new PrintStream(JkLog.getOutputStream()).print(".");
    }

    @Override
    public void doEndProgress(String msg) {
        new PrintStream(JkLog.getOutputStream()).print(msg);
    }

}