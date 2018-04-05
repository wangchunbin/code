package com.deploy.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * Git操作工具类
 * 
 * @author WangChunBin
 *
 */
public class GitUtil {
	/**
	 * 使用cmd命令克隆远程仓库,速度较快
	 * 
	 * @param remoteAddress
	 * @param localGitPath
	 * @param branch
	 * @param userName
	 * @param passWord
	 * @return
	 * @throws Exception
	 */
	public static Repository cloneByCmd(String remoteAddress, String localGitPath, String branch, String userName, String email) throws Exception {
		File dir = new File(localGitPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		CmdUtil.execCMD(localGitPath, "git config --global user.name \"" + userName + "\"");
		CmdUtil.execCMD(localGitPath, "git config --global user.email \"" + email + "\"");
		CmdUtil.execCMD(localGitPath, "git clone -b " + branch + " " + remoteAddress + " " + localGitPath);
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(localGitPath + "/.git")).readEnvironment().findGitDir().build();
		return repository;
	}
	
	/**
	 * 使用CMD命令拉取远程分支更新,速度较快
	 * 
	 * @param localRepository
	 * @param remoteBranchName
	 * @throws Exception
	 */
	public static void pullByCmd(String localGitPath, String branch) throws Exception {
		CmdUtil.execCMD(localGitPath, "git checkout " + branch);
		CmdUtil.execCMD(localGitPath, "git pull");
	}

	/**
	 * 获取本地仓库(.git目录路径)
	 * 
	 * @param localGitAddress
	 * @return
	 * @throws Exception
	 */
	public static Repository getLocalRepository(String localGitAddress) throws Exception {
		Git git = null;
		try {
			git = Git.open(new File(localGitAddress));
			git.log().call();
		} catch (Exception e) {
			return null;
		}
		return git.getRepository();
	}

	/**
	 * 使用jgit克隆远程仓库,速度较慢
	 * 
	 * @param remoteAddress
	 * @param localGitPath
	 * @param branch
	 * @param userName
	 * @param passWord
	 * @return
	 * @throws Exception
	 */
	public static Repository clone(String remoteAddress, String localGitPath, String branch, String userName, String passWord) throws Exception {
		FileUtil.deleteDirOrFile(localGitPath);
		File localPath = new File(localGitPath);
		localPath.mkdir();
		CloneCommand clone = Git.cloneRepository().setURI(remoteAddress).setBranch(branch).setDirectory(localPath);
		if (remoteAddress.contains("http") || remoteAddress.contains("https")) {
			UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(userName, passWord);
			clone.setCredentialsProvider(user);
		}
		Git repo1 = clone.call();
		repo1.close();
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(localPath + "/.git")).readEnvironment().findGitDir().build();
		return repository;
	}

	/**
	 * 使用jgit拉取远程分支更新,速度较慢
	 * 
	 * @param localRepository
	 * @param remoteBranchName
	 * @throws Exception
	 */
	public static void pull(Repository localRepository, String remoteBranchName, String userName, String passWord) throws Exception {
		try{
			checkoutByBranchOrCommitID(localRepository, remoteBranchName);
		}catch(Exception e){
			e.printStackTrace();
		}
		Git git = new Git(localRepository);
		UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(userName, passWord);
		git.pull().setRemoteBranchName(remoteBranchName).setCredentialsProvider(user).call();
		git.close();
	}

	/**
	 * 获取最近一次版本提交ID
	 * 
	 * @param localRepository
	 * @return
	 * @throws Exception
	 */
	public static String getLastCommitID(Repository localRepository) throws Exception {
		Git git = new Git(localRepository);
		Iterable<RevCommit> iterable = git.log().call();
		Iterator<RevCommit> iterator = iterable.iterator();
		String commitID = null;
		while (iterator.hasNext()) {
			RevCommit rc = iterator.next();
			commitID = rc.getId().getName();
			break;
		}
		git.close();
		return commitID;
	}

	/**
	 * 获取最近一次提交备注
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getLastCommitMessage(Repository localRepository) throws Exception {
		Git git = new Git(localRepository);
		Iterable<RevCommit> iterable = git.log().call();
		Iterator<RevCommit> iterator = iterable.iterator();
		String message = null;
		while (iterator.hasNext()) {
			RevCommit rc = iterator.next();
			message = rc.getShortMessage();
			break;
		}
		git.close();
		return message;
	}

	/**
	 * 对比两个版本差异,获取修改的文件及修改类型
	 * 
	 * @param localRepository
	 * @param Child
	 * @param Parent
	 * @throws Exception
	 */
	public static Map<String, String> diff(Repository localRepository, String oldCommitID, String newCommitID) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		Git git = new Git(localRepository);
		ObjectReader reader = localRepository.newObjectReader();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		ObjectId old = localRepository.resolve(oldCommitID + "^{tree}");
		ObjectId head = localRepository.resolve(newCommitID + "^{tree}");
		oldTreeIter.reset(reader, old);
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, head);
		List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
		//DasHealthCare/javahis5/src/com/javahis/util/Version.java:DELETE
		for (DiffEntry diffEntry : diffs) {
			map.put((diffEntry.getNewPath().contains("null") ? diffEntry.getOldPath() : diffEntry.getNewPath()), diffEntry.getChangeType().toString());
		}
		git.close();
		return map;
	}
	
	/**
	 * 切换分支或迁出版本
	 * 
	 * @param localRepository
	 * @param branchOrCommitID
	 * @throws Exception 
	 */
	public static void checkoutByBranchOrCommitID(Repository localRepository, String branchOrCommitID) throws Exception{
		Git git = new Git(localRepository);
		CheckoutCommand checkoutCmd = git.checkout();
		checkoutCmd.setName(branchOrCommitID);
		checkoutCmd.call();
		git.close();
	}
	
	/**
	 * 获取所有git提交文件版本说明信息
	 * 
	 * @param localRepository
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String>  getGitCommitFileVersionInfo(Repository localRepository) throws Exception {
		Map<String, String> gitCommitFileVersionInfo = new HashMap<String, String>();// 保存提交备注信息
		// commitMessage="classA={修改了method1}{DB版本号3.2}{hh}@classB={修改了method2}";
		List<String> commitMessages =  new ArrayList<String>();
		Git git = new Git(localRepository);
		Iterable<RevCommit> iterable = git.log().call();
		Iterator<RevCommit> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			RevCommit rc = iterator.next();
			String message = rc.getShortMessage();
			if(message != null && !"".equals(message.trim())){
				commitMessages.add(message);
			}
		}
		if(commitMessages.size()>0){
			Collections.reverse(commitMessages);// 反转，保持git commit顺序
			for(String commitMessage : commitMessages){
				String[] strs = commitMessage.split("@");
				for (String str : strs) {
					if (!StringUtil.isBlank(str)) {
						String[] keyValue = str.split("=");
						if (keyValue != null && keyValue.length == 2) {
							gitCommitFileVersionInfo.put(keyValue[0].trim(), keyValue[1].trim());
						}
					}
				}
			}
		}
		git.close();
		return gitCommitFileVersionInfo;
	}
}
