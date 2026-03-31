package org.example.coffee.dto.order;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductOrderOutput {
    private Long productSizeId;
    private String productName;
    private String size;
    private Integer price;
    private String image;
    private Integer quantityOrder;
    private Integer totalPrice;
}
