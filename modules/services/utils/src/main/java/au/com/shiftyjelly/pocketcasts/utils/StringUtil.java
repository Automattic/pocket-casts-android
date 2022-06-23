package au.com.shiftyjelly.pocketcasts.utils;

import android.text.Html;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtil {
	
	public final static boolean isBlank(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	public final static boolean isPresent(String str) {
		return str != null && str.trim().length() > 0;
	}

	public static String join(CharSequence delimiter, Object[] tokens) {
		StringBuilder sb = new StringBuilder();
		boolean firstTime = true;
		for (Object token: tokens) {
			if (firstTime) {
				firstTime = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(token);
		}
		return sb.toString();
	}
	
	public static String md5(String s) {
	    try {
	    	MessageDigest digest = MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes(),0,s.length());
	        String hash = new BigInteger(1, digest.digest()).toString(16);
	        return hash;
	    } 
	    catch (NoSuchAlgorithmException e) {}
	    
	    return null;
	}
}
