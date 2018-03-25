package com.deploy.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Version {
	/**
	 * 版本号
	 * 
	 * @return
	 */
	int versionNumber();

	/**
	 * 版本说明
	 * 
	 * @return
	 */
	String information();
}
