package org.jerkar.api.depmanagement;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.jerkar.api.system.JkLog;

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
            JkLog.trace(message);
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
        JkLog.infoStream().print(".");
    }

    @Override
    public void doEndProgress(String msg) {
        JkLog.infoStream().println(msg);
    }

}