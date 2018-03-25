package com.deploy.util;

import java.io.File;

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
	public static void startUp(final File tomcatDir) throws Exception {
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec("cmd /c startup.bat", null, new File(tomcatDir.getPath() + "/bin"));
		if (process.waitFor() > -1) {
			System.out.println("Tomcat shutdown.bat文件执行完成！");
		}
	}

	/**
	 * 关闭Tomcat
	 * 
	 * @return
	 */
	public static void close(File tomcatDir) throws Exception {
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec("cmd /c shutdown.bat", null, new File(tomcatDir.getPath() + "/bin"));
		if (process.waitFor() > -1) {
			System.out.println("Tomcat shutdown.bat文件执行完成！");
		}
	}
}
