package com.deploy.dao;

public class FileVersionInfo {
    private String file;

    private Integer deployId;

    private Integer versionNumber;

    private String information;

    private String lastModifyTime;

    private Integer fileSize;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file == null ? null : file.trim();
    }

    public Integer getDeployId() {
        return deployId;
    }

    public void setDeployId(Integer deployId) {
        this.deployId = deployId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information == null ? null : information.trim();
    }

    public String getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(String lastModifyTime) {
        this.lastModifyTime = lastModifyTime == null ? null : lastModifyTime.trim();
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

	@Override
	public String toString() {
		return "FileVersionInfo [file=" + file + ", deployId=" + deployId + ", versionNumber=" + versionNumber
				+ ", information=" + information + ", lastModifyTime=" + lastModifyTime + ", fileSize=" + fileSize
				+ "]";
	}
}