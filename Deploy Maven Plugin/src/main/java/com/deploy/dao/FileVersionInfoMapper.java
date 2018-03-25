package com.deploy.dao;

import java.util.List;

public interface FileVersionInfoMapper {
    int deleteByPrimaryKey(String file);

    int insert(FileVersionInfo record);

    int insertSelective(FileVersionInfo record);

    FileVersionInfo selectByPrimaryKey(String file);

    int updateByPrimaryKeySelective(FileVersionInfo record);

    int updateByPrimaryKey(FileVersionInfo record);
    
    List<FileVersionInfo> selectAll();
    
    int deleteAll();
}