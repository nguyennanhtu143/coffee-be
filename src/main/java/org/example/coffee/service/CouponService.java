package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.coupon.ApplyCouponOutput;
import org.example.coffee.dto.coupon.CouponInput;
import org.example.coffee.dto.coupon.CouponOutput;
import org.example.coffee.entity.CouponEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.exceptionhandler.NotFoundException;
import org.example.coffee.repository.CouponRepository;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final CustomRepository customRepository;

    private void verifyShopAccess(String accessToken) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
    }

    @Transactional
    public CouponOutput createCoupon(String accessToken, CouponInput couponInput) {
        verifyShopAccess(accessToken);

        if (Boolean.TRUE.equals(couponRepository.existsByCode(couponInput.getCode()))) {
            throw new BadRequestException("Mã coupon đã tồn tại");
        }

        CouponEntity couponEntity = CouponEntity.builder()
                .code(couponInput.getCode().toUpperCase())
                .description(couponInput.getDescription())
                .discountType(couponInput.getDiscountType())
                .discountValue(couponInput.getDiscountValue())
                .maxDiscount(couponInput.getMaxDiscount())
                .minOrderValue(couponInput.getMinOrderValue() != null ? couponInput.getMinOrderValue() : 0)
                .maxUsage(couponInput.getMaxUsage() != null ? couponInput.getMaxUsage() : Integer.MAX_VALUE)
                .currentUsage(0)
                .isActive(Boolean.TRUE)
                .startDate(couponInput.getStartDate())
                .endDate(couponInput.getEndDate())
                .createdAt(LocalDateTime.now())
                .build();

        couponRepository.save(couponEntity);
        return toOutput(couponEntity);
    }

    @Transactional(readOnly = true)
    public List<CouponOutput> getAllCoupons(String accessToken) {
        verifyShopAccess(accessToken);
        return couponRepository.findAll().stream()
                .map(this::toOutput)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateCoupon(String accessToken, Long couponId) {
        verifyShopAccess(accessToken);
        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new NotFoundException(Common.RECORD_NOT_FOUND));
        coupon.setIsActive(Boolean.FALSE);
        couponRepository.save(coupon);
    }

    @Transactional
    public ApplyCouponOutput applyCoupon(String code, Integer orderTotal) {
        CouponEntity coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Mã coupon không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        if (Boolean.FALSE.equals(coupon.getIsActive())) {
            throw new BadRequestException("Mã coupon đã bị vô hiệu hóa");
        }
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("Mã coupon đã hết hạn");
        }
        if (coupon.getCurrentUsage() >= coupon.getMaxUsage()) {
            throw new BadRequestException("Mã coupon đã hết lượt sử dụng");
        }
        if (orderTotal < coupon.getMinOrderValue()) {
            throw new BadRequestException("Đơn hàng tối thiểu " + coupon.getMinOrderValue() + " để áp dụng mã này");
        }

        int discountAmount;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discountAmount = orderTotal * coupon.getDiscountValue() / 100;
            if (coupon.getMaxDiscount() != null && discountAmount > coupon.getMaxDiscount()) {
                discountAmount = coupon.getMaxDiscount();
            }
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        if (discountAmount > orderTotal) {
            discountAmount = orderTotal;
        }

        coupon.setCurrentUsage(coupon.getCurrentUsage() + 1);
        couponRepository.save(coupon);

        return ApplyCouponOutput.builder()
                .code(coupon.getCode())
                .originalPrice(orderTotal)
                .discountAmount(discountAmount)
                .finalPrice(orderTotal - discountAmount)
                .build();
    }

    private CouponOutput toOutput(CouponEntity entity) {
        return CouponOutput.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscount(entity.getMaxDiscount())
                .minOrderValue(entity.getMinOrderValue())
                .maxUsage(entity.getMaxUsage())
                .currentUsage(entity.getCurrentUsage())
                .isActive(entity.getIsActive())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }
}
