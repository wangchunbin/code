package com.deploy.dao;

public class DeployConfig {
    private Integer deployId;

    private String gitRemoteAddress;

    private String localGitPath;

    private String projectAtGitRepositoryPath;

    private String branch;

    private String gitRemoteUsername;

    private String gitRemoteEmail;

    private String gitRemotePassword;

    private String backupDir;

    private String tomcatProjectDir;

    private Integer tomcatPort;

    private String driverClassName;

    private String url;

    private String username;

    private String password;

    private String sqlAtGitRepositoryPath;

    private String separator;

    public Integer getDeployId() {
        return deployId;
    }

    public void setDeployId(Integer deployId) {
        this.deployId = deployId;
    }

    public String getGitRemoteAddress() {
        return gitRemoteAddress;
    }

    public void setGitRemoteAddress(String gitRemoteAddress) {
        this.gitRemoteAddress = gitRemoteAddress == null ? null : gitRemoteAddress.trim();
    }

    public String getLocalGitPath() {
        return localGitPath;
    }

    public void setLocalGitPath(String localGitPath) {
        this.localGitPath = localGitPath == null ? null : localGitPath.trim();
    }

    public String getProjectAtGitRepositoryPath() {
        return projectAtGitRepositoryPath;
    }

    public void setProjectAtGitRepositoryPath(String projectAtGitRepositoryPath) {
        this.projectAtGitRepositoryPath = projectAtGitRepositoryPath == null ? null : projectAtGitRepositoryPath.trim();
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch == null ? null : branch.trim();
    }

    public String getGitRemoteUsername() {
        return gitRemoteUsername;
    }

    public void setGitRemoteUsername(String gitRemoteUsername) {
        this.gitRemoteUsername = gitRemoteUsername == null ? null : gitRemoteUsername.trim();
    }

    public String getGitRemoteEmail() {
        return gitRemoteEmail;
    }

    public void setGitRemoteEmail(String gitRemoteEmail) {
        this.gitRemoteEmail = gitRemoteEmail == null ? null : gitRemoteEmail.trim();
    }

    public String getGitRemotePassword() {
        return gitRemotePassword;
    }

    public void setGitRemotePassword(String gitRemotePassword) {
        this.gitRemotePassword = gitRemotePassword == null ? null : gitRemotePassword.trim();
    }

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir == null ? null : backupDir.trim();
    }

    public String getTomcatProjectDir() {
        return tomcatProjectDir;
    }

    public void setTomcatProjectDir(String tomcatProjectDir) {
        this.tomcatProjectDir = tomcatProjectDir == null ? null : tomcatProjectDir.trim();
    }

    public Integer getTomcatPort() {
        return tomcatPort;
    }

    public void setTomcatPort(Integer tomcatPort) {
        this.tomcatPort = tomcatPort;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName == null ? null : driverClassName.trim();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
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
        this.separator = separator == null ? null : separator.trim();
    }
}