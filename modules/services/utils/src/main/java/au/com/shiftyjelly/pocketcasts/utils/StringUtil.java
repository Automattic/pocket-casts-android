package au.com.shiftyjelly.pocketcasts.utils;

public class StringUtil {
	
	public final static boolean isBlank(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	public final static boolean isPresent(String str) {
		return str != null && str.trim().length() > 0;
	}

}
