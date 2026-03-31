package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.statistic.*;
import org.example.coffee.entity.UserEntity;
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

    private void verifyShopAccess(String accessToken) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
    }

    @Transactional(readOnly = true)
    public StatisticOverviewOutput getOverview(String accessToken) {
        verifyShopAccess(accessToken);

        Long totalRevenue = userOrderRepository.sumTotalRevenueCompleted();
        Long totalOrders = userOrderRepository.countAllBy();
        Long totalCompleted = userOrderRepository.countByState(Common.COMPLETED);
        Long totalCanceled = userOrderRepository.countByState(Common.CANCELED);

        List<OrderStateCountOutput> ordersByState = new ArrayList<>();
        String[] states = {Common.PENDING_PAYMENT, Common.CONFIRMED, Common.SHIPPING, Common.COMPLETED, Common.CANCELED};
        for (String state : states) {
            Long count = userOrderRepository.countByState(state);
            ordersByState.add(OrderStateCountOutput.builder().state(state).count(count).build());
        }

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

    @Transactional(readOnly = true)
    public List<RevenueOutput> getRevenueByDays(String accessToken, int days) {
        verifyShopAccess(accessToken);

        List<RevenueOutput> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            Long revenue = userOrderRepository.sumRevenueByDateRange(start, end);
            Long orders = userOrderRepository.countOrdersByDateRange(start, end);

            result.add(RevenueOutput.builder()
                    .period(date.toString())
                    .totalRevenue(revenue)
                    .totalOrders(orders)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<RevenueOutput> getRevenueByMonths(String accessToken, int months) {
        verifyShopAccess(accessToken);

        List<RevenueOutput> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthEnd.atTime(LocalTime.MAX);

            Long revenue = userOrderRepository.sumRevenueByDateRange(start, end);
            Long orders = userOrderRepository.countOrdersByDateRange(start, end);

            result.add(RevenueOutput.builder()
                    .period(monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue()))
                    .totalRevenue(revenue)
                    .totalOrders(orders)
                    .build());
        }

        return result;
    }
}
