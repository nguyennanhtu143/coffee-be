package org.example.coffee.repository;

import org.example.coffee.entity.ProductPageConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPageConfigRepository extends JpaRepository<ProductPageConfigEntity, Long> {
}
