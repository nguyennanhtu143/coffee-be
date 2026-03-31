package org.example.coffee.repository;

import org.example.coffee.entity.ProductCategoryMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategoryMapEntity, Long> {
    List<ProductCategoryMapEntity> findAllByCategoryId(Long categoryId);

    void deleteByCategoryIdAndProductId(Long categoryId, Long productId);

    void deleteAllByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndProductId(Long categoryId, Long productId);

    void deleteAllByProductId(Long productId);
}
