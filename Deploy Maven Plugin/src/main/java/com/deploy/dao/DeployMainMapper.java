package com.deploy.dao;

public interface DeployMainMapper {
    int deleteByPrimaryKey(Integer deployId);

    int insert(DeployMain record);

    int insertSelective(DeployMain record);

    DeployMain selectByPrimaryKey(Integer deployId);

    int updateByPrimaryKeySelective(DeployMain record);

    int updateByPrimaryKey(DeployMain record);
}