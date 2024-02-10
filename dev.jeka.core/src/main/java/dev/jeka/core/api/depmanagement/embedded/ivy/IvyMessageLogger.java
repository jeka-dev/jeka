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
            JkLog.trace(message);  // This should not appear at jeka info level
            break;
        case Message.MSG_VERBOSE:
            if (JkLog.Verbosity.QUITE_VERBOSE.equals(JkLog.verbosity())) {
                JkLog.trace(message);
            }
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
        new PrintStream(JkLog.getOutPrintStream()).print(".");
    }

    @Override
    public void doEndProgress(String msg) {
        new PrintStream(JkLog.getOutPrintStream()).print(msg);
    }

}