package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.entity.Role;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.service.RoleService;
import com.czdxwx.aiinterviewcoachbackend.service.TagService;
import com.czdxwx.aiinterviewcoachbackend.service.TtsService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RoleCreateRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 【重要】整个Controller都需要ADMIN角色权限
public class AdminController {

    private final RoleService roleService;
    private final TagService tagService;

    @PostMapping("")
    public ResponseEntity<Role> createPublicRole(@Valid @RequestBody RoleCreateRequest request) {
        Role createdRole = roleService.createPublicRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deletePublicRole(@PathVariable Long roleId) {
        roleService.deletePublicRole(roleId);
        return ResponseEntity.noContent().build();
    }

    // --- 标签管理 ---
    @PostMapping("/tags")
    public ResponseEntity<Tag> createPublicTag(@Valid @RequestBody TagCreateRequest request) {
        Tag createdTag = tagService.createPublicTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
    }

    @DeleteMapping("/tags/{tagId}")
    public ResponseEntity<Void> deletePublicTag(@PathVariable Long tagId) {
        tagService.deletePublicTag(tagId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 为公共岗位关联一个公共标签
     */
    @PostMapping("/role-tags")
    public ResponseEntity<Void> associateTagToRole(@RequestBody Map<String, Long> payload) {
        Long roleId = payload.get("roleId");
        Long tagId = payload.get("tagId");
        tagService.associateTagToRole(roleId, tagId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}