package org.example.coffee.repository;

import org.example.coffee.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
	@Query("select p from ProductEntity p order by p.createdAt desc")
	Page<ProductEntity> findAll(Pageable pageable);

	Page<ProductEntity> findAllByIdIn(List<Long> productIds, Pageable pageable);

	List<ProductEntity> findAllByIdIn(List<Long> productIds);

	List<ProductEntity> findAllByIdNotIn(List<Long> productIds);

	@Query("SELECT u FROM ProductEntity u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))")
	Page<ProductEntity> searchProductEntitiesByString(@Param("search") String search, Pageable pageable);

	@Query("""
			SELECT p FROM ProductEntity p
			WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
			AND (:categoryId IS NULL OR p.id IN (
				SELECT pcm.productId FROM ProductCategoryMapEntity pcm WHERE pcm.categoryId = :categoryId
			))
			ORDER BY
			CASE WHEN :sortBy = 'name' AND :direction = 'asc' THEN p.name END ASC,
			CASE WHEN :sortBy = 'name' AND :direction = 'desc' THEN p.name END DESC,
			CASE WHEN :sortBy = 'createdAt' AND :direction = 'asc' THEN p.createdAt END ASC,
			CASE WHEN :sortBy = 'createdAt' AND :direction = 'desc' THEN p.createdAt END DESC,
			CASE WHEN :sortBy = 'price' AND :direction = 'asc' THEN (
				SELECT MIN(ps.price) FROM ProductSizeEntity ps WHERE ps.productId = p.id
			) END ASC,
			CASE WHEN :sortBy = 'price' AND :direction = 'desc' THEN (
				SELECT MIN(ps.price) FROM ProductSizeEntity ps WHERE ps.productId = p.id
			) END DESC,
			p.createdAt DESC
			""")
	Page<ProductEntity> searchAdminProducts(@Param("search") String search,
											@Param("categoryId") Long categoryId,
											@Param("sortBy") String sortBy,
											@Param("direction") String direction,
											Pageable pageable);
}
