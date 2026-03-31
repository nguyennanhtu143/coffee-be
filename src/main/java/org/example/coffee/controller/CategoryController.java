package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.category.CategoryInput;
import org.example.coffee.dto.category.CategoryOutput;
import org.example.coffee.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor

@RequestMapping("/api/v1/category")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "Tạo category")
    @PostMapping("/create")
    public void createCategory(@RequestHeader("Authorization") String accessToken,
                               @Valid @RequestBody CategoryInput categoryInput) {
        categoryService.createCategory(accessToken, categoryInput);
    }

    @Operation(summary = "Lấy ra danh mục")
    @GetMapping("/get-categories")
    public List<CategoryOutput> getCategories() {
        return categoryService.getCategories();
    }

    @Operation(summary = "Lấy ra category")
    @GetMapping("/get")
    public CategoryOutput getCategory(@RequestParam Long categoryId) {
        return categoryService.getCategory(categoryId);
    }

    @Operation(summary = "Xóa category")
    @DeleteMapping("/delete")
    public void deleteCategory(@RequestParam Long categoryId,
                               @RequestHeader("Authorization") String accessToken) {
        categoryService.deleteCategory(categoryId, accessToken);
    }
}
