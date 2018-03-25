package com.deploy.dao;

public class FileCheckInfo {
    private Integer id;

    private Integer deployId;

    private Integer lastDeployId;

    private String file;

    private String checkTime;

    private String checkInfo;

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

    public Integer getLastDeployId() {
        return lastDeployId;
    }

    public void setLastDeployId(Integer lastDeployId) {
        this.lastDeployId = lastDeployId;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file == null ? null : file.trim();
    }

    public String getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(String checkTime) {
        this.checkTime = checkTime == null ? null : checkTime.trim();
    }

    public String getCheckInfo() {
        return checkInfo;
    }

    public void setCheckInfo(String checkInfo) {
        this.checkInfo = checkInfo == null ? null : checkInfo.trim();
    }
}