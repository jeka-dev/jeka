package org.jake.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LogUtils {
	
	public static final String LOGGER_NAME = "org.javake";
	
	public static final String LOG_LEVEL_SYSTEM_PROPERTY = "org.javake.log.level"; 
	
	public static final String HANDLER_CLASS_SYSTEM_PROPERTY = "org.javake.log.handlerClass";
	
	
	private LogUtils() {
	}
	
	public static void setSystemPropertyLevelOr(Logger logger, Level defaultLevel) {
		String levelName = System.getProperty(LOG_LEVEL_SYSTEM_PROPERTY);
		Level level;
		if (levelName != null) {
			try {
				level = Level.parse(levelName);
			} catch (IllegalArgumentException e) {
				logger.warning("Log level '" + levelName + "' as defined in system property " + LOG_LEVEL_SYSTEM_PROPERTY + " is not a valid level. Use only value defined in class java.utils.logging.Level");
				level = defaultLevel;
			}
		} else {
			level = defaultLevel;
		}
		logger.setLevel(level);
	}
	
	public static Formatter createDefaultFormatter() {
		return new SimpleFormatter() {
			
			@Override
			public synchronized String format(LogRecord record) {
				if (record.getLevel() == Level.INFO) {
					return "- " + record.getMessage() + "\n";
				}
				return record.getLevel() + "- " + record.getMessage() + "\n";
			}
			
		};
	}
	
	public static Logger createDefaultLogger() {
		Logger logger = Logger.getLogger(LOGGER_NAME);
		String handlerClassName = System.getProperty(HANDLER_CLASS_SYSTEM_PROPERTY);
		Handler handler = createDefaultHandler();
		if (handlerClassName != null) {
			try {
				Class<?> handlerClass = Class.forName(handlerClassName);
				handler = (Handler) handlerClass.newInstance();
			} catch (ClassNotFoundException e) {
				System.err.println("Class with name mentionned in system property " 
			+ HANDLER_CLASS_SYSTEM_PROPERTY + ": " + handlerClassName + " does not exist : use default handler."  );
			} catch (InstantiationException e) {
				System.err.println("Class with name mentionned in system property " 
						+ HANDLER_CLASS_SYSTEM_PROPERTY + ": " + handlerClassName + " has thrown an exception while been instanciated : use default handler"  );
				e.printStackTrace(System.err);
			} catch (IllegalAccessException e) {
				System.err.println("Class with name mentionned in system property " 
						+ HANDLER_CLASS_SYSTEM_PROPERTY + ": " + handlerClassName + " does not define public no-agrs constructors : use default handler"  );
			}
		}
		logger.addHandler(handler);
		logger.setUseParentHandlers(false);
		return logger;
	}
	
	private static Handler createDefaultHandler() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(createDefaultFormatter());
		return handler;
	} 

}
