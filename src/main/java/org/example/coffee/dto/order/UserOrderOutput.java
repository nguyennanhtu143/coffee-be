package org.example.coffee.dto.order;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class UserOrderOutput {
    private Long id;
    private Long orderId;
    private Integer amount;
}
