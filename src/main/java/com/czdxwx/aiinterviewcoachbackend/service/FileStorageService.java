package com.czdxwx.aiinterviewcoachbackend.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;

public interface FileStorageService {
    void init(); // 初始化存储目录
    String store(MultipartFile file, String subDirectory); // 返回存储后的文件名或路径标识
    // Stream<Path> loadAll(String subDirectory); // (可选) 加载所有文件
    // Path load(String subDirectory, String filename); // (可选) 加载单个文件
    // Resource loadAsResource(String subDirectory, String filename); // (可选) 加载为Resource
    void delete(String subDirectory, String filename);
    // void deleteAll(); // (可选)
}
