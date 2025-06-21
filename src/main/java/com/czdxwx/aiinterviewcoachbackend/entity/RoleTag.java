package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@TableName("role_tags")
@NoArgsConstructor
@AllArgsConstructor
public class RoleTag implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long tagId;
}