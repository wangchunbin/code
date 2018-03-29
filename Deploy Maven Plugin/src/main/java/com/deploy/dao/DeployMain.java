package com.deploy.dao;

public class DeployMain {
    private Integer deployId;

    private String branch;

    private String projectVersion;

    private String gitCommitId;

    private String deployStartTime;

    private String deployEndTime;

    private String isSuccess;

    public Integer getDeployId() {
        return deployId;
    }

    public void setDeployId(Integer deployId) {
        this.deployId = deployId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch == null ? null : branch.trim();
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion == null ? null : projectVersion.trim();
    }

    public String getGitCommitId() {
        return gitCommitId;
    }

    public void setGitCommitId(String gitCommitId) {
        this.gitCommitId = gitCommitId == null ? null : gitCommitId.trim();
    }

    public String getDeployStartTime() {
        return deployStartTime;
    }

    public void setDeployStartTime(String deployStartTime) {
        this.deployStartTime = deployStartTime == null ? null : deployStartTime.trim();
    }

    public String getDeployEndTime() {
        return deployEndTime;
    }

    public void setDeployEndTime(String deployEndTime) {
        this.deployEndTime = deployEndTime == null ? null : deployEndTime.trim();
    }

    public String getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(String isSuccess) {
        this.isSuccess = isSuccess == null ? null : isSuccess.trim();
    }
}