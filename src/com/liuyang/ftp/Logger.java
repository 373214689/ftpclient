package com.liuyang.ftp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	
	public static boolean enableDebug = true;
	public static boolean enableInfo = true;
	public static boolean enableError = true;
	public static boolean enableWarn = true;

	public static Logger getLogger(Class<?> c) {
		return new Logger(c.getName());
	}
	
	private String className;
	
	private Logger(String name) {
		className = name;
	}
	
    /**
     * Get time of now with a formatter.
     * @return formatted by "yyyy-MM-dd HH:mm:ss.ms"
     */
    private static String now() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
        return String.format("%s.%03d", formatter.format(date), date.getTime() % 1000);
    }
    
	public void debug(Object m) {
		if (enableDebug) 
		    System.out.println(String.format("%s DEBUG %s %s", now(), className, m));
	}
	
	public void debug(String format, Object... m) {
		if (enableDebug) 
		    System.out.println(String.format("%s DEBUG %s %s", now(), className, String.format(format, m)));
	}
	
	public void error(Object m) {
		if (enableError) 
		    System.out.println(String.format("%s ERROR %s %s", now(), className, m));
	}
	
	public void info(Object m) {
		if (enableInfo) 
		    System.out.println(String.format("%s INFO %s %s", now(), className, m));
	}

	public void warn(Object m) {
		if (enableWarn) 
		    System.out.println(String.format("%s WARN %s %s", now(), className, m));
	}
}
