package com.czdxwx.aiinterviewcoachbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.czdxwx.aiinterviewcoachbackend.model.enums.ProficiencyStatus;
import lombok.Data;
import org.apache.ibatis.type.EnumTypeHandler;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName user_question_status
 */
@TableName(value ="user_question_status")
@Data
public class UserQuestionStatus implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("question_id")
    private Long questionId;

    @TableField(value = "proficiency_status", typeHandler = EnumTypeHandler.class)
    private ProficiencyStatus proficiencyStatus;

    @TableField("last_practiced_at")
    private Date lastPracticedAt;

    @TableField("notes")
    private String notes;
    @TableField("is_bookmarked")
    private Boolean isBookmarked;
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        UserQuestionStatus other = (UserQuestionStatus) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getQuestionId() == null ? other.getQuestionId() == null : this.getQuestionId().equals(other.getQuestionId()))
            && (this.getProficiencyStatus() == null ? other.getProficiencyStatus() == null : this.getProficiencyStatus().equals(other.getProficiencyStatus()))
            && (this.getLastPracticedAt() == null ? other.getLastPracticedAt() == null : this.getLastPracticedAt().equals(other.getLastPracticedAt()))
            && (this.getNotes() == null ? other.getNotes() == null : this.getNotes().equals(other.getNotes()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getQuestionId() == null) ? 0 : getQuestionId().hashCode());
        result = prime * result + ((getProficiencyStatus() == null) ? 0 : getProficiencyStatus().hashCode());
        result = prime * result + ((getLastPracticedAt() == null) ? 0 : getLastPracticedAt().hashCode());
        result = prime * result + ((getNotes() == null) ? 0 : getNotes().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", questionId=").append(questionId);
        sb.append(", proficiencyStatus=").append(proficiencyStatus);
        sb.append(", lastPracticedAt=").append(lastPracticedAt);
        sb.append(", notes=").append(notes);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}