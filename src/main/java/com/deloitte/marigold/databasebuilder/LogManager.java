package com.deloitte.marigold.databasebuilder;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class acts as a single point of contact to log application related logger statements,that involves
 * in simple Strings to placeholder replacable values.
 * 
 * This Utility emphasizes the usability of SL4J Frame work which is termed as ,
 * The Simple Logging Facade for Java (SLF4J) serves as a simple facade or abstraction 
 * for various logging frameworks, such as java.util.logging, logback and log4j.
 * 
 * SLF4J allows the end-user to plug in the desired logging framework at deployment time.
 * Note that SLF4J-enabling your library/application implies the addition of only a single mandatory 
 * dependency, namely slf4j-api-1.7.7.jar.
 *
 * @author mapabba
 *
 */

public class LogManager {
	
	public static final String SPACE = " ";
    public static final String TAB = "\t";
    private static final String END_OF_LINE = System.getProperty("line.separator");
	private Logger logger = null;
	@SuppressWarnings("rawtypes")
	private static final Map loggers = new HashMap();
	
	private LogManager(@SuppressWarnings("rawtypes") Class c) {
		this.logger = LoggerFactory.getLogger(c);
	}
	
	@SuppressWarnings("unchecked")
	public static LogManager getLogger(@SuppressWarnings("rawtypes") Class c) {
		String className = c.getName();
		LogManager logger = (LogManager) loggers.get(className);
		if (logger == null) {
			synchronized (LogManager.class) {
				logger = (LogManager) loggers.get(className);
				if (logger == null) {
					logger = new LogManager(c);
					loggers.put(className, logger);
				}
			}
		}
		return logger;
	}
	
	/**
	 * This Method Logs an Error Message.
	 * @param message
	 */
	public void error(String message) {
		logger.error(message);
	}
	/**
	 * This Method Logs an Error Message with an Exception.
	 * @param message
	 */
	public void error(String message, Throwable t) {
		logger.error(message, t);
		printStackTrace(t);
	}
	
	
	/**
	 * This Method Logs an warn Message.
	 * @param message
	 */
	public void warn(String message) {
		logger.warn(message);
	}
	
	/**
	 * This Method Logs an Warn Message with an Exception.
	 * @param message
	 */
	public void warn(String message, Throwable t) {
		logger.warn(message, t);
		printStackTrace(t);
	}
	
	/**
	 * This Method Logs info Message .
	 * @param message
	 */
	public void info(String message) {
		logger.info(message);
	}
	
	/**
	 * This Method Logs debug Message .
	 * @param message
	 */
	public void debug(String message) {
		logger.debug(message);
	}

	//Including Messages with Place Holders also
	
	/**
	 * This method works with place holders and respective messages.
	 * usage :	logger.error("one two three: {} {} {}", new Object[]{"a", "b", "c"}, new Exception("something went wrong"));
	 * @param messageWPlaceHolders
	 * @param messageArr
	 * @param t
	 */
	public void error(String messageWPlaceHolders, Object[] messageArr,Throwable t) {
		logger.error(
				messageWPlaceHolders,
			    messageArr,
			    t);
		
		printStackTrace(t);
	}
	
	/**
	 * This method works with place holders and respective messages.
	 * usage :	logger.error("one two three: {} {} {}", new Object[]{"a", "b", "c"});
	 * @param messageWPlaceHolders
	 * @param messageArr
	 * @param t
	 */
	public void error(String messageWPlaceHolders, Object[] messageArr) {
		logger.error(
				messageWPlaceHolders,
			    messageArr
			    );
	}
	/**
	 * This method works with place holders and respective messages.
	 * usage :	logger.warn("one two three: {} {} {}", new Object[]{"a", "b", "c"}, new Exception("something went wrong"));
	 * @param messageWPlaceHolders
	 * @param messageArr
	 * @param t
	 */
	public void warn(String messageWPlaceHolders, Object[] messageArr,Throwable t) {
		logger.warn(
				messageWPlaceHolders,
			    messageArr,
			    t);
		
		printStackTrace(t);
	}

	/**
	 * This method works with place holders and respective messages.
	 * usage :	logger.warn("one two three: {} {} {}", new Object[]{"a", "b", "c"});
	 * @param messageWPlaceHolders
	 * @param Object[]  messageArr
	 * @param t
	 */
	public void warn(String messageWPlaceHolders, Object[] messageArr) {
		logger.warn(
				messageWPlaceHolders,
			    messageArr);
	}
	
	public void debug(String messageWPlaceHolders, Object[] messageArr) {
		logger.debug(
				messageWPlaceHolders,
			    messageArr
			    );
		
	}

	public void info(String messageWPlaceHolders, Object[] messageArr) {
		logger.info(
				messageWPlaceHolders,
			    messageArr
			    );
	 }
	
	//With Single Object
	/**
	 * This method works with place holders and respective messages.
	 * usage :	logger.warn("one two three: {}", "a",exception);
	 * @param messageWPlaceHolders
	 * @param Object[]  messageArr
	 * @param t
	 */
	public void error(String messageWPlaceHolders, Object message,Throwable t) {
		logger.error(
				messageWPlaceHolders,
			    message,
			    t);
		
		printStackTrace(t);
	}
	
	public void error(String messageWPlaceHolders, Object message) {
		logger.error(
				messageWPlaceHolders,
			    message
			    );
	}
	public void warn(String messageWPlaceHolders, Object message,Throwable t) {
		logger.warn(
				messageWPlaceHolders,
			    message,
			    t);
		
		printStackTrace(t);
	}
	public void warn(String messageWPlaceHolders, Object message) {
		logger.warn(
				messageWPlaceHolders,
			    message);
	}
	
	public void debug(String messageWPlaceHolders, Object message) {
		logger.debug(
				messageWPlaceHolders,
			    message
			    );
		
	}

	public void info(String messageWPlaceHolders, Object  message) {
		logger.info(
				messageWPlaceHolders,
			    message
			    );
		
	}
	/**
	 * Constructs all the stack trace elements.
	 * @param t
	 * @return
	 */
	private String printStackTrace( Throwable t){
	        StringBuilder sbl = new StringBuilder();
	    	sbl.append(END_OF_LINE);
	        sbl.append(t.toString());
	        for(StackTraceElement ste:t.getStackTrace()){
	        	sbl.append(END_OF_LINE);
	        	sbl.append(TAB);
	        	sbl.append(ste.toString());
	        }
	        return sbl.toString();
	    }
	
	


}