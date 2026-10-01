package com.spendwise.controller;

import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.request.SaveCategoryRequest;
import com.spendwise.dto.request.UpdateCategoryRequest;
import com.spendwise.dto.response.CategoryDto;
import com.spendwise.dto.service.CategoryService;
import com.spendwise.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public CategoryController(CategoryService categoryService, CurrentUserService currentUserService) {
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    /**
     * Lists categories available to the authenticated user.
     *
     * @return all categories visible to the current user
     */
    @GetMapping
    public List<CategoryDto> list() {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Listing categories for userId={}", user.getId());
        return categoryService.list(user).stream().map(CategoryDto::from).toList();
    }

    /**
     * Creates a category for the authenticated user.
     *
     * @param request contains the category name and icon metadata
     * @return the created category
     */
    @PostMapping
    public CategoryDto create(@Valid @RequestBody SaveCategoryRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Creating category for userId={} name={}", user.getId(), request.getName());
        return CategoryDto.from(categoryService.create(user, request.getName(), request.getIcon()));
    }

    /**
     * Updates a category owned by the authenticated user.
     *
     * @param id the category identifier
     * @param request contains the new category state to persist
     * @return the updated category
     */
    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        UserProfile user = currentUserService.getCurrentUser();
        log.info("Updating category for userId={} categoryId={}", user.getId(), id);
        return CategoryDto.from(categoryService.update(user, id, request.getName(), request.getIcon(), request.isActive()));
    }
}
