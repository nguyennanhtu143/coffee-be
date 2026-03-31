package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.order.CancelOrderInput;
import org.example.coffee.dto.order.ProductOrdersOutput;
import org.example.coffee.dto.order.UserOrderInput;
import org.example.coffee.dto.order.UserOrderOutput;
import org.example.coffee.service.order.OrderService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor

@RequestMapping("/api/v1/order")
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "order sản phẩm")
    @PostMapping()
    public UserOrderOutput orderProducts(@RequestHeader("Authorization") String accessToken,
                                         @Valid @RequestBody UserOrderInput userOrderInput) {
        return orderService.orderProducts(accessToken, userOrderInput);
    }

    @Operation(summary = "Lấy order theo trạng thái order")
    @GetMapping("/get-orders-by-state")
    public Page<ProductOrdersOutput> getOrdersByState(@RequestHeader("Authorization") String accessToken,
                                                      @RequestParam String state,
                                                      @ParameterObject Pageable pageable) {
        return orderService.getProductOrdersByState(accessToken, state, pageable);
    }

    @Operation(summary = "Hủy đặt hàng")
    @PostMapping("/cancel")
    public void cancelOrder(@RequestHeader("Authorization") String accessToken,
                            @Valid @RequestBody CancelOrderInput cancelOrderInput) {
        orderService.cancelOrder(accessToken, cancelOrderInput);
    }

    @Operation(summary = "Hủy đơn chưa thanh toán (dùng khi rời trang thanh toán)")
    @PostMapping("/cancel-unpaid")
    public void cancelUnpaidOrder(@RequestParam Long orderId) {
        orderService.cancelUnpaidOrder(orderId);
    }
}
