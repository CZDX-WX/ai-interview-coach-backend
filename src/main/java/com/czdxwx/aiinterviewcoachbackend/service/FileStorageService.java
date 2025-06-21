package com.czdxwx.aiinterviewcoachbackend.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;

public interface FileStorageService {
    void init(); // 初始化存储目录
    String store(MultipartFile file, String subDirectory); // 返回存储后的文件名或路径标识
    // **新增**: 一个接收字节数组的 store 方法
    String store(byte[] data, String subDirectory, String fileExtension);
    void delete(String relativePath); // 稍微修改 delete 方法，使其更通用
}
