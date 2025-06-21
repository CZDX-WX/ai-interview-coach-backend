package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.czdxwx.aiinterviewcoachbackend.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileSystemStorageService implements FileStorageService {

    @Value("${app.file-storage.upload-dir:./uploads}") // 从配置文件读取，默认为 ./uploads
    private String uploadBaseDir;
    private Logger logger = LoggerFactory.getLogger(FileSystemStorageService.class);
    private Path rootLocation;

    @Override
    @PostConstruct // Bean 初始化时执行
    public void init() {
        this.rootLocation = Paths.get(uploadBaseDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法初始化文件存储位置！", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new RuntimeException("无法存储空文件。");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetDirectory = this.rootLocation.resolve(subDirectory);
            Files.createDirectories(targetDirectory); // 确保子目录存在

            Path destinationFile = targetDirectory.resolve(storedFilename)
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(targetDirectory.toAbsolutePath())) {
                throw new RuntimeException("无法将文件存储在指定目录之外。");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            // 返回相对于 base upload dir 的路径，包含子目录和新文件名
            return Paths.get(subDirectory, storedFilename).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("存储文件失败：" + originalFilename, e);
        }
    }

    // **新增**: store(byte[]) 方法的实现
    @Override
    public String store(byte[] data, String subDirectory, String fileExtension) {
        if (data == null || data.length == 0) {
            throw new RuntimeException("无法存储空的字节数组。");
        }
        // 使用 UUID 生成唯一文件名，确保不会重复
        String storedFilename = UUID.randomUUID().toString() + (fileExtension.startsWith(".") ? fileExtension : "." + fileExtension);

        try {
            Path targetDirectory = this.rootLocation.resolve(subDirectory);
            Files.createDirectories(targetDirectory);

            Path destinationFile = targetDirectory.resolve(storedFilename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(targetDirectory.toAbsolutePath())) {
                throw new RuntimeException("无法将文件存储在指定目录之外。");
            }

            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 返回包含子目录的相对路径
            return Paths.get(subDirectory, storedFilename).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("存储字节数据失败！", e);
        }
    }

    // **修改**: delete 方法接收包含子目录的相对路径
    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }
        try {
            Path fileToDelete = this.rootLocation.resolve(relativePath).normalize().toAbsolutePath();
            // 安全检查，确保删除操作不会超出根上传目录
            if (!fileToDelete.startsWith(this.rootLocation.toAbsolutePath())) {
                logger.warn("尝试删除根上传目录之外的文件，操作被拒绝: {}", fileToDelete);
                return;
            }
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            logger.error("删除文件失败: {}", relativePath, e);
            // 在生产环境中，可能不希望此异常中断整个流程，只记录日志
        }
    }
}