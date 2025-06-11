package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {
    private String id; // String 类型，方便前端处理，后端可以是 Long
    private String name; // fileName
    private String uploadDate; // 格式化后的日期字符串 "YYYY-MM-DD"
    private String url; // 可选，下载链接或查看链接
    private Boolean isDefault;
}