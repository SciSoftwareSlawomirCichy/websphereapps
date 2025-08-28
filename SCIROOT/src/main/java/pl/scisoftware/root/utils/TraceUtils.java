package pl.scisoftware.root.utils;

import java.util.logging.Logger;

import com.ibm.websphere.wim.ras.WIMLogger;

public class TraceUtils {

	private TraceUtils() {
	}

	public static final String TRACE_CATEGORY_PREFIX = "scisoftware.root.";

	public static Logger createTraceLogger(Class<?> clazz) {
		return WIMLogger.getTraceLogger(TRACE_CATEGORY_PREFIX + clazz.getSimpleName());
	}

	public static Logger createTraceLogger(String categoryName) {
		return WIMLogger.getTraceLogger(TRACE_CATEGORY_PREFIX + categoryName);
	}
	
	public static String createDisplayClazzName(Class<?> clazz) {
		String[] packageElements = clazz.getPackage().getName().split("\\.");
		StringBuilder sb = new StringBuilder();
		for (String element : packageElements) {
			sb.append(element.charAt(0)).append('.');
		}
		return sb.toString() + clazz.getSimpleName();
	}

}
