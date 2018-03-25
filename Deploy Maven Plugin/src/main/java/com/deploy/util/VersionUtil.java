package com.deploy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
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
		if (pom == null || !pom.exists() || !pom.isFile()) {
			throw new Exception("pom文件不存在或不是一个文件！");
		}
		FileInputStream fis = new FileInputStream(pom);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains("<version>") && line.contains("</version>")) {
				return line.substring(line.indexOf("<version>") + "<version>".length(), line.indexOf("</version>"));
			}
		}
		br.close();
		fis.close();
		return null;
	}

	/**
	 * 缓存
	 */
	private static Map<String, FileVersionInfo> ClassAndXfileVersionInfoMap = null;

	/**
	 * 增量写入版本信息
	 * 
	 * @param sourceDir
	 * @param checkInfo
	 * @param diffInfo
	 * @throws Exception
	 */
	public static void incrementalWriteVersion(File sourceDir, String projectName, Map<String, String> diffInfo) throws Exception {
		if (sourceDir != null && sourceDir.exists() && sourceDir.isDirectory()) {
			File[] files = sourceDir.listFiles();
			if (files != null && files.length > 0) {
				if (ClassAndXfileVersionInfoMap == null) {
					List<FileVersionInfo> fileVersionInfoList = SqliteUtil.getAllFileVersionInfo();
					if (fileVersionInfoList != null && fileVersionInfoList.size() > 0) {
						ClassAndXfileVersionInfoMap = new HashMap<String, FileVersionInfo>();
						for (FileVersionInfo fvi : fileVersionInfoList) {
							if (fvi.getFile().contains(".x")) {
								String path = fvi.getFile();
								String shortName = path.substring(path.indexOf(projectName) + projectName.length() + 1, path.lastIndexOf("."));
								ClassAndXfileVersionInfoMap.put(shortName, fvi);// 放入HashMap方便查找
							}
							if (fvi.getFile().contains(".class")) {
								String path = fvi.getFile();
								String shortName = path.substring(path.indexOf("classes") + "classes".length() + 1, path.lastIndexOf("."));
								ClassAndXfileVersionInfoMap.put(shortName, fvi);// 放入HashMap方便查找
							}
						}
					}
				}
				for (File file : files) {
					if (file.isFile()) {
						String shortName = null;
						if (file.getName().contains(".x")) {
							String path = file.getPath();
							if (path.contains("WebContent")) {
								shortName = path.substring(path.indexOf("WebContent") + "WebContent".length() + 1, path.lastIndexOf("."));
							}
						} else if (file.getName().contains(".java")) {
							String path = file.getPath();
							if (path.contains("src")) {
								shortName = path.substring(path.indexOf("src") + "src".length() + 1, path.lastIndexOf("."));
							}
						} else {
							continue;
						}
						if (shortName == null) {
							writeVersion(file, 1, "初始版本！");
							continue;
						}
						FileVersionInfo fvi = null;
						if (ClassAndXfileVersionInfoMap != null) {
							fvi = ClassAndXfileVersionInfoMap.get(shortName);
						}
						if (fvi == null) {
							writeVersion(file, 1, "初始版本！");
							continue;
						}
						boolean isAt = false;
						if (diffInfo != null && diffInfo.size() > 0) {
							for (Entry<String, String> entry : diffInfo.entrySet()) {
								if (entry.getKey().contains(shortName)) {
									isAt = true;
									break;
								}
							}
						}
						if (isAt) {
							writeVersion(file, fvi.getVersionNumber() + 1, "第" + fvi.getVersionNumber() + 1 + "次修改！");
						}
					} else {
						incrementalWriteVersion(file, projectName, diffInfo);
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
	public static void batchWriteInitVersion(File sourceDir) throws Exception {
		if (sourceDir != null && sourceDir.exists() && sourceDir.isDirectory()) {
			File[] files = sourceDir.listFiles();
			if (files != null && files.length > 0) {
				for (File file : files) {
					if (file.isFile()) {
						if (file.getName().contains(".x") || file.getName().contains(".java")) {
							writeVersion(file, 1, "初始版本！");
						}
					} else {
						batchWriteInitVersion(file);
					}
				}
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
	public static void writeVersion(File sourceFile, Integer versionNumber, String information) throws Exception {
		if (sourceFile == null || versionNumber == null || information == null) {
			throw new Exception("传入的参数不能为null！");
		}
		FileInputStream fis = new FileInputStream(sourceFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis, "GBK"));
		StringBuffer sb = new StringBuffer();
		String line = null;
		String suffix = sourceFile.getName().substring(sourceFile.getName().lastIndexOf(".") + 1);
		if ("java".equals(suffix)) {
			String javaName = sourceFile.getName().substring(0, sourceFile.getName().indexOf(".java"));
			int lineCount = 0;
			while ((line = br.readLine()) != null) {
				if (line.matches("^\\s*public\\s+class\\s+" + javaName + ".*")) {
					line = line.replaceFirst("^\\s*public\\s+class\\s+", "@com.das.version.Version(versionNumber="
							+ versionNumber + ",information=\"" + information + "\")\r\npublic class ");
				}
				if (line.matches("^\\s*class\\s+" + javaName + ".*")) {
					line = line.replaceFirst("^\\s*class\\s+", "@com.das.version.Version(versionNumber=" + versionNumber
							+ ",information=\"" + information + "\")\r\nclass ");
				}
				if (line.matches("^\\s*public\\s+abstract\\s+class\\s+" + javaName + ".*")) {
					line = line.replaceFirst("^\\s*public\\s+abstract\\s+class\\s+",
							"@com.das.version.Version(versionNumber=" + versionNumber + ",information=\"" + information
									+ "\")\r\npublic abstract class ");
				}
				if (line.matches("^\\s*public\\s+final\\s+class\\s+" + javaName + ".*")) {
					line = line.replaceFirst("^\\s*public\\s+final\\s+class\\s+",
							"@com.das.version.Version(versionNumber=" + versionNumber + ",information=\"" + information
									+ "\")\r\npublic final class ");
				}
				if (line.matches("^\\s*public\\s+interface\\s+" + javaName + ".*")) {
					line = line.replaceFirst("^\\s*public\\s+interface\\s+", "@com.das.version.Version(versionNumber="
							+ versionNumber + ",information=\"" + information + "\")\r\npublic interface ");
				}
				if (line.matches("^\\s*public\\s+@interface\\s+" + javaName + ".*")) {
					line = line.replaceFirst("^\\s*public\\s+@interface\\s+", "@com.das.version.Version(versionNumber="
							+ versionNumber + ",information=\"" + information + "\")\r\npublic @interface ");
				}
				if (lineCount > 0) {
					line = "\r\n" + line;
				}
				sb.append(line);
				lineCount++;
			}
		} else if ("x".equals(suffix)) {
			sb.append("#versionNumber=" + versionNumber + "\r\n");
			sb.append("#information=" + information.replace("\r\n", " "));
			while ((line = br.readLine()) != null) {
				sb.append("\r\n" + line);
			}
		}
		br.close();
		fis.close();
		String content = sb.toString();
		FileOutputStream fos = new FileOutputStream(sourceFile);
		fos.write(content.getBytes("GBK"));
		fos.flush();
		fos.close();
	}

	/**
	 * 缓存
	 */
	private static URL[] classLoadUrls = null;

	/**
	 * 获取tomcat项目中class或者.x文件版本信息
	 * 
	 * @param file
	 * @return
	 */
	public static Map<String, Object> getVersionInfo(File tomcatProjectfile) throws Exception {
		if (tomcatProjectfile != null && tomcatProjectfile.exists() && (tomcatProjectfile.getName().contains(".class") || tomcatProjectfile.getName().contains(".x"))) {
			Map<String, Object> versionInfo = new HashMap<String, Object>();
			if (tomcatProjectfile.getName().contains(".class")) {
				String path = tomcatProjectfile.getPath();
				String className = path.substring(path.indexOf("classes\\") + "classes\\".length(), path.lastIndexOf(".")).replace("\\", ".");
				if (classLoadUrls == null) {
					String classPath = path.substring(0, path.indexOf("classes") + "classes".length());
					String libPath = path.substring(0, path.indexOf("WEB-INF") + "WEB-INF".length()) + "\\lib";
					URL[] libJarURLs = FileUtil.getTomcatProjectLibJarURL(libPath);
					URL classPathURL = new File(classPath).toURI().toURL();
					URL[] urls = new URL[libJarURLs.length + 1];
					if (libJarURLs != null) {
						for (int i = 0; i < libJarURLs.length; i++) {
							urls[i] = libJarURLs[i];
						}
					}
					urls[libJarURLs.length] = classPathURL;
					classLoadUrls = urls;
				}
				@SuppressWarnings("resource")
				URLClassLoader ucl = new URLClassLoader(classLoadUrls);
				@SuppressWarnings("rawtypes")
				Class cls = ucl.loadClass(className);
				if (cls != null) {
					@SuppressWarnings("unchecked")
					Annotation versionAnnotation = cls.getAnnotation(Version.class);
					if (versionAnnotation != null) {
						Version version = (Version) versionAnnotation;
						versionInfo.put("versionNumber", version.versionNumber());
						versionInfo.put("information", version.information());
					}
				}
			} else if (tomcatProjectfile.getName().contains(".x")) {
				FileInputStream fis = new FileInputStream(tomcatProjectfile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis, "GBK"));
				String line = null;
				int lineCount = 0;
				while ((line = br.readLine()) != null) {
					if (lineCount == 0) {
						if (line.contains("#versionNumber=")) {
							versionInfo.put("versionNumber",
									Integer.parseInt(line.replace("#versionNumber=", "").trim()));
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
						modifyInfo.put(file, "新增");
					} else {
						StringBuffer sb = new StringBuffer();
						Integer i1 = tomcatFvi.getVersionNumber();
						Integer i2 = file.getVersionNumber();
						if (i1 != null) {
							if (!i1.equals(i2)) {
								sb.append("文件版本有变化，上次记录:" + file.getVersionNumber() + ",当前tomcat该文件:" + tomcatFvi.getVersionNumber() + "。");
							}
						} else if (i2 != null) {
							sb.append("文件版本有变化，上次记录:" + file.getVersionNumber() + ",当前tomcat该文件:" + tomcatFvi.getVersionNumber() + "。");
						}
						String s1 = tomcatFvi.getInformation();
						String s2 = file.getInformation();
						if (!StringUtil.isBlank(s1)) {
							if (!s1.equals(s2)) {
								sb.append("文件版本说明有变化，上次记录:" + file.getInformation() + ",当前:" + tomcatFvi.getInformation() + "。");
							}
						} else if (!StringUtil.isBlank(s2)) {
							sb.append("文件版本说明有变化，上次记录:" + file.getInformation() + ",当前:" + tomcatFvi.getInformation() + "。");
						}
						if (!tomcatFvi.getFileSize().equals(file.getFileSize())) {
							sb.append("文件大小有变化，上次记录:" + file.getFileSize() + ",当前tomcat该文件:" + tomcatFvi.getFileSize() + "。");
						}
						if (!tomcatFvi.getLastModifyTime().equals(file.getLastModifyTime())) {
							sb.append("文件修改时间有变化，上次记录:" + file.getLastModifyTime() + ",当前tomcat该文件:" + tomcatFvi.getLastModifyTime() + "。");
						}
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
			int threadCount = 4;
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
						Map<String, Object> versionInfo = VersionUtil.getVersionInfo(file);
						if (versionInfo != null && versionInfo.size() > 0) {
							fvi.setVersionNumber(versionInfo.get("versionNumber") == null ? null : (Integer) versionInfo.get("versionNumber"));
							fvi.setInformation(versionInfo.get("information") == null ? null : (String) versionInfo.get("information"));
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
	 * 保存tomcat项目中文件版本信息
	 * 
	 * @param deployId
	 * @param tomcatProjectDir
	 * @throws Exception
	 */
	public static void saveTomcatFileVersionInfo(Integer deployId, File tomcatProjectDir) throws Exception {
		List<File> fileList = new ArrayList<File>();
		FileUtil.listFiles(tomcatProjectDir, fileList);
		if (fileList != null && fileList.size() > 0) {
			List<FileVersionInfo> fviList = SqliteUtil.getAllFileVersionInfo();
			SqliteUtil.saveFileVersionModifyBak(fviList);
			SqliteUtil.deleteAllFileVersionInfo();
			int threadCount = 4;
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
				new SaveFileVersionInfoThread(deployId, subList, cdl).start();
			}
			cdl.await();
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

		private CountDownLatch countDownLatch;

		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		private Calendar cal = Calendar.getInstance();

		public SaveFileVersionInfoThread(Integer deployId, List<File> fileList, CountDownLatch countDownLatch) {
			this.deployId = deployId;
			this.fileList = fileList;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() {
			try {
				if (fileList != null && countDownLatch != null) {
					SqlSession sqlSession = SqliteUtil.getSqlSessionFactory().openSession();
					FileVersionInfoMapper fvim = sqlSession.getMapper(FileVersionInfoMapper.class);
					for (File file : fileList) {
						FileVersionInfo fvi = new FileVersionInfo();
						fvi.setDeployId(deployId);
						fvi.setFile(file.getPath());
						fvi.setFileSize(Long.valueOf(file.length()).intValue());
						cal.setTimeInMillis(file.lastModified());
						fvi.setLastModifyTime(sdf.format(cal.getTime()));
						Map<String, Object> versionInfo = VersionUtil.getVersionInfo(file);
						if (versionInfo != null && versionInfo.size() > 0) {
							fvi.setVersionNumber(versionInfo.get("versionNumber") == null ? null : (Integer) versionInfo.get("versionNumber"));
							fvi.setInformation(versionInfo.get("information") == null ? null : (String) versionInfo.get("information"));
						}
						fvim.insert(fvi);
						sqlSession.commit();
					}
					sqlSession.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				countDownLatch.countDown();
			}
		}
	}
}
