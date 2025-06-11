package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("forum_category")
public class ForumCategory implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private Integer threadCount;
    private Integer postCount;
    private Long lastThreadId; // 用于在Service层查询最新的主题信息
}