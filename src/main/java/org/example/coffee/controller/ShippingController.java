package org.example.coffee.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.example.coffee.service.shipping.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/shipping")
public class ShippingController {
    private final ShippingService shippingService;

    @Operation(summary = "Lấy danh sách tỉnh/thành phố (Public)")
    @GetMapping("/provinces")
    public ResponseEntity<JsonNode> getProvinces() {
        return ResponseEntity.ok(shippingService.getProvinces());
    }

    @Operation(summary = "Lấy danh sách khu vực theo tỉnh (Public)")
    @GetMapping("/districts")
    public ResponseEntity<JsonNode> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(shippingService.getDistricts(provinceId));
    }

    @Operation(summary = "Lấy danh sách phường/xã theo khu vực (Public)")
    @GetMapping("/wards")
    public ResponseEntity<JsonNode> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(shippingService.getWards(districtId));
    }

    @Operation(summary = "Tính phí vận chuyển (Public)")
    @PostMapping("/calculate-fee")
    public ResponseEntity<Map<String, Object>> calculateFee(@RequestParam Integer toDistrictId,
                                                             @RequestParam String toWardCode) {
        return ResponseEntity.ok(shippingService.calculateShippingFee(toDistrictId, toWardCode));
    }

    @Operation(summary = "Shop tạo đơn giao hàng GHN")
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createShipping(@RequestHeader("Authorization") String accessToken,
                                                               @RequestParam Long orderId) {
        return ResponseEntity.ok(shippingService.createShipping(accessToken, orderId));
    }

    @Operation(summary = "Xem trạng thái giao hàng")
    @GetMapping("/track")
    public ResponseEntity<Map<String, Object>> trackShipping(@RequestParam Long orderId) {
        return ResponseEntity.ok(shippingService.trackShipping(orderId));
    }

    @Operation(summary = "Hủy giao hàng")
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelShipping(@RequestHeader("Authorization") String accessToken,
                                                               @RequestParam Long orderId) {
        return ResponseEntity.ok(shippingService.cancelShipping(accessToken, orderId));
    }

//    @Operation(summary = "GHN webhook - nhận cập nhật trạng thái giao hàng")
//    @PostMapping("/ghn-webhook")
//    public ResponseEntity<String> ghnWebhook(@RequestBody JsonNode body) {
//        shippingService.handleGHNWebhook(body);
//        return ResponseEntity.ok("OK");
//    }
}
