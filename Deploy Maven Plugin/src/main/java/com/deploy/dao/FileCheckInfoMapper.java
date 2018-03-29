package com.deploy.dao;

public interface FileCheckInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(FileCheckInfo record);

    int insertSelective(FileCheckInfo record);

    FileCheckInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(FileCheckInfo record);

    int updateByPrimaryKey(FileCheckInfo record);
}