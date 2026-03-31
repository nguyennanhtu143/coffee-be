package org.example.coffee.dto.statistic;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class RevenueOutput {
    private String period;
    private Long totalOrders;
    private Long totalRevenue;
}
