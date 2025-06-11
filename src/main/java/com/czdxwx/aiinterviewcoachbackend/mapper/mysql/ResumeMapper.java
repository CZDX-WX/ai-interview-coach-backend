package com.czdxwx.aiinterviewcoachbackend.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.czdxwx.aiinterviewcoachbackend.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
    // BaseMapper 提供了基本的 CRUD
    // 如果需要根据 userId 查询，可以使用 Wrapper 或自定义方法
    default List<Resume> findByUserId(Long userId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Resume>()
                .eq("user_id", userId)
                .orderByDesc("upload_date")); // 按上传日期降序
    }
}