package com.czdxwx.aiinterviewcoachbackend.service.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用于创建新标签的请求体
 */
@Data
public class TagCreateRequest {

    /**
     * 用户输入的新标签的名称
     */
    @NotBlank(message = "标签名不能为空")
    private String name;

    /**
     * 【关键】用户创建此标签时，所在的岗位上下文ID。
     * 这个ID可以是公共岗位的ID，也可以是用户自定义岗位的ID。
     */
    private Long roleId;
}