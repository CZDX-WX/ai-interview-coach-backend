package com.czdxwx.aiinterviewcoachbackend.service.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class UserProfileDto {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String school;
    private String major;
    private String graduationYear;
    private Set<String> authorities;
    private List<ResumeDto> resumes; // 包含用户的简历信息
}