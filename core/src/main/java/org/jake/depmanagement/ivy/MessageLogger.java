package org.jake.depmanagement.ivy;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.jake.JakeLog;
import org.jake.JakeOptions;

@SuppressWarnings("unchecked")
class MessageLogger extends AbstractMessageLogger {

	@Override
	public void log(String message, int level) {
		switch (level) {
		case Message.MSG_ERR :
			JakeLog.error(message);
			break;
		case Message.MSG_WARN :
			JakeLog.warn(message);
			break;
		case Message.MSG_INFO :
			JakeLog.info(message);
			break;
		case Message.MSG_VERBOSE :
			if (JakeOptions.isVerbose()) {
				JakeLog.info(message);
			}
			break;
		case Message.MSG_DEBUG :
			if (JakeOptions.isVerbose()) {
				JakeLog.info(message);
			}
			break;
		default :
			JakeLog.info("["+level+"] " + message);
		}

	}

	@Override
	public void rawlog(String msg, int level) {
		log(msg, level);
	}

	@Override
	public void doProgress() {
		JakeLog.infoStream().print(".");
	}

	@Override
	public void doEndProgress(String msg) {
		JakeLog.infoStream().println(msg);
	}




}