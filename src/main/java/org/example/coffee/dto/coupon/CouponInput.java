package org.example.coffee.dto.coupon;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CouponInput {
    @NotBlank(message = "Mã coupon không được để trống")
    private String code;

    private String description;

    @NotBlank(message = "Loại giảm giá không được để trống")
    private String discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @Min(value = 1, message = "Giá trị giảm phải lớn hơn 0")
    private Integer discountValue;

    private Integer maxDiscount;

    @Min(value = 0, message = "Đơn tối thiểu phải >= 0")
    private Integer minOrderValue;

    @Min(value = 1, message = "Số lần sử dụng tối đa phải >= 1")
    private Integer maxUsage;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;
}
