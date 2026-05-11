package org.example.coffee.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductPageConfigOutput {
    private Integer userPageSize;
    private Integer adminPageSize;
}
