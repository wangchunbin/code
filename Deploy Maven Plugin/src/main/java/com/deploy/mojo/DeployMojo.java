package com.deploy.mojo;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Repository;
import com.deploy.dao.DeployMain;
import com.deploy.dao.FileVersionInfo;
import com.deploy.util.CmdUtil;
import com.deploy.util.ExcelUtil;
import com.deploy.util.FileUtil;
import com.deploy.util.GitUtil;
import com.deploy.util.SqliteUtil;
import com.deploy.util.StringUtil;
import com.deploy.util.TomcatUtil;
import com.deploy.util.VersionUtil;

/**
 * 程序部署插件mojo类
 * 
 * @author WangChunBin
 *
 */
@Mojo(name = "Deploy")
public class DeployMojo extends AbstractMojo {
	/**
	 * 远程仓库URL地址
	 */
	@Parameter
	private String gitRemoteAddress;

	/**
	 * 本地仓库地址
	 */
	@Parameter
	private String localGitPath;

	/**
	 * 项目在git仓库的相对路径(在本地相对于localGitPath路径)
	 */
	@Parameter
	private String projectAtGitRepositoryPath;

	/**
	 * 分支名称
	 */
	@Parameter
	private String branch;
	
	/**
	 * git提交版本号，设置该参数可实现tomcat中项目版本回退，如果不设置该参数，则取分支最新的提交版本号；如果设置了该参数值，则拉取相应版本代码执行全量部署。
	 */
	@Parameter
	private String gitCommitID;

	/**
	 * gitLab或者gitHub 用户名
	 */
	@Parameter
	private String gitRemoteUserName;

	/**
	 * gitLab或者gitHub 注册邮箱
	 */
	@Parameter
	private String gitRemoteEmail;

	/**
	 * gitLab或者gitHub 密码
	 */
	@Parameter
	private String gitRemotePassWord;

	/**
	 * 原文件备份目录
	 */
	@Parameter
	private File backupDir;

	/**
	 * tomcat中项目路径
	 */
	@Parameter
	private File tomcatProjectDir;

	/**
	 * tomcat端口
	 */
	@Parameter
	private Integer tomcatPort;

	/**
	 * 数据库驱动类
	 */
	@Parameter
	private String driverClassName;

	/**
	 * 数据库连接URL
	 */
	@Parameter
	private String url;

	/**
	 * 数据库连接用户名
	 */
	@Parameter
	private String username;

	/**
	 * 数据库连接密码
	 */
	@Parameter
	private String password;

	/**
	 * 增量SQL脚本及数据execl在Git仓库中相对路径(在本地相对于本地仓库localGitPath路径)
	 */
	@Parameter
	private String sqlAtGitRepositoryPath;

	/**
	 * 语句分割符
	 */
	@Parameter
	private String separator;

	@Override
	public String toString() {
		return "[gitRemoteAddress=" + gitRemoteAddress + ", localGitPath=" + localGitPath
				+ ", projectAtGitRepositoryPath=" + projectAtGitRepositoryPath + ", branch=" + branch
				+ ", gitCommitID=" + gitCommitID + ", gitRemoteUserName=" + gitRemoteUserName + ", gitRemoteEmail=" + gitRemoteEmail
				+ ", gitRemotePassWord=" + gitRemotePassWord + ", backupDir="
				+ (backupDir == null ? null : backupDir.getPath()) + ", tomcatProjectDir="
				+ (tomcatProjectDir == null ? null : tomcatProjectDir.getPath()) + ", tomcatPort="
				+ (tomcatPort == null ? null : tomcatPort) + ", driverClassName=" + driverClassName + ", url=" + url
				+ ", username=" + username + ", password=" + password + ", sqlAtGitRepositoryPath="
				+ sqlAtGitRepositoryPath+ ", separator=" + separator + "]";
	}

	/**
	 * 插件执行目标方法
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("插件参数：" + this.toString());
		getLog().info("正在检查插件入参是否为空值...");
		if (StringUtil.isBlank(gitRemoteAddress) || StringUtil.isBlank(localGitPath)
				|| StringUtil.isBlank(projectAtGitRepositoryPath) || StringUtil.isBlank(branch)
				|| StringUtil.isBlank(gitRemoteUserName) || StringUtil.isBlank(gitRemoteEmail)
				|| StringUtil.isBlank(gitRemotePassWord) || backupDir == null || tomcatProjectDir == null
				|| tomcatPort == null) {
			throw new MojoFailureException("传入的参数有误，请检查！");
		}
		getLog().info("关键参数不为空！");
		getLog().info("正在检查本机JDK版本是否是1.6.X...");
		String javaVersion = System.getProperty("java.version");// 获取jdk版本
		if (!javaVersion.contains("1.6")) {
			throw new MojoFailureException("当前jdk版本不是1.6,请检查！");
		}
		getLog().info("当前JDK版本符合要求！");
		getLog().info("正在检查当前目录Sqlite数据库是否存在...");
		File deployDB = new File("deploy.db");
		if (!deployDB.exists()) {
			getLog().info("当前目录无Sqlite数据库，准备执行Sqlite数据库创建及初始化...");
			try {
				SqliteUtil.initSqliteDB();// 执行sqlite数据库初始化脚本
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoFailureException("初始化Sqlite数据库失败！");
			}
		}
		getLog().info("Sqlite数据库准备就绪！");
		getLog().info("正在检查tomcat项目路径是否是空目录...");
		boolean tomcatProjectDirIsNull = FileUtil.isNullDir(tomcatProjectDir); // 判空
		getLog().info("tomcat项目路径:" + tomcatProjectDir.getPath() + (tomcatProjectDirIsNull ? "是" : "不是") + "空目录！");
		boolean isIncrementalDeployment = false;// 是否是增量部署,true增量,fasle全量
		String tomcatDeployId = null;// tomcat项目部署ID
		String tomcatBranch = null;// tomcat项目git分支
		String tomcatGitCommitId = null;// git Commit ID
		String tomcatProjectVersion = null;// tomcat中项目版本号
		if (!tomcatProjectDirIsNull) {
			getLog().info("正在获取tomcat中项目版本信息...");
			try {
				String tomcatProjectVersionInfo = FileUtil.readFile(tomcatProjectDir.getPath() + "/version.txt"); // 读取tomcat项目版本文件内容
				if (!StringUtil.isBlank(tomcatProjectVersionInfo)) {
					String[] strs = tomcatProjectVersionInfo.trim().split(":"); // 切割出内容项
					if (strs != null && strs.length == 4) {// 取到版本信息，执行增量部署，否则全量部署
						tomcatDeployId = strs[0];
						tomcatBranch = strs[1];
						tomcatGitCommitId = strs[2];
						tomcatProjectVersion = strs[3];
						if (!branch.equals(tomcatBranch.trim())) {
							isIncrementalDeployment = false;// 当前待部署分支与tomcat项目git分支不相同，则执行全量部署！
						} else {
							isIncrementalDeployment = true;
						}
					}
				}
			} catch (Exception e) {
			}
			getLog().info("tomcat中项目部署ID:" + tomcatDeployId + ",Git分支:" + tomcatBranch + ",Git提交ID:" + tomcatGitCommitId + ",项目版本号:" + tomcatProjectVersion);
		}
		getLog().info("准备执行" + (isIncrementalDeployment ? "增量部署！" : "全量部署！"));
		getLog().info("正在创建数据库部署主表信息...");
		DeployMain deployMain = null;
		try {
			deployMain = SqliteUtil.createDeployMain(); // 创建主表记录，拿到表主键(部署ID:deployId)
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("创建数据库部署主表信息失败！");
		}
		getLog().info("创建数据库部署主表信息成功！本次部署ID:" + deployMain.getDeployId());
		getLog().info("正在保存本次部署配置信息...");
		try {
			SqliteUtil.saveDeployConfig(deployMain.getDeployId(), this); // 将当前mojo对象配置信息保存进库
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("保存本次部署配置信息失败！");
		}
		getLog().info("保存本次部署配置信息成功！");
		Map<FileVersionInfo, String> checkInfo = null;
		if (isIncrementalDeployment) {
			getLog().info("正在检查tomcat中项目文件变化...");
			try {
				checkInfo = VersionUtil.getTomcatFileModifyInfo(tomcatProjectDir); // 通过tomcat文件与数据库记录信息对比，获取tomcat文件修改信息
				if (checkInfo != null && checkInfo.size() > 0) {
					for (Entry<FileVersionInfo, String> entry : checkInfo.entrySet()) {
						getLog().warn(entry.getKey().getFile() + ":" + entry.getValue());
					}
					SqliteUtil.saveFileVersionCheckInfo(deployMain.getDeployId(), Integer.parseInt(tomcatDeployId), checkInfo);// 保存检查信息
					getLog().info("是否继续执行?(Y/N)");
					Scanner scanner = new Scanner(System.in); // 获取用户输入,决定是否继续执行
					String flag = scanner.nextLine();
					if (!"Y".equalsIgnoreCase(flag.trim())) {
						getLog().info("感谢使用,记得点赞额！Bye!");
						scanner.close();
						return;
					}
					scanner.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoFailureException("正在检查tomcat中文件变化失败！");
			}
			getLog().info("检查tomcat中项目文件变化完成！");
		}
		getLog().info("正在获取本地git仓库...");
		Repository repository = null;
		try {
			repository = GitUtil.getLocalRepository(localGitPath); // 获取本地仓库对象
			if (repository == null) {
				getLog().info("无本地git仓库,准备执行克隆" + branch + "分支命令...");
				repository = GitUtil.cloneByCmd(gitRemoteAddress, localGitPath, branch, gitRemoteUserName, gitRemoteEmail); // 使用git clone命令克隆
				if (repository == null) {
					throw new MojoFailureException("执行克隆命令失败！");
				}
			} else {
				getLog().info("准备执行pull命令,拉取" + branch + "分支更新...");
				GitUtil.pullByCmd(localGitPath, branch);// 拉取分支更新
			}
			if(!StringUtil.isBlank(gitCommitID)){
				getLog().info("准备迁出" + branch + "分支" + gitCommitID +"版本...");
				GitUtil.checkoutByBranchOrCommitID(repository, gitCommitID);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("获取本地git仓库失败！");
		}
		getLog().info("获取本地git仓库成功！");
		String newCommitID = null;// 最新commitID
		Map<String, String> gitCommitFileVersionInfo = null;// git提交文件版本信息 
		String newProjectVersion = null;// pom.xml中项目版本号
		try {
			if(!StringUtil.isBlank(gitCommitID)){
				newCommitID = gitCommitID;
			}else{
				newCommitID = GitUtil.getLastCommitID(repository);
			}
			gitCommitFileVersionInfo = GitUtil.getGitCommitFileVersionInfo(repository);
			newProjectVersion = VersionUtil.getProjectVersionByPOM(new File(localGitPath + "/" + projectAtGitRepositoryPath + "/pom.xml"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("获取" + branch + "分支最新CommitID及Commit备注或pom文件中项目版本号失败！");
		}
		getLog().info(branch + "分支最新CommitID:" + newCommitID + ",pom文件中项目版本号:" + newProjectVersion);
		try {
			deployMain.setBranch(branch);
			deployMain.setGitCommitId(newCommitID);
			deployMain.setProjectVersion(newProjectVersion);
			SqliteUtil.updateDeployMain(deployMain);// 更新部署主表信息
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("更新部署主表信息失败！");
		}
		Map<String, String> diffInfo = null;
		if (isIncrementalDeployment) {
			getLog().info("正在执行对比版本差异操作...");
			try {
				diffInfo = GitUtil.diff(repository, tomcatGitCommitId, newCommitID);// 通过对比两个commitID，获取差异信息
				if (diffInfo != null && diffInfo.size() > 0) {
					for (Entry<String, String> entry : diffInfo.entrySet()) {
						getLog().warn("文件:" + entry.getKey() + ",修改类型:" + entry.getValue());
					}
				}
				SqliteUtil.saveGitDiffInfo(deployMain.getDeployId(), newCommitID, tomcatGitCommitId, diffInfo);// 保存差异信息
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoFailureException("执行对比版本差异操作失败！");
			}
			getLog().info("执行对比版本差异操作成功！");
		}
		getLog().info("正在进行java源文件、x文件版本号写入操作...");
		String stamp = deployMain.getDeployId() + "";// 将本次部署ID作为备份文件夹下主目录名称
		File sourceDir = null;// 源文件备份路径,作为源文件版本号写入、编译打包目录
		try {
			sourceDir = new File(backupDir.getPath() + "/" + stamp + "/source/" + tomcatProjectDir.getName());
			sourceDir.mkdirs();
			FileUtil.copyDirContentToDir(new File(localGitPath + "/" + projectAtGitRepositoryPath), sourceDir);// 源码复制
			if (isIncrementalDeployment) {
				VersionUtil.incrementalWriteVersion(sourceDir, tomcatProjectDir.getName(), newProjectVersion, gitCommitFileVersionInfo, diffInfo); // 写入增量版本信息
			} else {
				VersionUtil.batchWriteInitVersion(sourceDir,newProjectVersion);// 写入全量版本信息
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("进行java源文件、x文件版本号写入操作失败！");
		}
		getLog().info("java源文件、x文件版本号写入操作完成！");
		getLog().info("正在执行maven命令，编译打包项目代码...");
		Map<String, String> result = null;
		try {
			result = CmdUtil.execCMD(sourceDir.getPath(), "mvn package -Dmaven.test.skip=true");// 执行maven打包命令
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("执行maven命令，编译打包项目代码失败！");
		}
		if (StringUtil.isBlank(result.get("InputInfo")) || !result.get("InputInfo").contains("SUCCESS")) {
			throw new MojoFailureException("执行maven命令，编译打包项目代码失败！");
		}
		getLog().info("执行maven命令，编译打包项目代码成功！");
		/*getLog().info("正在执行关闭tomcat操作...");
		try {
			TomcatUtil.shutdown(tomcatProjectDir.getParentFile().getParentFile(), tomcatPort);// 关闭tomcat
		} catch (Exception e) {
			getLog().warn("当前tomcat未运行或者其他原因导致关闭失败！当前程序将继续运行！");
		}*/
		getLog().info("正在备份目录下创建相关目录并解压war包...");
		File tempDir = null;// 临时目录
		File tomcatBackupDir = null;// tomcat文件备份目录
		try {
			String tempPath = backupDir.getPath() + "/" + stamp + "/" + tomcatProjectDir.getName();
			tempDir = new File(tempPath);
			tempDir.mkdirs();// 创建临时目录，解压war包
			String sourceBackupPath = backupDir.getPath() + "/" + stamp + "/backup";
			tomcatBackupDir = new File(sourceBackupPath);
			tomcatBackupDir.mkdirs();// 创建备份目录
			List<File> wars = FileUtil.findFile(new File(sourceDir.getPath() + "/target"), ".*\\.war$");// 查找打包后的war包
			FileUtil.copyFileToDir(wars.get(0), tempDir);// 拷贝war包
			CmdUtil.execCMD(tempDir.getPath(), "jar xvf " + wars.get(0).getName());// 执行war包解压操作
			FileUtil.deleteDirOrFile(tempDir.getPath() + "/" + wars.get(0).getName());// 删除war包
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("执行备份目录下创建相关目录并解压war包操作失败！");
		}
		getLog().info("执行备份目录下创建相关目录并解压war包操作成功！");
		Map<File, String> jarDiffInfo = null;// jar差异信息
		if (isIncrementalDeployment) {
			getLog().info("正在执行对比jar包差异操作...");
			try {
				jarDiffInfo = FileUtil.diffLibJar(new File(tempDir.getPath()+"/WEB-INF/lib"), new File(tomcatProjectDir+"/WEB-INF/lib"));
				if (jarDiffInfo != null && jarDiffInfo.size() > 0) {
					for (Entry<File, String> entry : jarDiffInfo.entrySet()) {
						getLog().warn("文件:" + entry.getKey().getName() + ",修改类型:" + entry.getValue());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoFailureException("执行对比jar包差异操作失败！");
			}
			getLog().info("执行对比jar包差异操作成功！");
		}
		if (isIncrementalDeployment) {
			getLog().info("开始执行增量部署...");
			try {
				getLog().info("正在执行文件备份、替换及增量文件打包操作...");
				if (diffInfo.size() > 0) {
					for (Map.Entry<String, String> info : diffInfo.entrySet()) {
						String filePath = info.getKey();
						if (filePath.contains("/WebContent")) {
							String file = filePath.replace(projectAtGitRepositoryPath + "/WebContent", "");
							File tomcatFile = new File(tomcatProjectDir + "/" + file);
							String tomcateFileParentPath = tomcatFile.getParent();
							String tomcateFilePackagePath = tomcateFileParentPath.replace(tomcatProjectDir.getPath(), "");
							if(!info.getValue().contains("ADD")){// 1.备份
								String fileBackupDirPath = tomcatBackupDir.getPath() + "/" + tomcateFilePackagePath;
								File fileBackupDir = new File(fileBackupDirPath);
								fileBackupDir.mkdirs();
								FileUtil.copyFileToDir(tomcatFile, fileBackupDir);
							}
							// 2.替换或删除文件
							if (info.getValue().contains("DELETE")) {
								FileUtil.deleteDirOrFile(tomcatFile.getPath());
							} else {
								File newFile = new File(tempDir.getPath() + "/" + file);
								FileUtil.replaceFile(newFile, tomcatFile);
								// 3.将文件放入到增量目录
								if(newFile.getPath().contains("WEB-INF/config/system")){// 过滤掉system目录下文件，不写入增量包
									continue;
								}
								File incrementalDir = new File(backupDir.getPath() + "/" + stamp + "/incremental/" + tomcateFilePackagePath);
								incrementalDir.mkdirs();
								FileUtil.copyFileToDir(newFile, incrementalDir);
							}
						}
						if (filePath.contains("/src")) {
							String file = filePath.replace(projectAtGitRepositoryPath + "/src", "").replace(".java", ".class");
							File tomcatFile = new File(tomcatProjectDir + "/WEB-INF/classes/" + file);
							String tomcateFileParentPath = tomcatFile.getParent();
							String tomcateFilePackagePath = tomcateFileParentPath.replace(tomcatProjectDir.getPath(), "");
							if(!info.getValue().contains("ADD")){// 1.备份
								String fileBackupDirPath = tomcatBackupDir.getPath() + "/" + tomcateFilePackagePath;
								File fileBackupDir = new File(fileBackupDirPath);
								fileBackupDir.mkdirs();
								FileUtil.copyFileToDir(tomcatFile, fileBackupDir);
							}
							// 2.替换或删除文件
							if (info.getValue().contains("DELETE")) {
								FileUtil.deleteDirOrFile(tomcatFile.getPath());
							} else {
								File newFile = new File(tempDir.getPath() + "/WEB-INF/classes/" + file);
								FileUtil.replaceFile(newFile, tomcatFile);
								// 3.将文件放入到增量目录
								File incrementalDir = new File(backupDir + "/" + stamp + "/incremental/code/" + tomcateFilePackagePath);
								incrementalDir.mkdirs();
								FileUtil.copyFileToDir(newFile, incrementalDir);
							}
						}
					}
				} else {
					getLog().info("git版本内容无差异！");
				}
				if(jarDiffInfo !=null && jarDiffInfo.size() > 0){
					for(Entry<File,String> entry : jarDiffInfo.entrySet()){
						if(!entry.getValue().contains("ADD")){// 1.备份
							String fileBackupDirPath = tomcatBackupDir.getPath() + "/WEB-INF/lib";
							File fileBackupDir = new File(fileBackupDirPath);
							fileBackupDir.mkdirs();
							if(entry.getValue().contains("DELETE")){// 删除备份
								FileUtil.copyFileToDir(entry.getKey(), fileBackupDir);
							}else{// 修改备份
								FileUtil.copyFileToDir(new File(tomcatProjectDir+ "/WEB-INF/lib/" +entry.getKey().getName()), fileBackupDir);
							}
						}
						// 2.替换或删除文件
						if(entry.getValue().contains("DELETE")){
							FileUtil.deleteDirOrFile(entry.getKey().getPath());
						}else{
							FileUtil.replaceFile(entry.getKey(), new File(tomcatProjectDir+ "/WEB-INF/lib/" +entry.getKey().getName()));
							// 3.将文件放入到增量目录
							File incrementalDir = new File(backupDir + "/" + stamp + "/incremental/code/WEB-INF/lib");
							incrementalDir.mkdirs();
							FileUtil.copyFileToDir(entry.getKey(), incrementalDir);
						}
					}
				}else{
					getLog().info("jar无差异！");
				}
				String incrementalFilePath = backupDir.getPath() + "/" + stamp + "/incremental/incremental.xls";// 生成增量文件清单
				ExcelUtil.saveIncrementalInfo(incrementalFilePath, projectAtGitRepositoryPath, gitCommitFileVersionInfo, diffInfo, jarDiffInfo);
				getLog().info("正在更新tomcat项目版本信息...");
				FileUtil.newFile(tomcatProjectDir.getPath() + "/" + "version.txt", deployMain.getDeployId() + ":" + branch + ":" + newCommitID + ":" + newProjectVersion);// 更新tomcat项目版本信息
				getLog().info("正在保存tomcat项目中文件版本信息...");
				VersionUtil.saveIncrementalTomcatFileVersionInfo(deployMain.getDeployId(), tomcatProjectDir, checkInfo, projectAtGitRepositoryPath, diffInfo, jarDiffInfo);
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoFailureException("执行增量部署失败！");
			}
			getLog().info("执行增量部署成功！");
		} else {
			getLog().info("开始执行全量部署...");
			try {
				FileUtil.copyDirContentToDir(tomcatProjectDir, tomcatBackupDir);// 备份
				FileUtil.deleteDirOrFile(tomcatProjectDir.getPath());// 删除tomcat下项目
				tomcatProjectDir.mkdirs();
				FileUtil.copyDirContentToDir(tempDir, tomcatProjectDir);// 执行部署
				getLog().info("正在更新tomcat项目版本信息...");
				FileUtil.newFile(tomcatProjectDir.getPath() + "/" + "version.txt", deployMain.getDeployId() + ":" + branch + ":" + newCommitID + ":" + newProjectVersion);
				getLog().info("正在保存tomcat项目中文件版本信息...");
				VersionUtil.saveAllTomcatFileVersionInfo(deployMain.getDeployId(), tomcatProjectDir);//多线程保存tomcat文件版本信息
			} catch (Exception e) {
				e.printStackTrace();
				throw new MojoFailureException("执行全量部署失败！");
			}
			getLog().info("执行全量部署成功！");
		}
		if(!StringUtil.isBlank(sqlAtGitRepositoryPath)){
			File dataCorrectionDir = new File(localGitPath+"/"+sqlAtGitRepositoryPath);
			if (!FileUtil.isNullDir(dataCorrectionDir) && !StringUtil.isBlank(driverClassName)
					&& !StringUtil.isBlank(url) && !StringUtil.isBlank(username) && !StringUtil.isBlank(password)) {
				getLog().info("正在执行数据修正操作...");
				try {
					File sqlBakDir=new File(backupDir.getPath() + "/" + stamp + "/incremental/sql/" + dataCorrectionDir.getName());
					sqlBakDir.mkdirs();
					FileUtil.copyDirContentToDir(dataCorrectionDir, sqlBakDir);// 备份增量脚本
					DBInitMojo mojo = new DBInitMojo();
					mojo.setDataDir(dataCorrectionDir);
					mojo.setSeparator(separator);
					mojo.setDriverClassName(driverClassName);
					mojo.setUrl(url);
					mojo.setUsername(username);
					mojo.setPassword(password);
					mojo.execute(); // 执行sql脚本
				} catch (Exception e) {
					e.printStackTrace();
					throw new MojoFailureException("执行数据修正操作失败！");
				}
				getLog().info("执行数据修正操作完成！");
			}
		}
		try {
			deployMain.setDeployEndTime(SqliteUtil.sdf.format(new Date()));
			deployMain.setIsSuccess(isIncrementalDeployment ? "执行增量部署成功！" : "执行全量部署成功！");
			SqliteUtil.updateDeployMain(deployMain); // 将成功信息写入部署主表
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("更新部署主表信息失败！");
		}
		try{
			getLog().info("正在清理临时文件...");
			FileUtil.deleteDirOrFile(sourceDir.getParentFile().getPath());
			FileUtil.deleteDirOrFile(tempDir.getPath());
		}catch(Exception e){
			e.printStackTrace();
			throw new MojoFailureException("清理临时文件失败！");
		}
		try {
			getLog().info("正在启动Tomcat...");
			TomcatUtil.startup(tomcatProjectDir.getParentFile().getParentFile(), tomcatPort);// 启动tomcat
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("启动Tomcat失败！");
		}
	}

	/**
	 * 测试
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		DeployMojo deploy = new DeployMojo();
		deploy.setGitRemoteAddress("http://192.168.123.199:10080/root/DASHISCODE.git");
		deploy.setLocalGitPath("C:/Users/wangchunbin/Desktop/code");
		deploy.setProjectAtGitRepositoryPath("DasHealthCare/javahis5");
		//deploy.setGitCommitID("d832a5621c7a1ea6e8990163ad5d4aef5278d753");
		deploy.setBranch("QRCODE_MASTER");
		deploy.setGitRemoteUserName("wangchunbin");
		deploy.setGitRemoteEmail("474103319@qq.com");
		deploy.setGitRemotePassWord("11111111");
		deploy.setBackupDir(new File("C:/Users/wangchunbin/Desktop/bak"));
		deploy.setTomcatProjectDir(new File("C:/Users/wangchunbin/Desktop/apache-tomcat-7.0.85/webapps/javahis5"));
		deploy.setTomcatPort(8080);
		deploy.execute();
	}

	public String getGitRemoteAddress() {
		return gitRemoteAddress;
	}

	public void setGitRemoteAddress(String gitRemoteAddress) {
		this.gitRemoteAddress = gitRemoteAddress;
	}

	public String getLocalGitPath() {
		return localGitPath;
	}

	public void setLocalGitPath(String localGitPath) {
		this.localGitPath = localGitPath;
	}

	public String getProjectAtGitRepositoryPath() {
		return projectAtGitRepositoryPath;
	}

	public void setProjectAtGitRepositoryPath(String projectAtGitRepositoryPath) {
		this.projectAtGitRepositoryPath = projectAtGitRepositoryPath;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}
	
	public String getGitCommitID() {
		return gitCommitID;
	}

	public void setGitCommitID(String gitCommitID) {
		this.gitCommitID = gitCommitID;
	}

	public String getGitRemoteUserName() {
		return gitRemoteUserName;
	}

	public void setGitRemoteUserName(String gitRemoteUserName) {
		this.gitRemoteUserName = gitRemoteUserName;
	}

	public String getGitRemoteEmail() {
		return gitRemoteEmail;
	}

	public void setGitRemoteEmail(String gitRemoteEmail) {
		this.gitRemoteEmail = gitRemoteEmail;
	}

	public String getGitRemotePassWord() {
		return gitRemotePassWord;
	}

	public void setGitRemotePassWord(String gitRemotePassWord) {
		this.gitRemotePassWord = gitRemotePassWord;
	}

	public File getBackupDir() {
		return backupDir;
	}

	public void setBackupDir(File backupDir) {
		this.backupDir = backupDir;
	}

	public File getTomcatProjectDir() {
		return tomcatProjectDir;
	}

	public Integer getTomcatPort() {
		return tomcatPort;
	}

	public void setTomcatPort(Integer tomcatPort) {
		this.tomcatPort = tomcatPort;
	}

	public void setTomcatProjectDir(File tomcatProjectDir) {
		this.tomcatProjectDir = tomcatProjectDir;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSqlAtGitRepositoryPath() {
		return sqlAtGitRepositoryPath;
	}

	public void setSqlAtGitRepositoryPath(String sqlAtGitRepositoryPath) {
		this.sqlAtGitRepositoryPath = sqlAtGitRepositoryPath;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
}
