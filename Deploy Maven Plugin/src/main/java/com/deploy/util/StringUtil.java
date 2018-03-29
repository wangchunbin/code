package com.deploy.util;

/**
 * 字符串工具类
 * 
 * @author wangchunbin
 *
 */
public class StringUtil {
	/**
	 * 是否是空白字符串
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isBlank(String str) {
		if (str == null || "".equals(str)) {
			return true;
		}
		return false;
	}
}
