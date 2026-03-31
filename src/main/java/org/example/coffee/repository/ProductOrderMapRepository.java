package org.example.coffee.repository;

import org.example.coffee.entity.ProductOrderMapEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOrderMapRepository extends JpaRepository<ProductOrderMapEntity, Long> {
    Page<ProductOrderMapEntity> findAllByOrderIdIn(List<Long> orderId, Pageable pageable);

    List<ProductOrderMapEntity> findAllByOrderId(Long orderId);

    @Query("SELECT p.nameProduct, p.image, p.size, SUM(p.quantityOrder), SUM(p.totalPrice) " +
            "FROM ProductOrderMapEntity p GROUP BY p.nameProduct, p.image, p.size " +
            "ORDER BY SUM(p.quantityOrder) DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);

    @Query("SELECT COUNT(p) > 0 FROM ProductOrderMapEntity p " +
            "JOIN UserOrderEntity o ON p.orderId = o.id " +
            "WHERE o.userId = :userId AND p.productSizeId = :productSizeId AND o.state = 'COMPLETED'")
    Boolean existsPurchase(@org.springframework.data.repository.query.Param("userId") Long userId,
                           @org.springframework.data.repository.query.Param("productSizeId") Long productSizeId);
}
