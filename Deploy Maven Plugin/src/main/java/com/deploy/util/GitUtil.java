package com.deploy.util;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	public static Repository cloneByCmd(String remoteAddress, String localGitPath, String branch, String userName, String email)
			throws Exception {
		File dir = new File(localGitPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		CmdUtil.execCMD(localGitPath, "git config --global user.name \"" + userName + "\"");
		CmdUtil.execCMD(localGitPath, "git config --global user.email \"" + email + "\"");
		Map<String, String> cloneInfo = CmdUtil.execCMD(localGitPath, "git clone -b " + branch + " " + remoteAddress + " " + localGitPath);
		System.out.println("cloneInfo:\r\n" + cloneInfo);
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(localGitPath + "/.git")).readEnvironment().findGitDir()
				.build();
		return repository;
	}

	/**
	 * 获取本地仓库(.git目录路径)
	 * 
	 * @param localGitAddress
	 * @return
	 * @throws Exception
	 */
	public static Repository getLocalRepository(String localGitAddress) throws Exception {
		Git git =null;
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
	public static Repository clone(String remoteAddress, String localGitPath, String branch, String userName,
			String passWord) throws Exception {
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
	 * 拉取远程分支
	 * 
	 * @param localRepository
	 * @param remoteBranchName
	 * @throws Exception
	 */
	public static void pull(Repository localRepository, String remoteBranchName, String userName, String passWord)
			throws Exception {
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
	 * 对比两个版本差异,获取修改的文件及修改类型
	 * 
	 * @param localRepository
	 * @param Child
	 * @param Parent
	 * @throws Exception
	 */
	public static Map<String, String> diff(Repository localRepository, String oldCommitID, String newCommitID)
			throws Exception {
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
		for (DiffEntry diffEntry : diffs) {
			map.put((diffEntry.getNewPath().contains("null") ? diffEntry.getOldPath() : diffEntry.getNewPath()), diffEntry.getChangeType().toString());
		}
		git.close();
		return map;
	}
}
