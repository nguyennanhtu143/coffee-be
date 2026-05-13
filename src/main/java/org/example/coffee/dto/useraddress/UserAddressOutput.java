package org.example.coffee.dto.useraddress;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserAddressOutput {
    private Long addressId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String address;
    private Integer provinceId;
    private String provinceName;
    private Integer toDistrictId;
    private String districtName;
    private String toWardCode;
    private String wardName;
    private Boolean isDefault;
}
