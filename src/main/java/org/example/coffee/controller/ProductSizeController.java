package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.productsize.ProductSizeInput;
import org.example.coffee.dto.productsize.ProductSizeOutput;
import org.example.coffee.service.ProductSizeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/product-size")
public class ProductSizeController {
    private final ProductSizeService productSizeService;

    @Operation(summary = "Tạo size cho sản phẩm (Shop)")
    @PostMapping("/create")
    public ProductSizeOutput createSize(@RequestHeader("Authorization") String accessToken,
                                         @Valid @RequestBody ProductSizeInput input) {
        return productSizeService.createSize(accessToken, input);
    }

    @Operation(summary = "Cập nhật size (Shop)")
    @PostMapping("/update")
    public ProductSizeOutput updateSize(@RequestHeader("Authorization") String accessToken,
                                         @RequestParam Long sizeId,
                                         @Valid @RequestBody ProductSizeInput input) {
        return productSizeService.updateSize(accessToken, sizeId, input);
    }

    @Operation(summary = "Xoá size (Shop)")
    @DeleteMapping("/delete")
    public void deleteSize(@RequestHeader("Authorization") String accessToken,
                            @RequestParam Long sizeId) {
        productSizeService.deleteSize(accessToken, sizeId);
    }

    @Operation(summary = "Lấy danh sách size theo sản phẩm (Public)")
    @GetMapping("/get")
    public List<ProductSizeOutput> getSizesByProduct(@RequestParam Long productId) {
        return productSizeService.getSizesByProduct(productId);
    }
}
