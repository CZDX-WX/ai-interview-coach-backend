package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.service.UserProfileService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ChangePasswordRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ResumeDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UpdateUserProfileRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UserProfileDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 用于方法级别安全
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()") // 确保所有接口都需要认证
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile() {
        log.debug("REST 请求：获取当前用户个人资料");
        UserProfileDto userProfile = userProfileService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UpdateUserProfileRequestDto profileDto) {
        log.debug("REST 请求：更新用户个人资料: {}", profileDto.getFullName());
        try {
            userProfileService.updateUserProfile(profileDto);
            return ResponseEntity.ok(Map.of("message", "个人资料更新成功。"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("更新个人资料时出错: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "更新个人资料时发生内部错误。"));
        }
    }

    @PostMapping("/resumes")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        log.debug("REST 请求：上传简历，文件名: {}", file.getOriginalFilename());
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传文件不能为空。"));
        }
        // 可以在这里添加更严格的文件类型和大小校验
        try {
            ResumeDto newResume = userProfileService.addResume(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(newResume);
        } catch (RuntimeException e) {
            log.error("上传简历失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "上传简历失败：" + e.getMessage()));
        }
    }

    @DeleteMapping("/resumes/{resumeId}")
    public ResponseEntity<?> deleteResume(@PathVariable Long resumeId) {
        log.debug("REST 请求：删除简历 ID: {}", resumeId);
        try {
            userProfileService.deleteResume(resumeId);
            return ResponseEntity.ok(Map.of("message", "简历删除成功。"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("删除简历失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "删除简历时发生内部错误。"));
        }
    }

    // 注意：下载简历的接口 (/api/profile/resumes/download/{resumeId}) 需要 FileStorageService 配合返回 Resource
    // 这里暂时不实现，您可以后续添加

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequestDto passwordDto) {
        log.debug("REST 请求：修改密码");
        try {
            userProfileService.changePassword(passwordDto);
            return ResponseEntity.ok(Map.of("message", "密码修改成功。下次登录请使用新密码。"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("修改密码时出错: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "修改密码时发生内部错误。"));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUserAccount() {
        log.debug("REST 请求：删除当前用户账户");
        try {
            // 调用 Service 层来处理删除逻辑
            userProfileService.deleteCurrentUserAccount();
            return ResponseEntity.ok(Map.of("message", "账户删除成功。"));
        } catch (Exception e) {
            log.error("删除账户时出错: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "删除账户时发生内部错误。"));
        }
    }

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatarFile") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传的头像文件不能为空。"));
        }
        try {
            // 调用 Service 层处理文件存储和数据库更新
            String avatarUrl = userProfileService.updateAvatar(file);
            // 返回新的头像 URL，以便前端更新
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            log.error("上传头像失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "上传头像时发生内部错误。"));
        }
    }
}