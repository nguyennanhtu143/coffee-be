package org.example.coffee.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductInput {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;
}
