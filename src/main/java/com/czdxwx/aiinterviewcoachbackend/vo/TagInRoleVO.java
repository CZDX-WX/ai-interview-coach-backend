package com.czdxwx.aiinterviewcoachbackend.vo;

import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TagInRoleVO extends Tag {
    /**
     * 标记这个标签是否是当前用户为该岗位个性化添加的。
     * true: 是用户自己添加的（前端可以显示“私有”颜色，并提供移除按钮）
     * false: 是该岗位默认自带的公共标签
     */
    private Boolean isUserAddition;
}