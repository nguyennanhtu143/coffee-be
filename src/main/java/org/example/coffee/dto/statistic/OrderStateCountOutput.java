package org.example.coffee.dto.statistic;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class OrderStateCountOutput {
    private String state;
    private Long count;
}
