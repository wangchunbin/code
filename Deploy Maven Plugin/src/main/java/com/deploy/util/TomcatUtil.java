package com.deploy.util;

import java.io.File;
import java.net.Socket;
import org.apache.catalina.startup.Bootstrap;

/**
 * Tomcat工具类
 * 
 * @author WangChunBin
 *
 */
public class TomcatUtil {
	/**
	 * 启动Tomcat
	 * 
	 * @return
	 */
	@SuppressWarnings("resource")
	public static void startup(File tomcatDir, int port) throws Exception {
		/*
		 * System.setProperty("catalina.home", tomcatDir.getPath());
		 * System.setProperty("catalina.base", tomcatDir.getPath());
		 * Bootstrap.main(new String[] { "start" });
		 */
		boolean isRunning = true;
		try {
			new Socket("127.0.0.1", port);// 判断当前tomcat是否开启
		} catch (Exception e) {
			isRunning = false;
		}
		if (!isRunning) {
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec("cmd /c startup.bat", null, new File(tomcatDir.getPath() + "/bin"));
			if (process.waitFor() > -1) {
				System.out.println("∝╬══→Tomcat启动完成！❤❤");
			}
		}
	}

	/**
	 * 关闭Tomcat
	 * 
	 * @return
	 */
	@SuppressWarnings("resource")
	public static void shutdown(File tomcatDir, int port) throws Exception {
		boolean isRunning = true;
		try {
			new Socket("127.0.0.1", port);// 判断当前tomcat是否开启
		} catch (Exception e) {
			isRunning = false;
		}
		if (isRunning) {
			System.setProperty("catalina.home", tomcatDir.getPath());
			System.setProperty("catalina.base", tomcatDir.getPath());
			Bootstrap.main(new String[] { "stop" });
		}
	}
}
