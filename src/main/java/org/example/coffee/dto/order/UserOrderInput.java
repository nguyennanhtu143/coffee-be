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
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    @NotEmpty(message = "Danh sách sản phẩm không được rỗng")
    @Valid
    private List<ProductOrderInput> productOrderInputs;

    @NotNull(message = "Tổng giá không được để trống")
    private Integer totalPrice;

    @NotNull(message = "Mã khu vực không được để trống")
    private Integer toDistrictId;

    @NotBlank(message = "Mã phường/xã không được để trống")
    private String toWardCode;

    private Integer shippingFee;
}
