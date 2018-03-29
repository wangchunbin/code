package com.deploy.dao;

import com.deploy.dao.DeployConfig;

public interface DeployConfigMapper {
    int deleteByPrimaryKey(Integer deployId);

    int insert(DeployConfig record);

    int insertSelective(DeployConfig record);

    DeployConfig selectByPrimaryKey(Integer deployId);

    int updateByPrimaryKeySelective(DeployConfig record);

    int updateByPrimaryKey(DeployConfig record);
}