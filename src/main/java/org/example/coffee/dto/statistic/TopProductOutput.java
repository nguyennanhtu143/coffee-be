package org.example.coffee.dto.statistic;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class TopProductOutput {
    private String productName;
    private String image;
    private String size;
    private Long totalQuantitySold;
    private Long totalRevenue;
}
