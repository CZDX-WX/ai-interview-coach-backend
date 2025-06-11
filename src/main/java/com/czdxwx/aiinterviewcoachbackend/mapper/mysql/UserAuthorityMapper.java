package com.czdxwx.aiinterviewcoachbackend.mapper.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAuthorityMapper {
    @Insert("INSERT INTO user_authority (user_id, authority_name) VALUES (#{userId}, #{authorityName})")
    void insertUserAuthority(@Param("userId") Long userId, @Param("authorityName") String authorityName);
}