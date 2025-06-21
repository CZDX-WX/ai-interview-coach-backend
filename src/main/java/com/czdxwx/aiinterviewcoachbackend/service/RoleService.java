package com.czdxwx.aiinterviewcoachbackend.service;

import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RoleCreateRequest;
import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 */
public interface RoleService {
    /**
     * 获取对用户可见的所有角色，并按类别分组
     */
    Map<String, List<Role>> getGroupedRoles(Long userId);

    /**
     * 为用户创建新的个性化岗位
     */
    Role create(RoleCreateRequest request, Long userId);

    /**
     * [供内部使用] 根据名称查找角色
     */
    Role findByName(String roleName);
}