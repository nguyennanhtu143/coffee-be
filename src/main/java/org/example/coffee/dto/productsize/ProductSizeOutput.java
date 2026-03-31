package org.example.coffee.dto.productsize;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductSizeOutput {
    private Long id;
    private Long productId;
    private String size;
    private Integer price;
    private String description;
}
