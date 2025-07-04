package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("user_role_tag_additions")
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleTagAddition {
    private Long userId;
    private Long roleId;
    private Long tagId;
}
