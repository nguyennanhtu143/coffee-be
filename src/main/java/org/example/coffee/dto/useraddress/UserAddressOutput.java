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
    private Integer toDistrictId;
    private String toWardCode;
    private Boolean isDefault;
}
