package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.mapper.RoleMapper;
import com.czdxwx.aiinterviewcoachbackend.mapper.TagMapper;
import com.czdxwx.aiinterviewcoachbackend.service.RoleService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RoleCreateRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public Map<String, List<Role>> getPublicGroupedRoles() {
        List<Role> roles = roleMapper.selectList(new QueryWrapper<Role>().isNull("owner_id"));
        return roles.stream().collect(Collectors.groupingBy(Role::getCategory));
    }

    @Override
    public Map<String, List<Role>> getPrivateGroupedRoles(Long userId) {
        List<Role> roles = roleMapper.selectList(new QueryWrapper<Role>().eq("owner_id", userId));
        return roles.stream().collect(Collectors.groupingBy(Role::getCategory));
    }

    @Override
    @Transactional
    public Role createPublicRole(RoleCreateRequest request) {
        if (roleMapper.exists(new QueryWrapper<Role>().eq("name", request.getName()).isNull("owner_id"))) {
            throw new IllegalArgumentException(String.format("公共岗位 '%s' 已存在。", request.getName()));
        }
        Role role = new Role();
        role.setName(request.getName());
        role.setCategory(request.getCategory());
        role.setOwnerId(null); // 明确设置为 NULL
        role.setCreatedAt(new Date());
        roleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional
    public Role createPrivateRole(RoleCreateRequest request, Long userId) {
        if (roleMapper.exists(new QueryWrapper<Role>().eq("name", request.getName()).eq("owner_id", userId))) {
            throw new IllegalArgumentException(String.format("您已创建过岗位 '%s'。", request.getName()));
        }
        Role role = new Role();
        role.setName(request.getName());
        role.setOwnerId(userId);
        role.setCategory(request.getCategory() != null ? request.getCategory() : "自定义岗位");
        role.setCreatedAt(new Date());
        roleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional
    public void deletePublicRole(Long roleId) {
        roleMapper.delete(new QueryWrapper<Role>().eq("id", roleId).isNull("owner_id"));
    }

    @Override
    @Transactional
    public void deletePrivateRole(Long roleId, Long userId) {
        int deletedRows = roleMapper.delete(new QueryWrapper<Role>().eq("id", roleId).eq("owner_id", userId));
        if (deletedRows == 0) {
            throw new AccessDeniedException("删除失败：岗位不存在或您没有权限。");
        }
    }

    @Override
    public Role findByName(String roleName) {
        return roleMapper.selectOne(new QueryWrapper<Role>().eq("name", roleName).isNull("owner_id"));
    }
}