package com.czdxwx.aiinterviewcoachbackend.controller;

import com.czdxwx.aiinterviewcoachbackend.config.security.CustomUserDetails;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.service.TagService;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagCreateRequest;
import com.czdxwx.aiinterviewcoachbackend.service.dto.TagSuggestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Handles all HTTP requests related to Tags.
 * Provides endpoints for listing, suggesting, and creating tags.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * Gets the list of available tags for the current user.
     * If a roleId is provided, it returns public tags relevant to that role.
     * Otherwise, it returns all public tags plus the user's private tags.
     * @param currentUser The currently authenticated user, injected by Spring Security.
     * @param roleId (Optional) The ID of the role to filter tags by.
     * @return A list of tags, potentially grouped by category.
     */
    @GetMapping("")
    public ResponseEntity<List<Tag>> getTags(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long roleId) {

        Long userId = (currentUser != null) ? currentUser.getId() : null;
        List<Tag> tags = tagService.getTags(userId, roleId);
        return ResponseEntity.ok(tags);
    }

    /**
     * Provides intelligent suggestions for a new tag name entered by the user.
     * Checks for exact matches and semantically similar tags to avoid duplicates.
     * @param request A DTO containing the tag name to check.
     * @param currentUser The currently authenticated user.
     * @return A TagSuggestionResponse DTO indicating if a match was found and providing the suggestion.
     */
    @PostMapping("/suggest")
    public ResponseEntity<TagSuggestionResponse> suggestTag(
            @RequestBody TagCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        String tagName = request.getName();
        return ResponseEntity.ok(tagService.suggest(tagName, currentUser.getId()));
    }

    /**
     * Creates a new custom tag for the currently authenticated user.
     * This endpoint should be called after the user confirms they want to create a new tag (e.g., after the /suggest endpoint returns NO_SIMILAR_FOUND).
     * @param request A DTO containing the new tag's name and the context roleId to associate it with.
     * @param currentUser The currently authenticated user.
     * @return The newly created Tag object with a 201 Created status.
     */
    @PostMapping("")
    public ResponseEntity<Tag> createTag(
            @Valid @RequestBody TagCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Tag createdTag = tagService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
    }
}