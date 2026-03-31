package org.example.coffee.repository;

import org.example.coffee.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    @Query("select c from CommentEntity c where c.productId = :productId order by c.createAt desc")
    Page<CommentEntity> findAllByProductId(Long productId, Pageable pageable);

    List<CommentEntity> findAllByProductId(Long productId);

    Boolean existsByUserIdAndProductSizeId(Long userId, Long productSizeId);
}
