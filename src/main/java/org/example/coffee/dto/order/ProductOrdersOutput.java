package org.example.coffee.dto.order;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductOrdersOutput {
    private Long orderId;
    private List<ProductOrderOutput> productOrderOutputs;
    private CancelOrderOutput cancelOrderOutput;
    private String state;
    private Integer totalPrice;
}
