package com.deploy.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import com.deploy.dao.CommonMapper;
import com.deploy.dao.CommonParam;
import com.deploy.dao.DeployConfig;
import com.deploy.dao.DeployConfigMapper;
import com.deploy.dao.DeployMain;
import com.deploy.dao.DeployMainMapper;
import com.deploy.dao.FileCheckInfo;
import com.deploy.dao.FileCheckInfoMapper;
import com.deploy.dao.FileVersionInfo;
import com.deploy.dao.FileVersionInfoMapper;
import com.deploy.dao.FileVersionModifyBak;
import com.deploy.dao.FileVersionModifyBakMapper;
import com.deploy.dao.GitDiffInfo;
import com.deploy.dao.GitDiffInfoMapper;
import com.deploy.mojo.DeployMojo;

/**
 * sqlite数据库操作工具类
 * 
 * @author wangchunbin
 *
 */
public class SqliteUtil {
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SqlSessionFactory ssf = null;

	/**
	 * 获取mybatis SqlSessionFactory
	 * 
	 * @return
	 * @throws IOException
	 */
	public static synchronized SqlSessionFactory getSqlSessionFactory() throws IOException {
		if (ssf == null) {
			String resource = "com/deploy/dao/MybatisConfig.xml";
			InputStream inputStream = Resources.getResourceAsStream(resource);
			ssf = new SqlSessionFactoryBuilder().build(inputStream);
			return ssf;
		} else {
			return ssf;
		}
	}

	/**
	 * 初始化sqlite数据库
	 * 
	 * @throws IOException
	 */
	public static void initSqliteDB() throws IOException {
		SqlSession sqlSession = getSqlSessionFactory().openSession();
		CommonMapper cm = sqlSession.getMapper(CommonMapper.class);
		CommonParam commonParam = new CommonParam();
		String sql = "CREATE TABLE deploy_config (" + "deploy_id  INTEGER NOT NULL," + "git_remote_address  TEXT(100),"
				+ "local_git_path  TEXT(100)," + "project_at_git_repository_path  TEXT(100)," + "branch  TEXT(100),"
				+ "git_remote_username  TEXT(100)," + "git_remote_email  TEXT(100)," + "git_remote_password  TEXT(100),"
				+ "backup_dir  TEXT(100)," + "tomcat_project_dir  TEXT(100)," + "driver_class_name  TEXT(100),"
				+ "url  TEXT(100)," + "username  TEXT(100)," + "password  TEXT(100),"
				+ "data_correction_dir  TEXT(100)," + "separator  TEXT(100)," + "PRIMARY KEY (deploy_id)" + ")";
		commonParam.setSql(sql);
		cm.executeUpdateSql(commonParam);
		sqlSession.commit();
		sql = "CREATE TABLE deploy_main (" + "deploy_id  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
				+ "branch  TEXT(100)," + "project_version  TEXT(100)," + "git_commit_id  TEXT(100),"
				+ "deploy_start_time  TEXT(100)," + "deploy_end_time  TEXT(100)," + "is_success  TEXT(100)" + ")";
		commonParam.setSql(sql);
		cm.executeUpdateSql(commonParam);
		sqlSession.commit();
		sql = "CREATE TABLE file_check_info (" + "id  INTEGER NOT NULL," + "deploy_id  INTEGER,"
				+ "last_deploy_id  INTEGER," + "file  TEXT(100)," + "check_time  TEXT(100)," + "check_info  TEXT(200),"
				+ "PRIMARY KEY (id ASC)" + ")";
		commonParam.setSql(sql);
		cm.executeUpdateSql(commonParam);
		sqlSession.commit();
		sql = "CREATE TABLE file_version_info (" + "file  TEXT(100) NOT NULL," + "deploy_id  INTEGER,"
				+ "version_number  INTEGER," + "information  TEXT(200)," + "last_modify_time  TEXT(100),"
				+ "file_size  INTEGER," + "PRIMARY KEY (file ASC)" + ")";
		commonParam.setSql(sql);
		cm.executeUpdateSql(commonParam);
		sqlSession.commit();
		sql = "CREATE TABLE file_version_modify_bak (" + "ID  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
				+ "file  TEXT(100)," + "deploy_id  INTEGER," + "version_number  INTEGER," + "infomation  TEXT(200),"
				+ "last_modify_time  TEXT(100)," + "file_size  INTEGER" + ")";
		commonParam.setSql(sql);
		cm.executeUpdateSql(commonParam);
		sqlSession.commit();
		sql = "CREATE TABLE git_diff_info (" + "id  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + "deploy_id  INTEGER,"
				+ "git_commit_id  TEXT(100)," + "last_git_commit_id  TEXT(100)," + "file  TEXT(100),"
				+ "modify_type  TEXT(100)" + ")";
		commonParam.setSql(sql);
		cm.executeUpdateSql(commonParam);
		sqlSession.commit();
		sqlSession.close();
	}

	/**
	 * 获取sqllite数据库存储的所有文件版本信息
	 * 
	 * @return
	 * @throws IOException
	 */
	public static List<FileVersionInfo> getAllFileVersionInfo() throws IOException {
		SqlSession sqlSession = getSqlSessionFactory().openSession();
		FileVersionInfoMapper fvim = sqlSession.getMapper(FileVersionInfoMapper.class);
		List<FileVersionInfo> list = fvim.selectAll();
		sqlSession.close();
		return list;
	}

	/**
	 * 创建部署主信息
	 * 
	 * @return
	 * @throws IOException
	 */
	public static DeployMain createDeployMain() throws IOException {
		SqlSession sqlSession = getSqlSessionFactory().openSession();
		DeployMainMapper dmm = sqlSession.getMapper(DeployMainMapper.class);
		DeployMain record = new DeployMain();
		record.setDeployStartTime(sdf.format(new Date()));
		dmm.insert(record);
		sqlSession.commit();
		sqlSession.close();
		return record;
	}

	/**
	 * 保存本次部署配置信息
	 * 
	 * @param hdm
	 * @throws IOException
	 */
	public static void saveDeployConfig(Integer deployId, DeployMojo dm) throws IOException {
		SqlSession sqlSession = getSqlSessionFactory().openSession();
		DeployConfigMapper dcm = sqlSession.getMapper(DeployConfigMapper.class);
		DeployConfig dc = new DeployConfig();
		dc.setBackupDir(dm.getBackupDir().getPath());
		dc.setBranch(dm.getBranch());
		dc.setDataCorrectionDir(dm.getDataCorrectionDir() == null ? null : dm.getDataCorrectionDir().getPath());
		dc.setDeployId(deployId);
		dc.setDriverClassName(dm.getDriverClassName());
		dc.setGitRemoteAddress(dm.getGitRemoteAddress());
		dc.setGitRemoteEmail(dm.getGitRemoteEmail());
		dc.setGitRemotePassword(dm.getGitRemotePassWord());
		dc.setGitRemoteUsername(dm.getGitRemoteUserName());
		dc.setLocalGitPath(dm.getLocalGitPath());
		dc.setPassword(dm.getPassword());
		dc.setProjectAtGitRepositoryPath(dm.getProjectAtGitRepositoryPath());
		dc.setSeparator(dm.getSeparator());
		dc.setTomcatProjectDir(dm.getTomcatProjectDir().getPath());
		dc.setUrl(dm.getUrl());
		dc.setUsername(dm.getUsername());
		dcm.insert(dc);
		sqlSession.commit();
		sqlSession.close();
	}

	/**
	 * 保存文件检查信息
	 * 
	 * @param deployId
	 * @param lastDeployId
	 * @param checkInfo
	 * @throws IOException
	 */
	public static void saveFileVersionCheckInfo(Integer deployId, Integer lastDeployId,
			Map<FileVersionInfo, String> checkInfo) throws IOException {
		if (checkInfo != null && checkInfo.size() > 0) {
			SqlSession sqlSession = getSqlSessionFactory().openSession();
			FileCheckInfoMapper fcim = sqlSession.getMapper(FileCheckInfoMapper.class);
			for (Entry<FileVersionInfo, String> entry : checkInfo.entrySet()) {
				FileCheckInfo record = new FileCheckInfo();
				record.setCheckInfo(entry.getValue());
				record.setCheckTime(sdf.format(new Date()));
				record.setDeployId(deployId);
				record.setFile(entry.getKey().getFile());
				record.setLastDeployId(lastDeployId);
				fcim.insert(record);
				sqlSession.commit();
			}
			sqlSession.close();
		}
	}

	/**
	 * 保存git版本对比差异信息
	 * 
	 * @param deployId
	 * @param commitId
	 * @param lastCommitId
	 * @param diffInfo
	 * @throws IOException
	 */
	public static void saveGitDiffInfo(Integer deployId, String commitId, String lastCommitId,
			Map<String, String> diffInfo) throws IOException {
		if (diffInfo != null && diffInfo.size() > 0) {
			SqlSession sqlSession = getSqlSessionFactory().openSession();
			GitDiffInfoMapper gdim = sqlSession.getMapper(GitDiffInfoMapper.class);
			for (Entry<String, String> entry : diffInfo.entrySet()) {
				GitDiffInfo record = new GitDiffInfo();
				record.setDeployId(deployId);
				record.setGitCommitId(commitId);
				record.setLastGitCommitId(lastCommitId);
				record.setFile(entry.getKey());
				record.setModifyType(entry.getValue());
				gdim.insert(record);
				sqlSession.commit();
			}
			sqlSession.close();
		}
	}

	/**
	 * 保存文件版本备份信息
	 * 
	 * @param fviList
	 * @throws IOException
	 */
	public static void saveFileVersionModifyBak(List<FileVersionInfo> fviList) throws IOException {
		if (fviList != null && fviList.size() > 0) {
			SqlSession sqlSession = getSqlSessionFactory().openSession();
			FileVersionModifyBakMapper fvmbm = sqlSession.getMapper(FileVersionModifyBakMapper.class);
			for (FileVersionInfo fvi : fviList) {
				FileVersionModifyBak fvmb = new FileVersionModifyBak();
				fvmb.setDeployId(fvi.getDeployId());
				fvmb.setFile(fvi.getFile());
				fvmb.setFileSize(fvi.getFileSize());
				fvmb.setInfomation(fvi.getInformation());
				fvmb.setLastModifyTime(fvi.getLastModifyTime());
				fvmb.setVersionNumber(fvi.getVersionNumber());
				fvmbm.insert(fvmb);
				sqlSession.commit();
			}
			sqlSession.close();
		}
	}

	/**
	 * 删除所有文件版本信息
	 * 
	 * @throws IOException
	 */
	public static void deleteAllFileVersionInfo() throws IOException {
		SqlSession sqlSession = getSqlSessionFactory().openSession();
		FileVersionInfoMapper fvim = sqlSession.getMapper(FileVersionInfoMapper.class);
		fvim.deleteAll();
		sqlSession.commit();
		sqlSession.close();
	}
}
