package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.czdxwx.aiinterviewcoachbackend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
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

    @Override
    public void delete(String subDirectory, String filename) {
        try {
            Path file = this.rootLocation.resolve(subDirectory).resolve(filename).normalize().toAbsolutePath();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("删除文件失败: " + filename, e);
        }
    }
    // 其他方法 (loadAll, load, loadAsResource) 可以根据需要实现
}