package org.example.coffee.repository;

import org.example.coffee.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    @Query("select p from ProductEntity p order by p.createdAt desc")
    Page<ProductEntity> findAll(Pageable pageable);

    Page<ProductEntity> findAllByIdIn(List<Long> productIds, Pageable pageable);

    List<ProductEntity> findAllByIdIn(List<Long> productIds);

    List<ProductEntity> findAllByIdNotIn(List<Long> productIds);

    @Query("SELECT u FROM ProductEntity u WHERE u.name LIKE %?1%")
    Page<ProductEntity> searchProductEntitiesByString(String search, Pageable pageable);
}
