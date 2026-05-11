package org.example.coffee.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductPageConfigInput {
    @NotNull(message = "User page size không được để trống")
    @Min(value = 1, message = "User page size phải lớn hơn hoặc bằng 1")
    @Max(value = 100, message = "User page size phải nhỏ hơn hoặc bằng 100")
    private Integer userPageSize;

    @NotNull(message = "Admin page size không được để trống")
    @Min(value = 1, message = "Admin page size phải lớn hơn hoặc bằng 1")
    @Max(value = 100, message = "Admin page size phải nhỏ hơn hoặc bằng 100")
    private Integer adminPageSize;
}
