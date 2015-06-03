package org.jerkar.internal.ivy;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.jerkar.JkLog;
import org.jerkar.JkOptions;

@SuppressWarnings("unchecked")
class MessageLogger extends AbstractMessageLogger {

	@Override
	public void log(String message, int level) {
		switch (level) {
		case Message.MSG_ERR :
			JkLog.error(message);
			break;
		case Message.MSG_WARN :
			JkLog.warn(message);
			break;
		case Message.MSG_INFO :
			JkLog.info(message);
			break;
		case Message.MSG_VERBOSE :
			if (JkOptions.isVerbose()) {
				JkLog.info(message);
			}
			break;
		case Message.MSG_DEBUG :
			if (JkOptions.isVerbose()) {
				JkLog.info(message);
			}
			break;
		default :
			JkLog.info("["+level+"] " + message);
		}

	}

	@Override
	public void rawlog(String msg, int level) {
		log(msg, level);
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