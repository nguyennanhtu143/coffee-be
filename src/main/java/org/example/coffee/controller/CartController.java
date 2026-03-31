package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.cart.CartInput;
import org.example.coffee.dto.cart.CartOutput;
import org.example.coffee.service.CartMapService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor

@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartMapService cartMapService;

    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    @PostMapping("/add")
    public void addProductToCart(@RequestHeader("Authorization") String accessToken,
                                 @Valid @RequestBody CartInput cartInput) {
        cartMapService.addProductToCart(accessToken, cartInput);
    }

    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    @DeleteMapping("/delete")
    public void deleteCart(@RequestHeader("Authorization") String accessToken,
                           @RequestParam Long cartId) {
        cartMapService.removeProductFromCart(accessToken, cartId);
    }

    @Operation(summary = "Lấy ra sản phẩm trong giỏ hàng")
    @GetMapping("/get")
    public Page<CartOutput> getProductsInCart(@RequestHeader("Authorization") String accessToken,
                                              @ParameterObject Pageable pageable) {
        return cartMapService.getProductsInCart(accessToken, pageable);
    }

    @Operation(summary = "Xem sản phẩm trước khi order")
    @GetMapping("/get-product-before-ordering")
    public List<CartOutput> getProductBeforeOrdering(@RequestHeader("Authorization") String accessToken,
                                                     @RequestParam List<Long> cartIds) {
        return cartMapService.getProductsBeforeOrdering(accessToken, cartIds);
    }
}
