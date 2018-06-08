package org.jerkar.api.depmanagement;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.jerkar.api.system.JkEvent;

class IvyMessageLogger extends AbstractMessageLogger {

    @Override
    public void log(String message, int level) {
        message = "[Ivy] " + message.trim();
        switch (level) {
        case Message.MSG_ERR:
            JkEvent.error(this, message);
            break;
        case Message.MSG_WARN:
            JkEvent.warn(this, message);
            break;
        case Message.MSG_INFO:
            JkEvent.info(this, message);
            break;
        case Message.MSG_VERBOSE:
            JkEvent.trace(this, message);
            break;
        case Message.MSG_DEBUG:
            JkEvent.trace(this, message);
            break;
        default:
            JkEvent.info(this,"[" + level + "] " + message);
        }

    }

    @Override
    public void rawlog(String msg, int level) {
        this.log(msg, level);
    }

    @Override
    public void doProgress() {
        JkEvent.progress(this, ".");
    }

    @Override
    public void doEndProgress(String msg) {
        JkEvent.progress(this, msg);
    }

}