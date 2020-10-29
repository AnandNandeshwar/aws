package ps.sample.test.utils;

import org.jboss.logging.Logger;

/**
 *ExceptionPrinter utility class to print complete StackTrace of exception 
 * @author anand nandeshwar
 *
 */

public class ExceptionPrinter {
	
	private static Logger log = Logger.getLogger(ExceptionPrinter.class.getName());
	
	private ExceptionPrinter(){
		throw new IllegalStateException("ExceptionPrinter can not be instantiate it is utility class.");
	}
	
	public static void printWarningLogs(Exception ex,String className) {
		log.info("***ERROR_OCCURED_IN_CLASS==>" + className +"::EXCEPTION_MESSAGE==>"+ex.getMessage()+"::STACKTRACE==>");
		for(StackTraceElement stackTrace : ex.getStackTrace()) {
			if(log.isDebugEnabled() || log.isInfoEnabled()) 
				log.debug("/n"+stackTrace.toString());
		}
	}
}
