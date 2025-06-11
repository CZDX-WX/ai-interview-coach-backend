package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@TableName("resume")
public class Resume implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("file_name")
    private String fileName;

    @TableField("storage_path")
    private String storagePath; // 存储路径或标识符

    @TableField("upload_date")
    private Instant uploadDate;

    @TableField("file_size")
    private Long fileSize;

    @TableField("content_type")
    private String contentType;

    @TableField("is_default")
    private Boolean isDefault = false;


}