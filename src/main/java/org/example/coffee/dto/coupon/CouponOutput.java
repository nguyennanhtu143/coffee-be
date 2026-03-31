package org.example.coffee.dto.coupon;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CouponOutput {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private Integer discountValue;
    private Integer maxDiscount;
    private Integer minOrderValue;
    private Integer maxUsage;
    private Integer currentUsage;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
