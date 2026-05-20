package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.statistic.*;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.ProductOrderMapRepository;
import org.example.coffee.repository.UserOrderRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class StatisticService {
    private final UserOrderRepository userOrderRepository;
    private final ProductOrderMapRepository productOrderMapRepository;
    private final CustomRepository customRepository;

    private static final int MAX_RANGE_DAYS = 184; // ~6 tháng
    private static final String[] OVERVIEW_STATES = {
            Common.PENDING_PAYMENT, Common.CONFIRMED, Common.SHIPPING,
            Common.COMPLETED, Common.CANCELED
    };

    private void verifyShopAccess(String accessToken) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
    }

    /**
     * Validate và trả về [startOfDay, endOfDay].
     * Nếu cả 2 null → trả về null (dùng all-time query).
     * Nếu 1 trong 2 null → ném exception.
     */
    private LocalDateTime[] resolveRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) return null;
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Phải cung cấp cả startDate và endDate.");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate phải trước hoặc bằng endDate.");
        }
        long days = endDate.toEpochDay() - startDate.toEpochDay();
        if (days > MAX_RANGE_DAYS) {
            throw new BadRequestException("Khoảng thời gian tối đa là 6 tháng (" + MAX_RANGE_DAYS + " ngày).");
        }
        return new LocalDateTime[]{startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)};
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StatisticOverviewOutput getOverview(String accessToken, LocalDate startDate, LocalDate endDate) {
        verifyShopAccess(accessToken);
        LocalDateTime[] range = resolveRange(startDate, endDate);

        // Revenue & total orders — lọc theo date range nếu có
        Long totalRevenue;
        Long totalOrders;
        Long totalCompleted;
        Long totalCanceled;
        List<OrderStateCountOutput> ordersByState = new ArrayList<>();

        if (range == null) {
            // All-time
            totalRevenue  = userOrderRepository.sumTotalRevenueCompleted();
            totalOrders   = userOrderRepository.countAllBy();
            totalCompleted = userOrderRepository.countByState(Common.COMPLETED);
            totalCanceled  = userOrderRepository.countByState(Common.CANCELED);
            for (String state : OVERVIEW_STATES) {
                ordersByState.add(OrderStateCountOutput.builder()
                        .state(state)
                        .count(userOrderRepository.countByState(state))
                        .build());
            }
        } else {
            LocalDateTime start = range[0], end = range[1];
            totalRevenue  = userOrderRepository.sumRevenueByDateRange(start, end);
            totalOrders   = userOrderRepository.countAllByDateRange(start, end);
            totalCompleted = userOrderRepository.countByStateAndDateRange(Common.COMPLETED, start, end);
            totalCanceled  = userOrderRepository.countByStateAndDateRange(Common.CANCELED, start, end);
            for (String state : OVERVIEW_STATES) {
                ordersByState.add(OrderStateCountOutput.builder()
                        .state(state)
                        .count(userOrderRepository.countByStateAndDateRange(state, start, end))
                        .build());
            }
        }

        // Top products — luôn all-time, không lọc theo date range
        List<Object[]> topProductsRaw = productOrderMapRepository.findTopSellingProducts(PageRequest.of(0, 10));
        List<TopProductOutput> topProducts = new ArrayList<>();
        for (Object[] row : topProductsRaw) {
            topProducts.add(TopProductOutput.builder()
                    .productName((String) row[0])
                    .image((String) row[1])
                    .size((String) row[2])
                    .totalQuantitySold((Long) row[3])
                    .totalRevenue((Long) row[4])
                    .build());
        }

        return StatisticOverviewOutput.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCompletedOrders(totalCompleted)
                .totalCanceledOrders(totalCanceled)
                .ordersByState(ordersByState)
                .topProducts(topProducts)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RevenueOutput> getRevenueByDays(String accessToken, LocalDate startDate, LocalDate endDate) {
        verifyShopAccess(accessToken);

        // startDate và endDate bắt buộc
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Phải cung cấp startDate và endDate.");
        }
        LocalDateTime[] range = resolveRange(startDate, endDate);
        assert range != null;

        List<RevenueOutput> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDateTime start = current.atStartOfDay();
            LocalDateTime end   = current.atTime(LocalTime.MAX);
            Long revenue = userOrderRepository.sumRevenueByDateRange(start, end);
            Long orders  = userOrderRepository.countOrdersByDateRange(start, end);
            result.add(RevenueOutput.builder()
                    .period(current.toString())
                    .totalRevenue(revenue)
                    .totalOrders(orders)
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RevenueOutput> getRevenueByMonths(String accessToken, int months) {
        verifyShopAccess(accessToken);

        List<RevenueOutput> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd   = monthStart.plusMonths(1).minusDays(1);
            LocalDateTime start  = monthStart.atStartOfDay();
            LocalDateTime end    = monthEnd.atTime(LocalTime.MAX);

            Long revenue = userOrderRepository.sumRevenueByDateRange(start, end);
            Long orders  = userOrderRepository.countOrdersByDateRange(start, end);

            result.add(RevenueOutput.builder()
                    .period(monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue()))
                    .totalRevenue(revenue)
                    .totalOrders(orders)
                    .build());
        }
        return result;
    }
}
