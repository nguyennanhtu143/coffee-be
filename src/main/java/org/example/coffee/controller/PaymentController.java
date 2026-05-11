package org.example.coffee.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.example.coffee.service.order.payment.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Thanh toán - trả về thông tin chuyển khoản (JSON) hoặc SUCCESS nếu tiền mặt")
    @PostMapping(produces = "text/plain")
    public ResponseEntity<String> processPayment(@RequestHeader("Authorization") String accessToken,
                                                 @RequestParam Long orderId,
                                                 @RequestParam String method,
                                                 HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        return ResponseEntity.ok(paymentService.processPayment(accessToken, method, orderId, ipAddress));
    }

    @Operation(summary = "Kiểm tra trạng thái thanh toán đơn hàng qua SePay")
    @GetMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam Long orderId) {
        return ResponseEntity.ok(paymentService.checkPaymentStatus(accessToken, orderId));
    }

    @Operation(summary = "SePay webhook - nhận thông báo chuyển khoản")
    @PostMapping("/sepay-webhook")
    public ResponseEntity<String> sePayWebhook(@RequestBody JsonNode body) {
        String result = paymentService.handleSePayWebhook(body);
        return ResponseEntity.ok(result);
    }
}
