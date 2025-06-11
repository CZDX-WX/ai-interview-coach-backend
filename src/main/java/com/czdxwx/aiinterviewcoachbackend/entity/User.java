package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@ToString(exclude = "passwordHash")
@TableName("app_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("full_name")
    private String fullName;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("school")
    private String school;

    @TableField("major")
    private String major;

    @TableField("graduation_year")
    private String graduationYear;

    @TableField("enabled")
    private boolean enabled = true;

    @TableField(value = "created_at", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT) // 假设有自动填充
    private Instant createdAt;

    @TableField(value = "updated_at", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE) // 假设有自动填充
    private Instant updatedAt;

    // 权限/角色列表, 通过 user_authority 表关联
    @TableField(exist = false) // 不直接映射到 app_user 表的列
    private Set<String> authorities = new HashSet<>();

    // 简历列表, 通过关联查询填充
    @TableField(exist = false)
    private List<Resume> resumes = new ArrayList<>(); // 初始化为空列表
}