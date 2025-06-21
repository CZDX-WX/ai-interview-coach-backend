package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.mapper.RoleMapper;
import com.czdxwx.aiinterviewcoachbackend.service.RoleService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RoleCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public Map<String, List<Role>> getGroupedRoles(Long userId) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNull("owner_id").or().eq(userId != null, "owner_id", userId);
        List<Role> roles = roleMapper.selectList(queryWrapper);
        return roles.stream().collect(Collectors.groupingBy(Role::getCategory));
    }

    @Override
    public Role create(RoleCreateRequest request, Long userId) {
        if (userId == null) {
            throw new SecurityException("只有登录用户才能创建自定义岗位");
        }
        Role existingRole = findByName(request.getName());
        if (existingRole != null && (existingRole.getOwnerId() == null || existingRole.getOwnerId().equals(userId))) {
            throw new IllegalArgumentException(String.format("岗位 '%s' 已存在，无需重复创建。", request.getName()));
        }

        Role newRole = new Role();
        newRole.setName(request.getName());
        newRole.setOwnerId(userId);
        newRole.setCategory(request.getCategory() != null ? request.getCategory() : "自定义岗位");
        newRole.setCreatedAt(new Date());
        roleMapper.insert(newRole);
        return newRole;
    }

    @Override
    public Role findByName(String roleName) {
        return roleMapper.selectOne(new QueryWrapper<Role>().eq("name", roleName).isNull("owner_id"));
    }
}
