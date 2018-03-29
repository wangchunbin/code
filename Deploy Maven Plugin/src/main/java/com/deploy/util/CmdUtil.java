package com.deploy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Windows CMD命令执行类
 * 
 * @author WangChunBin
 *
 */
public class CmdUtil {
	/**
	 * 在指定路径下，执行指定命令
	 * 
	 * @param path
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> execCMD(String path, String command) throws Exception {
		File dir=new File(path);
		if(!dir.isDirectory()){
			throw new Exception(path+"不是一个文件夹目录！");
		}
		Map<String, String> processInfo = new HashMap<String, String>();
		CountDownLatch countDownLatch = new CountDownLatch(2);
		Runtime runtime = Runtime.getRuntime();
		// cmd /c dir 是执行完dir命令后封闭命令窗口。
		// cmd /k dir 是执行完dir命令后不封闭命令窗口。
		// cmd /c start dir 会打开一个新窗口后执行dir指令，原窗口会封闭。
		// cmd /k start dir 会打开一个新窗口后执行dir指令，原窗口不会封闭。
		// windows中调用cmd命令，多条命令执行中间要用“&&”连接,分号是Linux下的分隔方式
		Process process = runtime.exec("cmd /c " + command , null, dir);
		InputStream inputStream = process.getInputStream();
		InputStream errorStream = process.getErrorStream();
		Thread inputThread = new ProcessMonitorThread("InputInfo", inputStream, countDownLatch, processInfo);
		Thread errorThread = new ProcessMonitorThread("ErrorInfo", errorStream, countDownLatch, processInfo);
		inputThread.start();
		errorThread.start();
		countDownLatch.await();
		return processInfo;
	}

	/**
	 * 监控进程运行线程类
	 * 
	 * @author WangChunBin
	 *
	 */
	private static class ProcessMonitorThread extends Thread {

		private String threadName;

		private InputStream inputStream;

		private CountDownLatch countDownLatch;

		private Map<String, String> processInfo;

		public ProcessMonitorThread(String threadName, InputStream inputStream, CountDownLatch countDownLatch,
				Map<String, String> processInfo) {
			this.threadName = threadName;
			this.inputStream = inputStream;
			this.countDownLatch = countDownLatch;
			this.processInfo = processInfo;
		}

		@Override
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream,"gbk"));
				StringBuffer sb = new StringBuffer();
				String line = null;
				int lineCount = 0;
				while ((line = br.readLine()) != null) {
					if (lineCount > 0) {
						sb.append("\r\n" + line);
					} else {
						sb.append(line);
					}
					lineCount++;
				}
				br.close();
				String info = sb.toString();
				if (info != null && !"".equals(info)) {
					processInfo.put(threadName, info);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				countDownLatch.countDown();
			}
		}
	}
}
