package org.example.coffee.repository;

import org.example.coffee.entity.UserOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrderRepository extends JpaRepository<UserOrderEntity, Long>, JpaSpecificationExecutor<UserOrderEntity> {
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

    Optional<UserOrderEntity> findByGhnOrderCode(String ghnOrderCode);

    List<UserOrderEntity> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserOrderEntity> findByStateIn(List<String> states);

    @Query("SELECT COUNT(o) FROM UserOrderEntity o WHERE o.state = :state AND o.createdAt BETWEEN :start AND :end")
    Long countByStateAndDateRange(@Param("state") String state,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM UserOrderEntity o WHERE o.createdAt BETWEEN :start AND :end")
    Long countAllByDateRange(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    /**
     * Đếm số đơn theo từng trạng thái của một user.
     * Trả về List<Object[]> với mỗi phần tử là [state (String), count (Long)].
     * Dùng cho chatbot để trả lời chính xác "Tôi có mấy đơn đang giao?" v.v.
     */
    @Query("SELECT o.state, COUNT(o) FROM UserOrderEntity o WHERE o.userId = :userId GROUP BY o.state")
    List<Object[]> countByStateForUser(@Param("userId") Long userId);

    /**
     * Lấy danh sách đơn đang hoạt động (chưa kết thúc) của user theo nhiều state.
     * Dùng để liệt kê mã đơn cụ thể cho chatbot tham chiếu.
     */
    List<UserOrderEntity> findByUserIdAndStateInOrderByCreatedAtDesc(Long userId, List<String> states);
}
