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

/**
 * Handles all HTTP requests related to Roles.
 * Provides endpoints for listing available roles and creating custom ones.
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * Gets all available roles, grouped by category.
     * This includes all public roles plus any private roles belonging to the currently authenticated user.
     * @param currentUser The currently authenticated user, injected by Spring Security. Can be null if the endpoint is accessed anonymously.
     * @return A map where keys are category names and values are lists of roles in that category.
     */
    @GetMapping("")
    public ResponseEntity<Map<String, List<Role>>> getGroupedRoles(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Long userId = (currentUser != null) ? currentUser.getId() : null;
        return ResponseEntity.ok(roleService.getGroupedRoles(userId));
    }

    /**
     * Creates a new custom role for the currently authenticated user.
     * @param request A DTO containing the new role's name and optional category.
     * @param currentUser The currently authenticated user (required).
     * @return The newly created Role object with a 201 Created status.
     */
    @PostMapping("")
    public ResponseEntity<Role> createCustomRole(
            @Valid @RequestBody RoleCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // The service layer will handle the logic of checking for duplicates before creation.
        Role createdRole = roleService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }
}