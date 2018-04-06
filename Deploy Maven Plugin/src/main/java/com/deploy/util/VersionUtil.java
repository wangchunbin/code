package com.deploy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import org.apache.ibatis.session.SqlSession;
import com.deploy.dao.FileVersionInfo;
import com.deploy.dao.FileVersionInfoMapper;
import javassist.ClassPool;
import javassist.CtClass;

/**
 * 版本处理工具类
 * 
 * @author wangchunbin
 *
 */
public class VersionUtil {
	/**
	 * 获取pom.xml文件中项目版本号
	 * 
	 * @param pom
	 * @return
	 * @throws Exception
	 */
	public static String getProjectVersionByPOM(File pom) throws Exception {
		if (pom == null || !pom.exists() || !pom.isFile()) {// 判空
			throw new Exception("pom文件不存在或不是一个文件！");
		}
		FileInputStream fis = new FileInputStream(pom);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains("<version>") && line.contains("</version>")) {// 截取
				return line.substring(line.indexOf("<version>") + "<version>".length(), line.indexOf("</version>"));
			}
		}
		br.close();
		fis.close();
		return null;
	}

	/**
	 * 增量写入版本信息
	 * 
	 * @param sourceDir
	 * @param checkInfo
	 * @param diffInfo
	 * @throws Exception
	 */
	public static void incrementalWriteVersion(File sourceDir, String projectName, String newProjectVersion, Map<String, String> gitCommitFileVersionInfo, Map<String, String> diffInfo) throws Exception {
		if (!FileUtil.isNullDir(sourceDir) && diffInfo != null && diffInfo.size() > 0) {// 判空
			List<File> fileList = new ArrayList<File>();// 保存待处理的增量文件
			for (Entry<String, String> entry : diffInfo.entrySet()) {// 转换文件路径
				if (!entry.getValue().contains("DELETE")) {
					String filePath = entry.getKey();
					if (filePath.contains("/src")) {
						String path = sourceDir.getPath() + "/" + filePath.substring(filePath.lastIndexOf("/src") + 1);
						File file = new File(path);
						if (file.getName().endsWith(".x") || file.getName().endsWith(".java")) {
							fileList.add(file);
						}
					}
					if (filePath.contains("/WebContent")) {
						String path = sourceDir.getPath() + "/" + filePath.substring(filePath.lastIndexOf("/WebContent") + 1);
						File file = new File(path);
						if (file.getName().endsWith(".x") || file.getName().endsWith(".java")) {
							fileList.add(file);
						}
					}
				}
			}
			if (fileList != null && fileList.size() > 0) {
				for (File file : fileList) {
					String shortName = null;// 文件短名称(包名+文件名(不包含文件后缀))
					String suffix=null;
					if (file.getName().endsWith(".x")) {
						String path = file.getPath();
						if (path.contains("WebContent")) {// 切割出文件短名称
							shortName = path.substring(path.indexOf("WebContent") + "WebContent".length() + 1, path.lastIndexOf("."));
							suffix=".x";
						}
					} else if (file.getName().endsWith(".java")) {
						String path = file.getPath();
						if (path.contains("src")) {// 切割出文件短名称
							shortName = path.substring(path.indexOf("src") + "src".length() + 1, path.lastIndexOf("."));
							suffix=".java";
						}
					} else {
						continue;
					}
					Map<String,String> param = new HashMap<String, String>();
					param.put("shortName", shortName + (suffix.equals(".java")?".class":suffix));
					FileVersionInfo oldFvi  = SqliteUtil.findFileVersionInfoByFileName(param);// 查询Sqlite数据库中对应文件版本信息
					if (oldFvi == null) {
						boolean flag = true;
						if (gitCommitFileVersionInfo != null && gitCommitFileVersionInfo.size() > 0) {
							for (String key : gitCommitFileVersionInfo.keySet()) {
								if (key.contains(shortName.replace("\\", "/") + suffix)) {// 判断提交备注信息中是否包含该文件，如果包含则将备注信息写入到版本信息中
									writeVersion(file, newProjectVersion, gitCommitFileVersionInfo.get(key));
									flag = false;
								}
							}
						}
						if(flag){
							writeVersion(file, newProjectVersion, "");
						}
					}else{
						boolean flag = true;
						if (gitCommitFileVersionInfo != null && gitCommitFileVersionInfo.size() > 0) {
							for (String key : gitCommitFileVersionInfo.keySet()) {
								if (key.contains(shortName.replace("\\", "/") + suffix)) {// 判断提交备注信息中是否包含该文件，如果包含则将备注信息写入到版本信息中
									writeVersion(file, newProjectVersion, gitCommitFileVersionInfo.get(key));
									flag = false;
								}
							}
						}
						if (flag) {
							writeVersion(file, newProjectVersion, "");
						}
					}
				}
			}
		}
	}

	/**
	 * 批量写入初始版本信息
	 * 
	 * @param sourceDir
	 * @throws Exception
	 */
	public static void batchWriteInitVersion(File sourceDir, String newProjectVersion) throws Exception {
		if (sourceDir != null && sourceDir.exists() && sourceDir.isDirectory()) {// 判空
			List<File> tempFileList = new ArrayList<File>();
			FileUtil.listFiles(sourceDir, tempFileList); // 递归获取文件夹下文件
			List<File> fileList = new ArrayList<File>();
			if (tempFileList != null && tempFileList.size() > 0) {// 过滤出.x、java文件
				for (File file : tempFileList) {
					if (file.getName().endsWith(".x") || file.getName().endsWith(".java")) {
						fileList.add(file);
					}
				}
			}
			if (fileList != null && fileList.size() > 0) {// 开启线程，执行版本信息写入操作
				int threadCount = 8;// 线程数
				int size = fileList.size();
				threadCount = Math.min(threadCount, size);
				int fileCount = size / threadCount;// 线程均分待处理文件数
				int addCount = size % threadCount;// 第一个线程需增加的待处理文件数
				CountDownLatch cdl = new CountDownLatch(threadCount);
				int cursor = 0;
				for (int i = 0; i < threadCount; i++) {
					List<File> subList = null;
					if (i == 0) {
						cursor = fileCount + addCount;
						subList = fileList.subList(0, cursor);
					} else {
						subList = fileList.subList(cursor, cursor += fileCount);
					}
					new WriteInitVersionThread(subList, cdl, newProjectVersion).start();// 启动任务线程
				}
				cdl.await();// 等待任务线程执行完
			}
		}
	}

	/**
	 * 写入初始版本信息线程体
	 * 
	 * @author WangChunBin
	 *
	 */
	public static class WriteInitVersionThread extends Thread {
		private List<File> fileList;

		private CountDownLatch countDownLatch;
		
		private String newProjectVersion;

		public WriteInitVersionThread(List<File> fileList, CountDownLatch countDownLatch, String newProjectVersion) {
			this.fileList = fileList;
			this.countDownLatch = countDownLatch;
			this.newProjectVersion =  newProjectVersion;
		}

		@Override
		public void run() {
			try {
				if (fileList != null && fileList.size() > 0) {
					for (File file : fileList) {
						writeVersion(file, newProjectVersion, "初始版本！"); // 写入初始版本信息
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				countDownLatch.countDown();
			}
		}
	}

	/**
	 * 往java源文件或者.x文件写入版本号
	 * 
	 * @param sourceFile
	 * @param versionNumber
	 * @param information
	 * @throws Exception
	 */
	public static void writeVersion(File sourceFile, String versionNumber, String information) throws Exception {
		if(sourceFile != null && sourceFile.exists()){
			if (versionNumber == null || information == null) {// 判空
				throw new Exception("传入的版本信息不能为null！");
			}
			FileInputStream fis = new FileInputStream(sourceFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, "GBK"));
			StringBuffer sb = new StringBuffer();
			String line = null;
			String suffix = sourceFile.getName().substring(sourceFile.getName().lastIndexOf(".") + 1);
			if ("java".equals(suffix)) {// 写入Version Annotation
				String javaName = sourceFile.getName().substring(0, sourceFile.getName().indexOf(".java"));
				int lineCount = 0;
				while ((line = br.readLine()) != null) {
					if (line.matches("^\\s*public\\s+class\\s+" + javaName + ".*")) {
						line = line.replaceFirst("^\\s*public\\s+class\\s+", "@com.das.version.Version(versionNumber=\""
								+ versionNumber + "\",information=\"" + information + "\")\r\npublic class ");
					}
					if (line.matches("^\\s*class\\s+" + javaName + ".*")) {
						line = line.replaceFirst("^\\s*class\\s+", "@com.das.version.Version(versionNumber=\"" + versionNumber
								+ "\",information=\"" + information + "\")\r\nclass ");
					}
					if (line.matches("^\\s*public\\s+abstract\\s+class\\s+" + javaName + ".*")) {
						line = line.replaceFirst("^\\s*public\\s+abstract\\s+class\\s+",
								"@com.das.version.Version(versionNumber=\"" + versionNumber + "\",information=\"" + information
										+ "\")\r\npublic abstract class ");
					}
					if (line.matches("^\\s*public\\s+final\\s+class\\s+" + javaName + ".*")) {
						line = line.replaceFirst("^\\s*public\\s+final\\s+class\\s+",
								"@com.das.version.Version(versionNumber=\"" + versionNumber + "\",information=\"" + information
										+ "\")\r\npublic final class ");
					}
					if (line.matches("^\\s*public\\s+interface\\s+" + javaName + ".*")) {
						line = line.replaceFirst("^\\s*public\\s+interface\\s+", "@com.das.version.Version(versionNumber=\""
								+ versionNumber + "\",information=\"" + information + "\")\r\npublic interface ");
					}
					if (line.matches("^\\s*public\\s+@interface\\s+" + javaName + ".*")) {
						line = line.replaceFirst("^\\s*public\\s+@interface\\s+", "@com.das.version.Version(versionNumber=\""
								+ versionNumber + "\",information=\"" + information + "\")\r\npublic @interface ");
					}
					if (lineCount > 0) {
						line = "\r\n" + line;
					}
					sb.append(line);
					lineCount++;
				}
			} else if ("x".equals(suffix)) {// 写入版本信息行
				sb.append("#versionNumber=" + versionNumber + "\r\n");
				sb.append("#information=" + information.replace("\r\n", " "));
				while ((line = br.readLine()) != null) {
					sb.append("\r\n" + line);
				}
			}
			br.close();
			fis.close();
			String content = sb.toString();
			/*File dir=sourceFile.getParentFile();
			if(!dir.exists()){
				dir.mkdirs();
			}*/
			FileOutputStream fos = new FileOutputStream(sourceFile);
			fos.write(content.getBytes("GBK"));
			fos.flush();
			fos.close();
		}
	}

    /**
     * 线程本地变量	
     */
	private static final ThreadLocal<ClassPool> threadLocal = new ThreadLocal<ClassPool>();
	
	/**
	 * 获取线程类池
	 * 
	 * @return
	 */
	private static ClassPool getClassPool(String classPath,String libPath){
		ClassPool pool = (ClassPool) threadLocal.get();
		try{
			if(pool == null){
				URL[] libJarURLs = FileUtil.getTomcatProjectLibJarURL(libPath);
				URL classPathURL = new File(classPath).toURI().toURL();
				pool = ClassPool.getDefault();
				for(URL url : libJarURLs){
					pool.appendClassPath(url.getFile());
				}
				pool.appendClassPath(classPathURL.getFile());
				pool.appendSystemPath();
				threadLocal.set(pool);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return pool;
	}
	
	/**
	 * 获取tomcat项目中class或者.x文件版本信息
	 * 
	 * @param file
	 * @return
	 */
	public static Map<String, String> getVersionInfo(File tomcatProjectfile) throws Exception {
		if (tomcatProjectfile != null && tomcatProjectfile.exists()
				&& (tomcatProjectfile.getName().endsWith(".class") || tomcatProjectfile.getName().endsWith(".x"))) {// 判空
			Map<String, String> versionInfo = new HashMap<String, String>();
			if (tomcatProjectfile.getName().endsWith(".class")) {
				String path = tomcatProjectfile.getPath();
				String className = path.substring(path.indexOf("classes\\") + "classes\\".length(), path.lastIndexOf(".")).replace("\\", ".");// 切割出类名
				// 获取项目类加载URL
				String classPath = path.substring(0, path.indexOf("classes") + "classes".length());
				String libPath = path.substring(0, path.indexOf("WEB-INF") + "WEB-INF".length()) + "\\lib";
				ClassPool pool = getClassPool(classPath, libPath);
				try{
					if(pool != null){
						CtClass cls=pool.get(className);// 加载类
						if (cls != null) {
							Object obj= cls.getAnnotation(Version.class);// 获取版本Annotation
							if (obj != null) {// 获取版本信息
								Version version = (Version) obj;
								versionInfo.put("versionNumber", version.versionNumber());
								versionInfo.put("information", version.information());
							}
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			} else if (tomcatProjectfile.getName().endsWith(".x")) {// 使用IO流获取.x文件版本信息
				FileInputStream fis = new FileInputStream(tomcatProjectfile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis, "GBK"));
				String line = null;
				int lineCount = 0;
				while ((line = br.readLine()) != null) {
					if (lineCount == 0) {
						if (line.contains("#versionNumber=")) {
							versionInfo.put("versionNumber", line.replace("#versionNumber=", "").trim());
						}
					} else if (lineCount == 1) {
						if (line.contains("#information=")) {
							versionInfo.put("information", line.replace("#information=", "").trim());
						}
					} else {
						break;
					}
					lineCount++;
				}
				br.close();
				fis.close();
			}
			return versionInfo;
		} else {
			return null;
		}
	}

	/**
	 * 获取tomcat项目下文件修改信息
	 * 
	 * @param tomcatProjectDir
	 * @return
	 * @throws IOException
	 */
	public static Map<FileVersionInfo, String> getTomcatFileModifyInfo(File tomcatProjectDir) throws Exception {
		List<FileVersionInfo> fileVersionInfoList = SqliteUtil.getAllFileVersionInfo();
		if (fileVersionInfoList != null && fileVersionInfoList.size() > 0) {
			Map<String, FileVersionInfo> fileVersionInfoMap = new HashMap<String, FileVersionInfo>();
			for (FileVersionInfo fvi : fileVersionInfoList) {
				fileVersionInfoMap.put(fvi.getFile(), fvi);// 放入HashMap方便查找
			}
			List<FileVersionInfo> fviList = getTomcatFileVersionInfo(tomcatProjectDir);
			Map<FileVersionInfo, String> modifyInfo = new HashMap<FileVersionInfo, String>();
			if (fviList != null && fviList.size() > 0) {
				for (FileVersionInfo tomcatFvi : fviList) {
					FileVersionInfo file = fileVersionInfoMap.get(tomcatFvi.getFile());
					if (file == null) {
						modifyInfo.put(tomcatFvi, "新增");
					} else {
						StringBuffer sb = new StringBuffer();
						String versionNumber1 = tomcatFvi.getVersionNumber();
						String versionNumber2 = file.getVersionNumber();
						if (versionNumber1 != null) {
							if (!versionNumber1.equals(versionNumber2)) {
								sb.append("文件版本有变化，上次记录:" + file.getVersionNumber() + ",当前tomcat该文件:" + tomcatFvi.getVersionNumber() + "。");
							}
						} else if (versionNumber2 != null) {
							sb.append("文件版本有变化，上次记录:" + file.getVersionNumber() + ",当前tomcat该文件:" + tomcatFvi.getVersionNumber() + "。");
						}
						String information1 = tomcatFvi.getInformation();
						String information2 = file.getInformation();
						if (!StringUtil.isBlank(information1)) {
							if (!information1.equals(information2)) {
								sb.append("文件版本说明有变化，上次记录:" + file.getInformation() + ",当前:" + tomcatFvi.getInformation() + "。");
							}
						} else if (!StringUtil.isBlank(information2)) {
							sb.append("文件版本说明有变化，上次记录:" + file.getInformation() + ",当前:" + tomcatFvi.getInformation() + "。");
						}
						/*if (!tomcatFvi.getFileSize().equals(file.getFileSize())) {
							sb.append("文件大小有变化，上次记录:" + file.getFileSize() + ",当前tomcat该文件:" + tomcatFvi.getFileSize() + "。");
						}
						if (!tomcatFvi.getLastModifyTime().equals(file.getLastModifyTime())) {
							sb.append("文件修改时间有变化，上次记录:" + file.getLastModifyTime() + ",当前tomcat该文件:" + tomcatFvi.getLastModifyTime() + "。");
						}*/
						if (!StringUtil.isBlank(sb.toString())) {
							tomcatFvi.setDeployId(file.getDeployId());// 添加旧DeployId，以便更新
							modifyInfo.put(tomcatFvi, sb.toString());
						}
						fileVersionInfoMap.remove(file.getFile());// 删除已比对完的键值对,剩下的就是被删除掉的
					}
				}
				if (fileVersionInfoMap.size() > 0) {
					for (Entry<String, FileVersionInfo> entry : fileVersionInfoMap.entrySet()) {
						modifyInfo.put(entry.getValue(), "删除");
					}
				}
				return modifyInfo;
			}
		}
		return null;
	}

	/**
	 * 获取tomcat项目下文件版本信息
	 * 
	 * @param tomcatProjectDir
	 * @return
	 * @throws Exception
	 */
	public static List<FileVersionInfo> getTomcatFileVersionInfo(File tomcatProjectDir) throws Exception {
		List<File> fileList = new ArrayList<File>();
		FileUtil.listFiles(tomcatProjectDir, fileList);
		if (fileList != null && fileList.size() > 0) {
			List<FileVersionInfo> vector = new Vector<FileVersionInfo>();
			int threadCount = 8;
			int size = fileList.size();
			threadCount = Math.min(threadCount, size);
			int fileCount = size / threadCount;
			int addCount = size % threadCount;
			CountDownLatch cdl = new CountDownLatch(threadCount);
			int cursor = 0;
			for (int i = 0; i < threadCount; i++) {
				List<File> subList = null;
				if (i == 0) {
					cursor = fileCount + addCount;
					subList = fileList.subList(0, cursor);
				} else {
					subList = fileList.subList(cursor, cursor += fileCount);
				}
				new GetFileVersionInfoThread(subList, vector, cdl).start();
			}
			cdl.await();
			return vector;
		}
		return null;
	}

	/**
	 * 获取文件版本信息线程体
	 * 
	 * @author wangchunbin
	 *
	 */
	public static class GetFileVersionInfoThread extends Thread {
		private List<File> fileList;

		private List<FileVersionInfo> vector;

		private CountDownLatch countDownLatch;

		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		private Calendar cal = Calendar.getInstance();

		public GetFileVersionInfoThread(List<File> fileList, List<FileVersionInfo> vector,
				CountDownLatch countDownLatch) {
			this.fileList = fileList;
			this.vector = vector;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() {
			try {
				if (fileList != null && vector != null && countDownLatch != null) {
					for (File file : fileList) {
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setFile(file.getPath());
						fvi.setFileSize(Long.valueOf(file.length()).intValue());
						cal.setTimeInMillis(file.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						Map<String, String> versionInfo = VersionUtil.getVersionInfo(file);
						if (versionInfo != null && versionInfo.size() > 0) {
							fvi.setVersionNumber(versionInfo.get("versionNumber") == null ? null : versionInfo.get("versionNumber"));
							fvi.setInformation(versionInfo.get("information") == null ? null : versionInfo.get("information"));
						}
						vector.add(fvi);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				countDownLatch.countDown();
			}
		}
	}
	
	/**
	 * 保存tomcat项目增量文件版本信息
	 * 
	 * @param deployId
	 * @param tomcatProjectDir
	 * @param checkInfo
	 * @param diffInfo
	 * @throws Exception
	 */
	public static void saveIncrementalTomcatFileVersionInfo(Integer deployId, File tomcatProjectDir, Map<FileVersionInfo, String> checkInfo, String projectAtGitRepositoryPath, Map<String, String> diffInfo , Map<File, String> jarDiffInfo) throws Exception {
		if (checkInfo != null && checkInfo.size() > 0) {
			for (Entry<FileVersionInfo, String> entry : checkInfo.entrySet()) {
				FileVersionInfo newFvi = entry.getKey();
				// 1.备份
				FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(newFvi.getFile());
				SqliteUtil.insertFileVersionModifyBak(oldFvi);
				// 2.更新
				newFvi.setDeployId(deployId);
				SqliteUtil.updateFileVersionInfo(newFvi);
			}
		}
		if (diffInfo != null && diffInfo.size() > 0) {
			for (Map.Entry<String, String> info : diffInfo.entrySet()) {
				String filePath = info.getKey();
				if (filePath.contains("/WebContent")) {
					String file = filePath.replace(projectAtGitRepositoryPath + "/WebContent", "");
					File tomcatFile = new File(tomcatProjectDir + "/" + file);
					if (tomcatFile.exists()) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Calendar cal = Calendar.getInstance();
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setFile(tomcatFile.getPath());
						fvi.setFileSize(Long.valueOf(tomcatFile.length()).intValue());
						cal.setTimeInMillis(tomcatFile.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						Map<String, String> versionInfo = getVersionInfo(tomcatFile);
						if (versionInfo != null && versionInfo.size() > 0) {
							fvi.setVersionNumber(versionInfo.get("versionNumber") == null ? null : versionInfo.get("versionNumber"));
							fvi.setInformation(versionInfo.get("information") == null ? null : versionInfo.get("information"));
						}
						// 1.备份
						FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(fvi.getFile());
						SqliteUtil.insertFileVersionModifyBak(oldFvi);
						// 2.更新
						fvi.setDeployId(deployId);
						if(info.getValue().contains("ADD")){
							SqliteUtil.insertFileVersionInfo(fvi);
						}else if(info.getValue().contains("DELETE")){
							if(oldFvi != null){
								SqliteUtil.deleteFileVersionInfo(oldFvi.getFile());
							}
						}else{
							SqliteUtil.updateFileVersionInfo(fvi);
						}
					}else if(info.getValue().contains("DELETE")){
						// 1.备份
						FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(tomcatFile.getPath());
						SqliteUtil.insertFileVersionModifyBak(oldFvi);
						// 2.删除
						if(oldFvi != null){
							SqliteUtil.deleteFileVersionInfo(oldFvi.getFile());
						}
					}
				}
				if (filePath.contains("/src")) {
					String file = filePath.replace(projectAtGitRepositoryPath + "/src", "").replace(".java", ".class");
					File tomcatFile = new File(tomcatProjectDir + "/WEB-INF/classes/" + file);
					if (tomcatFile.exists()) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Calendar cal = Calendar.getInstance();
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setFile(tomcatFile.getPath());
						fvi.setFileSize(Long.valueOf(file.length()).intValue());
						cal.setTimeInMillis(tomcatFile.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						Map<String, String> versionInfo = getVersionInfo(tomcatFile);
						if (versionInfo != null && versionInfo.size() > 0) {
							fvi.setVersionNumber(versionInfo.get("versionNumber") == null ? null : versionInfo.get("versionNumber"));
							fvi.setInformation(versionInfo.get("information") == null ? null : versionInfo.get("information"));
						}
						// 1.备份
						FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(fvi.getFile());
						SqliteUtil.insertFileVersionModifyBak(oldFvi);
						// 2.更新
						fvi.setDeployId(deployId);
						if(info.getValue().contains("ADD")){
							SqliteUtil.insertFileVersionInfo(fvi);
						}else if(info.getValue().contains("DELETE")){
							if(oldFvi != null){
								SqliteUtil.deleteFileVersionInfo(oldFvi.getFile());
							}
						}else{
							SqliteUtil.updateFileVersionInfo(fvi);
						}
					}else if(info.getValue().contains("DELETE")){
						// 1.备份
						FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(tomcatFile.getPath());
						SqliteUtil.insertFileVersionModifyBak(oldFvi);
						// 2.删除
						if(oldFvi != null){
							SqliteUtil.deleteFileVersionInfo(oldFvi.getFile());
						}
					}
				}
			}
		}
		if(jarDiffInfo !=null && jarDiffInfo.size() >0){
			for (Entry<File, String> entry : jarDiffInfo.entrySet()) {
				if(entry.getValue().contains("ADD")){
					File tomcatFile = new File(tomcatProjectDir + "/WEB-INF/lib/" + entry.getKey().getName());
					if (tomcatFile.exists()) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Calendar cal = Calendar.getInstance();
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setFile(tomcatFile.getPath());
						fvi.setFileSize(Long.valueOf(tomcatFile.length()).intValue());
						cal.setTimeInMillis(tomcatFile.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						fvi.setDeployId(deployId);
						SqliteUtil.insertFileVersionInfo(fvi);
					}
				}
				if(entry.getValue().contains("MODIFY")){
					File tomcatFile = new File(tomcatProjectDir + "/WEB-INF/lib/" + entry.getKey().getName());
					if (tomcatFile.exists()) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Calendar cal = Calendar.getInstance();
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setFile(tomcatFile.getPath());
						fvi.setFileSize(Long.valueOf(tomcatFile.length()).intValue());
						cal.setTimeInMillis(tomcatFile.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						// 1.备份
						FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(fvi.getFile());
						SqliteUtil.insertFileVersionModifyBak(oldFvi);
						// 2.更新
						fvi.setDeployId(deployId);
						SqliteUtil.updateFileVersionInfo(fvi);
					}
				}
				if(entry.getValue().contains("DELETE")){
					// 1.备份
					FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(entry.getKey().getPath());
					SqliteUtil.insertFileVersionModifyBak(oldFvi);
					// 2.删除
					if(oldFvi != null){
						SqliteUtil.deleteFileVersionInfo(oldFvi.getFile());
					}
				}
			}
		}
		File projectVersionFile=new File(tomcatProjectDir.getPath() + "/" + "version.txt");
		if(projectVersionFile != null && projectVersionFile.exists()){//更新项目版本文件版本信息
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar cal = Calendar.getInstance();
			FileVersionInfo fvi = new FileVersionInfo();
			fvi.setFile(projectVersionFile.getPath());
			fvi.setFileSize(Long.valueOf(projectVersionFile.length()).intValue());
			cal.setTimeInMillis(projectVersionFile.lastModified());
			fvi.setLastModifyTime(sdf.format(cal.getTime()));
			// 1.备份
			FileVersionInfo oldFvi = SqliteUtil.getFileVersinInfo(fvi.getFile());
			SqliteUtil.insertFileVersionModifyBak(oldFvi);
			// 2.更新
			fvi.setDeployId(deployId);
			SqliteUtil.updateFileVersionInfo(fvi);
		}
	}

	/**
	 * 保存tomcat项目全部文件版本信息
	 * 
	 * @param deployId
	 * @param tomcatProjectDir
	 * @throws Exception
	 */
	public static void saveAllTomcatFileVersionInfo(Integer deployId, File tomcatProjectDir) throws Exception {
		List<File> fileList = new ArrayList<File>();
		FileUtil.listFiles(tomcatProjectDir, fileList);
		if (fileList != null && fileList.size() > 0) {
			List<FileVersionInfo> fviList = SqliteUtil.getAllFileVersionInfo();
			List<List<FileVersionInfo>> lists = new ArrayList<List<FileVersionInfo>>();
			SqliteUtil.saveFileVersionModifyBak(fviList);
			SqliteUtil.deleteAllFileVersionInfo();
			int threadCount = 8;
			int size = fileList.size();
			threadCount = Math.min(threadCount, size);
			int fileCount = size / threadCount;
			int addCount = size % threadCount;
			CountDownLatch cdl = new CountDownLatch(threadCount);
			int cursor = 0;
			for (int i = 0; i < threadCount; i++) {
				List<FileVersionInfo> list = new ArrayList<FileVersionInfo>();
				lists.add(list);
				List<File> subList = null;
				if (i == 0) {
					cursor = fileCount + addCount;
					subList = fileList.subList(0, cursor);
				} else {
					subList = fileList.subList(cursor, cursor += fileCount);
				}
				new SaveFileVersionInfoThread(deployId, subList, list, cdl).start();
			}
			cdl.await();
			if (lists != null && lists.size() > 0) {
				SqlSession sqlSession = SqliteUtil.getSqlSessionFactory().openSession();
				FileVersionInfoMapper fvim = sqlSession.getMapper(FileVersionInfoMapper.class);
				for (List<FileVersionInfo> list : lists) {
					if (list != null && list.size() > 0) {
						for (FileVersionInfo fvi : list) {
							fvim.insert(fvi);
						}
						sqlSession.commit();
					}
				}
				sqlSession.close();
			}
		}
	}

	/**
	 * 保存文件版本信息线程体
	 * 
	 * @author wangchunbin
	 *
	 */
	public static class SaveFileVersionInfoThread extends Thread {
		private Integer deployId;

		private List<File> fileList;

		private List<FileVersionInfo> list;

		private CountDownLatch countDownLatch;

		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		private Calendar cal = Calendar.getInstance();

		public SaveFileVersionInfoThread(Integer deployId, List<File> fileList, List<FileVersionInfo> list, CountDownLatch countDownLatch) {
			this.deployId = deployId;
			this.fileList = fileList;
			this.list = list;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() {
			try {
				if (fileList != null && list != null && countDownLatch != null) {
					for (File file : fileList) {
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setDeployId(deployId);
						fvi.setFile(file.getPath());
						fvi.setFileSize(Long.valueOf(file.length()).intValue());
						cal.setTimeInMillis(file.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						Map<String, String> versionInfo = VersionUtil.getVersionInfo(file);
						if (versionInfo != null && versionInfo.size() > 0) {
							fvi.setVersionNumber(versionInfo.get("versionNumber") == null ? null : versionInfo.get("versionNumber"));
							fvi.setInformation(versionInfo.get("information") == null ? null : versionInfo.get("information"));
						}
						list.add(fvi);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				countDownLatch.countDown();
			}
		}
	}
}
