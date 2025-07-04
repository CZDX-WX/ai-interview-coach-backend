package com.czdxwx.aiinterviewcoachbackend.controller;
import com.czdxwx.aiinterviewcoachbackend.config.security.CustomUserDetails;
import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.service.RoleService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RoleCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles") // 这是给普通用户的接口
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("")
    public ResponseEntity<Map<String, List<Role>>> getVisibleRoles(@AuthenticationPrincipal CustomUserDetails currentUser) {
        // 【修改】获取公共岗位和自己的私有岗位
        Map<String, List<Role>> publicRoles = roleService.getPublicGroupedRoles();
        if (currentUser != null) {
            Map<String, List<Role>> privateRoles = roleService.getPrivateGroupedRoles(currentUser.getId());
            // 合并两个Map
            privateRoles.forEach((category, roles) -> publicRoles.merge(category, roles, (oldList, newList) -> {
                oldList.addAll(newList);
                return oldList;
            }));
        }
        return ResponseEntity.ok(publicRoles);
    }

    @PostMapping("")
    public ResponseEntity<Role> createPrivateRole(
            @Valid @RequestBody RoleCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Role createdRole = roleService.createPrivateRole(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deletePrivateRole(
            @PathVariable Long roleId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        roleService.deletePrivateRole(roleId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}