package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.czdxwx.aiinterviewcoachbackend.config.security.SecurityUtils;
import com.czdxwx.aiinterviewcoachbackend.entity.Resume;
import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.mapper.ForumPostMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.ForumThreadMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.ResumeMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.UserMapper;
import com.czdxwx.aiinterviewcoachbackend.service.FileStorageService;
import com.czdxwx.aiinterviewcoachbackend.service.UserProfileService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ChangePasswordRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.ResumeDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UpdateUserProfileRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.UserProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);
    // 从配置文件读取文件上传的基础目录
    @Value("${app.file-storage.upload-dir:./uploads}")
    private String uploadBaseDir;
    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    // 新增注入的 Mapper
    private final ForumThreadMapper forumThreadMapper;
    private final ForumPostMapper forumPostMapper;

    public UserProfileServiceImpl(UserMapper userMapper, ResumeMapper resumeMapper, PasswordEncoder passwordEncoder,
                                  FileStorageService fileStorageService, ForumThreadMapper forumThreadMapper, ForumPostMapper forumPostMapper) {
        this.userMapper = userMapper;
        this.resumeMapper = resumeMapper;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.forumThreadMapper = forumThreadMapper;
        this.forumPostMapper = forumPostMapper;
    }

    private User getCurrentUserEntity() {
        String currentUsername = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalStateException("无法获取当前认证用户信息"));
        return userMapper.findOneByUsernameWithAuthorities(currentUsername) // 确保这个方法加载了所需信息
                .orElseThrow(() -> new RuntimeException("当前用户 '" + currentUsername + "' 在数据库中未找到"));
    }

    private ResumeDto mapToResumeDto(Resume resume) {
        return new ResumeDto(
                String.valueOf(resume.getId()),
                resume.getFileName(),
                dateFormatter.format(resume.getUploadDate()),
                "/api/profile/resumes/download/" + resume.getId(), // 构造一个象征性的下载链接
                resume.getIsDefault()
        );
    }

    /**
     * 将 Resume 实体转换为 ResumeDto，并构建可访问的 URL
     */
    private ResumeDto mapResumeToDto(Resume resume) {
        // 构建前端可直接使用的完整 URL
        String url = "/api/files/" + resume.getStoragePath().replace("\\", "/");

        return new ResumeDto(
                String.valueOf(resume.getId()),
                resume.getFileName(),
                dateFormatter.format(resume.getUploadDate()),
                url, // 使用构建好的 URL
                resume.getIsDefault()
        );
    }

    @Override
    public User updateUserProfile(UpdateUserProfileRequestDto profileDto) {
        User user = getCurrentUserEntity();

        // 仅更新 DTO 中非 null 的字段
        if (profileDto.getFullName() != null) user.setFullName(profileDto.getFullName());
        if (profileDto.getEmail() != null) {
            // 如果允许修改邮箱，需要检查邮箱是否已被其他用户占用
            if (!user.getEmail().equalsIgnoreCase(profileDto.getEmail()) &&
                    userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .eq("LOWER(email)", profileDto.getEmail().toLowerCase())) > 0) {
                throw new IllegalArgumentException("邮箱 '" + profileDto.getEmail() + "' 已被注册。");
            }
            user.setEmail(profileDto.getEmail());
        }
        if (profileDto.getAvatarUrl() != null) user.setAvatarUrl(profileDto.getAvatarUrl()); // 简单URL，实际头像上传应有专门接口
        if (profileDto.getSchool() != null) user.setSchool(profileDto.getSchool());
        if (profileDto.getMajor() != null) user.setMajor(profileDto.getMajor());
        if (profileDto.getGraduationYear() != null) user.setGraduationYear(profileDto.getGraduationYear());

        user.setUpdatedAt(Instant.now());
        userMapper.updateById(user);
        log.info("用户 '{}' 的个人资料已更新", user.getUsername());
        return user;
    }


    /**
     * 获取当前用户的完整个人资料，包括可访问的简历链接
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile() {
        User user = getCurrentUserEntity();
        UserProfileDto dto = new UserProfileDto();
        BeanUtils.copyProperties(user, dto, "id", "fullName", "resumes", "authorities");
        dto.setId(String.valueOf(user.getId()));
        dto.setFullName(user.getFullName());

        List<Resume> resumes = resumeMapper.findByUserId(user.getId());
        // 使用 mapResumeToDto 转换，确保每个简历都有正确的 URL
        dto.setResumes(resumes.stream().map(this::mapResumeToDto).collect(Collectors.toList()));

        // 确保从数据库加载权限
        user.setAuthorities(userMapper.findAuthoritiesByUserId(user.getId()));
        dto.setAuthorities(user.getAuthorities());

        log.debug("获取用户个人资料: {}", user.getUsername());
        return dto;
    }

    /**
     * 上传新简历，并保存包含可访问 URL 的路径
     */
    @Override
    public ResumeDto addResume(MultipartFile file) {
        User user = getCurrentUserEntity();
        // 为简历文件创建一个特定的子目录
        String subDirectory = "resumes/user_" + user.getId();
        String storedRelativePath = fileStorageService.store(file, subDirectory);

        Resume resume = new Resume();
        resume.setUserId(user.getId());
        resume.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
        resume.setStoragePath(storedRelativePath); // 存储包含子目录的相对路径
        resume.setUploadDate(Instant.now());
        resume.setFileSize(file.getSize());
        resume.setContentType(file.getContentType());

        resumeMapper.insert(resume);
        log.info("用户 '{}' 上传了新简历: {}", user.getUsername(), resume.getFileName());

        // 返回包含完整可访问 URL 的 DTO
        return mapResumeToDto(resume);
    }

    @Override
    public void deleteResume(Long resumeId) {
        User user = getCurrentUserEntity();
        log.debug("用户 '{}' 请求删除简历 ID: {}", user.getUsername(), resumeId);

        Resume resume = resumeMapper.selectById(resumeId);

        // 验证简历是否存在，以及是否属于当前登录用户
        if (resume == null || !resume.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("简历未找到或您没有权限删除该简历。");
        }

        // **核心修正点**:
        // resume.getStoragePath() 中存储的已经是我们需要的相对路径 (例如: "resumes/user_1/xxxx.pdf")
        // 我们直接将这个路径传递给更新后的 delete 方法即可。
        // 不再需要手动分割路径。
        try {
            fileStorageService.delete(resume.getStoragePath());
            log.info("已从文件系统删除简历文件: {}", resume.getStoragePath());
        } catch (RuntimeException e) {
            // 即使文件删除失败，也记录错误并继续删除数据库记录，避免留下孤立的文件引用
            log.error("从文件系统删除简历文件 {} 时失败，但将继续从数据库删除记录。", resume.getStoragePath(), e);
        }

        // 从数据库中删除记录
        int deletedRows = resumeMapper.deleteById(resumeId);
        if (deletedRows > 0) {
            log.info("用户 '{}' 已成功删除简历记录 (ID: {})。", user.getUsername(), resumeId);
        } else {
            log.warn("尝试从数据库删除简历记录 (ID: {}) 失败，可能已被删除。", resumeId);
        }
    }

    @Override
    public void changePassword(ChangePasswordRequestDto passwordDto) {
        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确。");
        }
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("新密码和确认密码不匹配。");
        }
        if (passwordDto.getNewPassword().length() < 6) { // 与注册时校验一致
            throw new IllegalArgumentException("新密码长度必须至少为6个字符。");
        }

        user.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userMapper.updateById(user);
        log.info("用户 '{}' 的密码已成功修改", user.getUsername());
    }

    /**
     * 实现账户删除的完整逻辑
     */
    @Override
    public void deleteCurrentUserAccount() {
        User user = getCurrentUserEntity();
        Long userId = user.getId();
        String username = user.getUsername();
        log.warn("开始删除用户账户的流程，用户ID: {}, 用户名: {}", userId, username);

        // 步骤 1: 匿名化该用户在论坛发表的所有内容
        log.debug("正在匿名化用户 {} 的论坛内容...", username);
        try {
            String anonymousName = "已注销用户";
            int anonymizedThreads = forumThreadMapper.anonymizeThreadsByUserId(userId, anonymousName);
            int anonymizedPosts = forumPostMapper.anonymizePostsByUserId(userId, anonymousName);
            log.info("已匿名化用户 {} 的内容：{} 个主题帖，{} 个回复帖。", username, anonymizedThreads, anonymizedPosts);
        } catch (Exception e) {
            // 即使匿名化失败（例如论坛模块尚未完全部署或数据库连接问题），也应记录错误并继续执行删除主账户的关键操作。
            log.error("在匿名化用户 {} 的论坛内容时发生错误，但这不会中断账户删除流程: {}", username, e.getMessage());
        }

        // 步骤 2: 删除用户上传的所有物理文件（例如简历）
        log.debug("正在删除用户 {} 的物理文件...", username);
        try {
            // 构建用户专属的上传目录路径，此处的 "user_" 前缀应与 FileStorageService 中存储文件的逻辑一致。
            Path userUploadDirectory = Paths.get(this.uploadBaseDir).resolve("user_" + userId);
            if (Files.exists(userUploadDirectory)) {
                // FileSystemUtils.deleteRecursively 会递归删除整个文件夹及其内容
                FileSystemUtils.deleteRecursively(userUploadDirectory);
                log.info("已成功删除用户 {} 的文件存储目录: {}", username, userUploadDirectory.toAbsolutePath());
            } else {
                log.info("用户 {} 没有文件存储目录，无需删除。", username);
            }
        } catch (IOException e) {
            log.error("删除用户 {} 的文件时发生IO错误: {}", username, e.getMessage());
            // 根据业务需求，文件删除失败可能需要抛出异常以回滚整个事务。
            // 但通常，即使文件删除失败，也应继续删除数据库中的用户记录，以防止用户无法被删除。
        }

        // 步骤 3: 从数据库删除用户记录
        // 由于我们在相关表（如 resume, user_authority）的外键上设置了 ON DELETE CASCADE，
        // 删除 app_user 表中的记录将自动级联删除这些关联表中的数据。
        log.debug("正在从数据库中删除用户 {} 的记录...", username);
        int deleteCount = userMapper.deleteById(userId);
        if (deleteCount > 0) {
            log.info("已成功从数据库中删除用户 {} (ID: {}) 的主记录及所有级联数据。", username, userId);
        } else {
            // 这种情况理论上不应发生，因为 user 对象是通过认证信息获取的
            log.error("尝试从数据库删除用户 {} (ID: {}) 失败，记录未找到。", username, userId);
            throw new RuntimeException("删除用户失败，用户记录在数据库中不存在。");
        }
    }

    @Override
    public String updateAvatar(MultipartFile file) {
        User user = getCurrentUserEntity();

        // 1. 删除旧头像文件 (逻辑不变)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                // 假设 avatarUrl 存储的是 /api/files/ 开头的URL路径
                String oldFilePath = user.getAvatarUrl().replace("/api/files/", "");
                Path oldAvatarPath = Paths.get(this.uploadBaseDir).resolve(oldFilePath);
                Files.deleteIfExists(oldAvatarPath);
                log.debug("已删除用户 {} 的旧头像: {}", user.getUsername(), oldAvatarPath);
            } catch (Exception e) {
                log.error("删除用户 {} 的旧头像文件失败: {}", user.getUsername(), e.getMessage());
            }
        }

        // 2. 存储新头像文件
        String subDirectory = "avatars/user_" + user.getId();
        String storedRelativePath = fileStorageService.store(file, subDirectory);

        // **核心修改**: 构建一个可以通过API访问的URL路径
        // 注意：路径分隔符应为'/'
        String newAvatarUrl = "/api/files/" + storedRelativePath.replace("\\", "/");

        user.setAvatarUrl(newAvatarUrl);
        userMapper.updateById(user);

        log.info("用户 {} 的头像已更新，新 URL: {}", user.getUsername(), newAvatarUrl);
        return newAvatarUrl;
    }
}