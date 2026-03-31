package org.example.coffee.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ProductOrderInput {
    @NotNull(message = "CartId không được để trống")
    private Long cartId;

    @NotNull(message = "ProductSizeId không được để trống")
    private Long productSizeId;

    private String nameProduct;
    private String size;
    private Integer price;
    private String image;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantityOrder;

    private Integer totalPrice;
}
