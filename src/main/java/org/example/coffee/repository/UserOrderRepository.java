package org.example.coffee.repository;

import org.example.coffee.entity.UserOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserOrderRepository extends JpaRepository<UserOrderEntity, Long> {
    List<UserOrderEntity> findAllByUserIdAndState(Long userId, String state);

    List<UserOrderEntity> findAllByState(String state);

    @Query("SELECT COUNT(o) FROM UserOrderEntity o WHERE o.state = :state")
    Long countByState(@Param("state") String state);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM UserOrderEntity o WHERE o.state = 'COMPLETED'")
    Long sumTotalRevenueCompleted();

    Long countAllBy();

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM UserOrderEntity o WHERE o.state = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end")
    Long sumRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM UserOrderEntity o WHERE o.state = 'COMPLETED' AND o.createdAt BETWEEN :start AND :end")
    Long countOrdersByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    UserOrderEntity findByGhnOrderCode(String ghnOrderCode);

    @Query("""
            SELECT o FROM UserOrderEntity o
            WHERE (:state IS NULL OR o.state = :state)
            AND (:orderId IS NULL OR o.id = :orderId)
            AND (:phoneNumber IS NULL OR o.phoneNumber LIKE CONCAT('%', :phoneNumber, '%'))
            AND (:createdFrom IS NULL OR o.createdAt >= :createdFrom)
            AND (:createdTo IS NULL OR o.createdAt <= :createdTo)
            ORDER BY o.createdAt DESC
            """)
    Page<UserOrderEntity> searchAdminOrders(@Param("state") String state,
                                            @Param("orderId") Long orderId,
                                            @Param("phoneNumber") String phoneNumber,
                                            @Param("createdFrom") LocalDateTime createdFrom,
                                            @Param("createdTo") LocalDateTime createdTo,
                                            Pageable pageable);
}
