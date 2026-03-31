package org.example.coffee.dto.productsize;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ProductSizeInput {
    @NotNull(message = "ProductId không được để trống")
    private Long productId;

    @NotBlank(message = "Size không được để trống")
    private String size;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá phải lớn hơn hoặc bằng 0")
    private Integer price;

    private String description;
}
