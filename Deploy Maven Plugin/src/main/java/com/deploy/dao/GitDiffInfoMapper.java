package com.deploy.dao;

public interface GitDiffInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(GitDiffInfo record);

    int insertSelective(GitDiffInfo record);

    GitDiffInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(GitDiffInfo record);

    int updateByPrimaryKey(GitDiffInfo record);
}