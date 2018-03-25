package com.deploy.dao;

public class GitDiffInfo {
    private Integer id;

    private Integer deployId;

    private String gitCommitId;

    private String lastGitCommitId;

    private String file;

    private String modifyType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDeployId() {
        return deployId;
    }

    public void setDeployId(Integer deployId) {
        this.deployId = deployId;
    }

    public String getGitCommitId() {
        return gitCommitId;
    }

    public void setGitCommitId(String gitCommitId) {
        this.gitCommitId = gitCommitId == null ? null : gitCommitId.trim();
    }

    public String getLastGitCommitId() {
        return lastGitCommitId;
    }

    public void setLastGitCommitId(String lastGitCommitId) {
        this.lastGitCommitId = lastGitCommitId == null ? null : lastGitCommitId.trim();
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file == null ? null : file.trim();
    }

    public String getModifyType() {
        return modifyType;
    }

    public void setModifyType(String modifyType) {
        this.modifyType = modifyType == null ? null : modifyType.trim();
    }
}