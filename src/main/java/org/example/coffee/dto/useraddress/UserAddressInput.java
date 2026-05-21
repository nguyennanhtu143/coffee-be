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

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String address;

    @NotNull(message = "Mã tỉnh/thành phố không được để trống")
    private Integer provinceId;

    @NotBlank(message = "Tên tỉnh/thành phố không được để trống")
    private String provinceName;

    @NotNull(message = "Mã quận/huyện không được để trống")
    private Integer toDistrictId;

    @NotBlank(message = "Tên quận/huyện không được để trống")
    private String districtName;

    @NotBlank(message = "Mã phường/xã không được để trống")
    private String toWardCode;

    @NotBlank(message = "Tên phường/xã không được để trống")
    private String wardName;

    private Boolean isDefault;
}
