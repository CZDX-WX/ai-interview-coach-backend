package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("forum_thread")
public class ForumThread implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long authorId;
    private String authorName;
    private String title;
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
    private Integer replyCount;
    private Integer viewCount;
    private Instant lastReplyAt;
    private Long lastReplyAuthorId;
    private String lastReplyAuthorName;
    private Boolean isPinned;
    private Boolean isLocked;
}