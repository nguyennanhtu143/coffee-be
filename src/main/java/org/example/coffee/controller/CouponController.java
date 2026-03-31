package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.coupon.ApplyCouponOutput;
import org.example.coffee.dto.coupon.CouponInput;
import org.example.coffee.dto.coupon.CouponOutput;
import org.example.coffee.service.CouponService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/coupon")
public class CouponController {
    private final CouponService couponService;

    @Operation(summary = "Tạo mã khuyến mãi (Shop)")
    @PostMapping("/create")
    public CouponOutput createCoupon(@RequestHeader("Authorization") String accessToken,
                                      @Valid @RequestBody CouponInput couponInput) {
        return couponService.createCoupon(accessToken, couponInput);
    }

    @Operation(summary = "Lấy tất cả mã khuyến mãi (Shop)")
    @GetMapping("/list")
    public List<CouponOutput> getAllCoupons(@RequestHeader("Authorization") String accessToken) {
        return couponService.getAllCoupons(accessToken);
    }

    @Operation(summary = "Vô hiệu hóa mã khuyến mãi (Shop)")
    @PostMapping("/deactivate")
    public void deactivateCoupon(@RequestHeader("Authorization") String accessToken,
                                  @RequestParam Long couponId) {
        couponService.deactivateCoupon(accessToken, couponId);
    }

    @Operation(summary = "Áp dụng mã khuyến mãi (Khách hàng)")
    @PostMapping("/apply")
    public ApplyCouponOutput applyCoupon(@RequestParam String code,
                                          @RequestParam Integer orderTotal) {
        return couponService.applyCoupon(code, orderTotal);
    }
}
