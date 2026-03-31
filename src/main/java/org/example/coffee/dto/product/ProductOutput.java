package org.example.coffee.dto.product;

import lombok.*;
import org.example.coffee.dto.productsize.ProductSizeOutput;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductOutput {
    private Long id;
    private Long productId;
    private String name;
    private String description;
    private String image;
    private Integer minPrice;
    private Double averageRatting;
    private List<ProductSizeOutput> sizes;
}
