package org.example.coffee.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UserOrderInput {
    private String fullName;
    private String phoneNumber;
    private String email;
    private String address;

    @NotBlank(message = "Payment method khong duoc de trong")
    private String paymentMethod;

    @NotEmpty(message = "Danh sach san pham khong duoc rong")
    @Valid
    private List<ProductOrderInput> productOrderInputs;

    @NotNull(message = "Tong gia khong duoc de trong")
    private Integer totalPrice;

    private Integer toDistrictId;
    private String toWardCode;
    private Integer shippingFee;
    private Long addressId;
}
