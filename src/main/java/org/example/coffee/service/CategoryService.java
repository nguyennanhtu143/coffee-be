package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.category.CategoryInput;
import org.example.coffee.dto.category.CategoryOutput;
import org.example.coffee.entity.CategoryEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.CategoryRepository;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.ProductCategoryRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CustomRepository customRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryOutput> getCategories() {
        List<CategoryEntity> categories = categoryRepository.findAll();
        List<CategoryOutput> categoriesOutput = new ArrayList<>();
        for (CategoryEntity category : categories) {
            CategoryOutput categoryOutput = CategoryOutput.builder()
                    .categoryId(category.getId())
                    .name(category.getName())
                    .build();

            categoriesOutput.add(categoryOutput);
        }
        return categoriesOutput;
    }

    @Transactional
    public void createCategory(String accessToken, CategoryInput categoryInput) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        if (userEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        CategoryEntity categoryEntity = CategoryEntity.builder()
                .name(categoryInput.getName())
                .build();
        categoryRepository.save(categoryEntity);
    }

    @Transactional(readOnly = true)
    public CategoryOutput getCategory(Long categoryId) {
        CategoryEntity categoryEntity = customRepository.getCategoryBy(categoryId);
        return CategoryOutput.builder()
                .categoryId(categoryEntity.getId())
                .name(categoryEntity.getName())
                .build();
    }

    @Transactional
    public void deleteCategory(Long categoryId, String accessToken) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        if (userEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        categoryRepository.deleteById(categoryId);
        productCategoryRepository.deleteAllByCategoryId(categoryId);
    }
}
