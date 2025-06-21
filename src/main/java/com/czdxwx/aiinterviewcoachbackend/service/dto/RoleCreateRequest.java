package com.czdxwx.aiinterviewcoachbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用于接收创建新角色（岗位）请求的数据传输对象
 */
@Data
public class RoleCreateRequest {

    /**
     * 新岗位的名称
     */
    @NotBlank(message = "岗位名称不能为空")
    private String name;

    /**
     * 新岗位所属的类别 (可选)
     * 如果不提供，service层会给一个默认值 "自定义岗位"
     */
    private String category;
}