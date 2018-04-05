package com.deploy.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
	/**
	 * 版本号
	 * 
	 * @return
	 */
	String versionNumber();

	/**
	 * 版本信息
	 * 
	 * @return
	 */
	String information();
}
