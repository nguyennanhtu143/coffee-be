package org.example.coffee.repository;

import org.example.coffee.entity.UserOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
