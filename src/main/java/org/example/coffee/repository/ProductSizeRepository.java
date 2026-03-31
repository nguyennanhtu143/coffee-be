package org.example.coffee.repository;

import org.example.coffee.entity.ProductSizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSizeEntity, Long> {
    List<ProductSizeEntity> findAllByProductId(Long productId);

    List<ProductSizeEntity> findAllByProductIdIn(List<Long> productIds);

    Optional<ProductSizeEntity> findByProductIdAndSize(Long productId, String size);

    void deleteAllByProductId(Long productId);

    Boolean existsByProductIdAndSize(Long productId, String size);
}
