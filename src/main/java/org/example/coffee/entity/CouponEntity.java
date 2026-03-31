package org.example.coffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "tbl_coupon")
@Builder
public class CouponEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String description;

    // "PERCENTAGE" hoặc "FIXED"
    private String discountType;

    // Giá trị giảm (% hoặc số tiền cố định)
    private Integer discountValue;

    // Giảm tối đa (cho loại PERCENTAGE)
    private Integer maxDiscount;

    // Đơn hàng tối thiểu để áp dụng
    private Integer minOrderValue;

    // Số lần sử dụng tối đa
    private Integer maxUsage;

    // Số lần đã sử dụng
    private Integer currentUsage;

    private Boolean isActive;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
