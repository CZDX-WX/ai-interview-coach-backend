package com.czdxwx.aiinterviewcoachbackend.service;

import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ChangePasswordRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ResumeDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UpdateUserProfileRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UserProfileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserProfileService {
    UserProfileDto getCurrentUserProfile();
    User updateUserProfile(UpdateUserProfileRequestDto profileDto);
    ResumeDto addResume(MultipartFile file);
    void deleteResume(Long resumeId); // 使用 Long 类型的 resumeId
    void changePassword(ChangePasswordRequestDto passwordDto);
    void deleteCurrentUserAccount(); // 新增删除账户方法
    String updateAvatar(MultipartFile file);
}