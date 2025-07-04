package com.czdxwx.aiinterviewcoachbackend.service;

import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RoleCreateRequest;
import java.util.List;
import java.util.Map;

public interface RoleService {

    /**
     * 获取所有公共角色
     */
    Map<String, List<Role>> getPublicGroupedRoles();

    /**
     * 获取指定用户的私有角色
     */
    Map<String, List<Role>> getPrivateGroupedRoles(Long userId);

    /**
     * 创建一个公共角色
     */
    Role createPublicRole(RoleCreateRequest request);

    /**
     * 为指定用户创建一个私有角色
     */
    Role createPrivateRole(RoleCreateRequest request, Long userId);

    /**
     * 删除一个公共角色
     */
    void deletePublicRole(Long roleId);

    /**
     * 删除一个用户的私有角色
     */
    void deletePrivateRole(Long roleId, Long userId);

    Role findByName(String roleName);
}