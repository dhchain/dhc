package org.dhc.util;

import java.util.Formatter;

public class StringUtil {

	public static String toHexString(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		String hex = formatter.toString();
		formatter.close();
		return hex;
	}

	public static byte[] fromHexString(String s) {
		int length = s.length() / 2;
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) ((Character.digit(s.charAt(i * 2), 16) << 4) | Character.digit(s.charAt((i * 2) + 1), 16));
		}
		return bytes;
	}

	public static String trim(String str) {
		if (str == null) {
			return null;
		}
		return str.trim();
	}

	public static String substring(String str, int start, int end) {
		if (str == null) {
			return null;
		} else {
			if (end < 0) {
				end += str.length();
			}

			if (start < 0) {
				start += str.length();
			}

			if (end > str.length()) {
				end = str.length();
			}

			if (start > end) {
				return "";
			} else {
				if (start < 0) {
					start = 0;
				}

				if (end < 0) {
					end = 0;
				}

				return str.substring(start, end);
			}
		}
	}
	
	public static boolean equals(String str1, String str2) {
		if(str1 != null) {
			return str1.equals(str2);
		}
		return str2 == null;
	}
	
	public static String trimToNull(String str) {
		String result = str;
		if(result == null) {
			return null;
		}
		result = result.trim();
		result = "".equals(result)? null: result;
		return result;
	}
	
	public static String truncate(String str, int limit) {
		String result = str;
		if(result == null) {
			return null;
		}
		if (result.length() > limit) {
			result = result.substring(0, limit);
		}
		return result;
	}

	public static boolean isEmpty(String text) {
		return trimToNull(text) == null;
	}
	
	public static String createInClause(int length) {
	    StringBuilder queryBuilder = new StringBuilder(" (");
	    for (int i = 0; i < length; i++) {
	        queryBuilder.append(" ?");
	        if (i != length - 1) {
	            queryBuilder.append(",");
	        }
	    }
	    queryBuilder.append(")");
	    return queryBuilder.toString();
	}

}
