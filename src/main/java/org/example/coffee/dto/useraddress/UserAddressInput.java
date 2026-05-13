package org.example.coffee.dto.useraddress;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressInput {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Ma tinh/thanh pho khong duoc de trong")
    private Integer provinceId;

    @NotBlank(message = "Ten tinh/thanh pho khong duoc de trong")
    private String provinceName;

    @NotNull(message = "Mã khu vực không được để trống")
    private Integer toDistrictId;

    @NotBlank(message = "Ten quan/huyen khong duoc de trong")
    private String districtName;

    @NotBlank(message = "Mã phường/xã không được để trống")
    private String toWardCode;

    @NotBlank(message = "Ten phuong/xa khong duoc de trong")
    private String wardName;

    private Boolean isDefault;
}
