package org.example.coffee.dto.statistic;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class StatisticOverviewOutput {
    private Long totalRevenue;
    private Long totalOrders;
    private Long totalCompletedOrders;
    private Long totalCanceledOrders;
    private List<OrderStateCountOutput> ordersByState;
    private List<TopProductOutput> topProducts;
}
