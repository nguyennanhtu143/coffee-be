package org.example.coffee.dto.coupon;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ApplyCouponOutput {
    private String code;
    private Integer originalPrice;
    private Integer discountAmount;
    private Integer finalPrice;
}
