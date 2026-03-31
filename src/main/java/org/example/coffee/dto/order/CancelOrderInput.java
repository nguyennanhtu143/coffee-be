package org.example.coffee.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CancelOrderInput {
    @NotNull(message = "OrderId không được để trống")
    private Long orderId;

    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}
