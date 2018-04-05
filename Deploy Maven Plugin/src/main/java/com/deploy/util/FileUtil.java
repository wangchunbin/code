package com.deploy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件操作工具类
 * 
 * @author WangChunBin
 *
 */
public class FileUtil {
	/**
	 * 递归获取目录下所有文件的File对象（不包含文件夹）
	 * 
	 * @param dir
	 * @return
	 */
	public static void listFiles(File dir, List<File> list) {
		if (dir != null && dir.isDirectory()&&list!=null) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File file : files) {
					if (file.isFile()) {
						list.add(file);
					} else if (file.isDirectory()) {
						listFiles(file, list);
					}
				}
			}
		}
	}

	/**
	 * 将指定文件复制到相应目录
	 * 
	 * @param file
	 * @param targetDir
	 * @throws Exception
	 */
	public static void copyFileToDir(File file, File targetDir) throws Exception {
		if (!file.exists()) {
			return;
		}
		String fileName = file.getName();
		String targetPath = targetDir.getPath() + "/" + fileName;
		FileInputStream in = new FileInputStream(file);
		FileOutputStream out = new FileOutputStream(targetPath);
		byte[] data = new byte[1024];
		int size = 0;
		while ((size = in.read(data)) != -1) {
			out.write(data, 0, size);
		}
		in.close();
		out.close();
	}

	/**
	 * 递归将文件夹内容复制到另外一个文件夹下
	 * 
	 * @param dir
	 * @param toDir
	 */
	public static void copyDirContentToDir(File dir, File toDir) throws Exception {
		if (dir != null && dir.exists() && toDir != null && toDir.exists()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File file : files) {
					if (file.isFile()) {
						copyFileToDir(file, toDir);
					} else if (file.isDirectory()) {
						String dirName = file.getName();
						String targetDirName = toDir.getPath() + "/" + dirName;
						new File(targetDirName).mkdirs();
						copyDirContentToDir(file, new File(targetDirName));
					}
				}
			}
		}
	}

	/**
	 * 替换文件
	 * 
	 * @param file
	 * @param toFile
	 * @throws Exception
	 */
	public static void replaceFile(File file, File toFile) throws Exception {
		File parentFile = toFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		if (toFile.exists()) {
			toFile.delete();
		}
		copyFileToDir(file, parentFile);
	}

	/**
	 * 判断文件夹是否是空目录
	 * 
	 * @param dir
	 * @return
	 * @throws Exception
	 */
	public static boolean isNullDir(File dir) {
		if (dir != null && dir.exists() && dir.list().length > 0) {
			return false;
		}
		return true;
	}

	/**
	 * 读取文件内容
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static String readFile(String path) throws Exception {
		File file = new File(path);
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
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
		fis.close();
		return sb.toString();
	}

	/**
	 * 创建一个新文件，并写入相关内容
	 * 
	 * @param path
	 * @param content
	 * @return
	 * @throws Exception
	 */
	public static boolean newFile(String path, String content) throws Exception {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		boolean result = file.createNewFile();
		if (result) {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(content.getBytes());
			fos.flush();
			fos.close();
			return true;
		}
		return false;
	}

	/**
	 * 删除文件夹或者文件
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static boolean deleteDirOrFile(String path) throws Exception {
		File file = new File(path);
		if (!file.isDirectory()) {
			file.delete();
		} else if (file.isDirectory()) {
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				File delFile = fileList[i];
				if (!delFile.isDirectory()) {
					delFile.delete();
				} else if (delFile.isDirectory()) {
					deleteDirOrFile(delFile.getPath());
				}
			}
			file.delete();
		}
		return true;
	}

	/**
	 * 用正则表达式匹配文件名，查找目录下文件(不进行递归查找)
	 * 
	 * @param dir
	 * @param regex
	 * @return
	 * @throws Exception
	 */
	public static List<File> findFile(File dir, String regex) throws Exception {
		if (dir == null || !dir.isDirectory() || StringUtil.isBlank(regex)) {
			return null;
		}
		List<File> fileList = new ArrayList<File>();
		Pattern compile = Pattern.compile(regex);
		File[] files = dir.listFiles();
		for (File file : files) {
			String fileName = file.getName();
			Matcher matcher = compile.matcher(fileName);
			if (matcher.find()) {
				fileList.add(file);
			}
		}
		return fileList;
	}

	/**
	 * 缓存
	 */
	private static URL[] libDirJarURLs = null;

	/**
	 * 返回tomcat项目lib目录下jar对应的URL
	 * 
	 * @param libDirPath
	 * @return
	 * @throws MalformedURLException
	 */
	public static URL[] getTomcatProjectLibJarURL(String libDirPath) throws MalformedURLException {
		if (libDirJarURLs == null || libDirJarURLs.length == 0) {
			File libDir = new File(libDirPath);
			URL[] urls = null;
			if (libDir != null && libDir.exists()) {
				File[] jars = libDir.listFiles();
				urls = new URL[jars.length];
				for (int i = 0; i < jars.length; i++) {
					urls[i] = jars[i].toURI().toURL();
				}
				libDirJarURLs = urls;
				return urls;
			} else {
				return null;
			}
		} else {
			return libDirJarURLs;// 直接返回缓存
		}
	}
	
	/**
	 * 获取jar变化
	 * 
	 * @param sourceLibDir
	 * @param tomcatLibDir
	 * @return
	 */
	public static Map<File,String> diffLibJar(File sourceLibDir, File tomcatLibDir){
		if(sourceLibDir != null && sourceLibDir.exists() && tomcatLibDir !=null && tomcatLibDir.exists()){
			File[] sourceJarFiles = sourceLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if(name.contains(".jar")){
						return true;
					}
					return false;
				}
			});
			File[] tomcatJarFiles = tomcatLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if(name.contains(".jar")){
						return true;
					}
					return false;
				}
			});
			Map<File,String> diffInfo = new HashMap<File,String>();
			Map<String,File> tomcatJarMap = new HashMap<String,File>(); 
			if(tomcatJarFiles != null && tomcatJarFiles.length > 0){
				for(File file : tomcatJarFiles){
					tomcatJarMap.put(file.getName(), file);
				}
			}
		    if(sourceJarFiles !=null && sourceJarFiles.length > 0){
		    	for(File file : sourceJarFiles){
		    		File tomcatJar = tomcatJarMap.get(file.getName());
		    		if(tomcatJar == null){
		    			diffInfo.put(file, "ADD");
		    		}else{
		    			if(file.length() != tomcatJar.length()){
		    				diffInfo.put(file, "MODIFY");
		    			}
		    			tomcatJarMap.remove(file.getName());
		    		}
		    	}
		    	if(tomcatJarMap != null && tomcatJarMap.size() >0){
		    		for(Entry<String,File> entry : tomcatJarMap.entrySet()){
		    			diffInfo.put(entry.getValue(), "DELETE");
		    		}
		    	}
		    	return diffInfo;
		    }
		}
	    return null;
	}
}
