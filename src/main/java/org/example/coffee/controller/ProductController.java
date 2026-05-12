package org.example.coffee.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.common.ProductSite;
import org.example.coffee.dto.product.ProductInput;
import org.example.coffee.dto.product.ProductOutput;
import org.example.coffee.dto.product.ProductPageConfigInput;
import org.example.coffee.dto.product.ProductPageConfigOutput;
import org.example.coffee.service.ProductPageConfigService;
import org.example.coffee.service.ProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@AllArgsConstructor

@RequestMapping("/api/v1/product")
@Slf4j
public class ProductController {
    private final ProductService productService;
    private final ProductPageConfigService productPageConfigService;

    @Operation(summary = "thêm vào sản phẩm")
    @PostMapping("/create")
    public void createProduct(@RequestHeader("Authorization") String accessToken,
                              @RequestPart("productInput") String productInputString,
                              @RequestPart(value = "image", required = false) MultipartFile multipartFile) throws JsonProcessingException {
        ProductInput productInput;
        ObjectMapper objectMapper = new ObjectMapper();
        productInput = objectMapper.readValue(productInputString, ProductInput.class);
        productService.createProduct(accessToken, productInput, multipartFile);
    }

    @Operation(summary = "Sửa sản phẩm")
    @PostMapping("/update")
    public void updateProduct(@RequestHeader("Authorization") String accessToken,
                              @RequestPart("productInput") String productInputString,
                              @RequestParam Long productId,
                              @RequestPart(value = "image", required = false) MultipartFile multipartFile) throws JsonProcessingException {
        ProductInput productInput;
        ObjectMapper objectMapper = new ObjectMapper();
        productInput = objectMapper.readValue(productInputString, ProductInput.class);
        productService.updateProduct(accessToken, productInput, productId, multipartFile);
    }

    @Operation(summary = "Xóa sản phẩm")
    @DeleteMapping("/delete")
    public void deleteProduct(@RequestHeader("Authorization") String accessToken,
                              @RequestParam Long productId) {
        productService.deleteProduct(accessToken, productId);
    }

    @Operation(summary = "Lấy ra sản phẩm của shop")
    @GetMapping("/get-products")
    public Page<ProductOutput> getProducts(@RequestParam(value = "site", defaultValue = "USER") String site,
                                           @RequestParam(value = "size", required = false) Integer size,
                                           @ParameterObject Pageable pageable) {
        return productService.getProducts(ProductSite.from(site), size, pageable);
    }

    @Operation(summary = "Lấy sản phẩm theo category")
    @GetMapping("/get-products-by-category")
    public Page<ProductOutput> getProductsByCategory(@RequestParam(value = "site", defaultValue = "USER") String site,
                                                    @RequestParam Long categoryId,
                                                    @RequestParam(value = "size", required = false) Integer size,
                                                    @ParameterObject Pageable pageable) {
        return productService.getProductsByCategory(ProductSite.from(site), size, pageable, categoryId);
    }

    @Operation(summary = "Thêm sản phẩm vào danh mục")
    @PostMapping("/add-product-to-category")
    public void addProductToCategory(@RequestHeader("Authorization") String accessToken,
                                     @RequestParam Long productId,
                                     @RequestParam Long categoryId) {
        productService.addProductToCategory(accessToken, productId, categoryId);
    }

    @Operation(summary = "Xem chi tiết sản phẩm")
    @GetMapping("/get-details")
    public ProductOutput getProductDetails(@RequestParam Long productId) {
        return productService.getProductDetails(productId);
    }

    @Operation(summary = "Xóa sản phẩm trong category")
    @DeleteMapping("/delete-product-from-category")
    public void deleteProductFromCategory(@RequestHeader("Authorization") String accessToken,
                                          @RequestParam Long categoryId,
                                          @RequestParam Long productId) {

        productService.removeProductFromCategory(accessToken, categoryId, productId);
    }

    @Operation(summary = "Lấy sản phẩm không thuộc vào category")
    @GetMapping("/get-products-not-in-category")
    public List<ProductOutput> getProductsNotInCategory(@RequestParam Long categoryId,
                                                        @RequestHeader("Authorization") String accessToken) {
        return productService.getProductsNotInCategory(accessToken, categoryId);
    }

    @Operation(summary = "Lấy sản phẩm theo tìm kiếm")
    @GetMapping("/get-products-by-search")
    public Page<ProductOutput> getProductsBy(@RequestParam String search,
                                             @RequestParam(value = "site", defaultValue = "USER") String site,
                                             @RequestParam(value = "size", required = false) Integer size,
                                             @ParameterObject Pageable pageable) {
        return productService.getProductsBySearch(search, ProductSite.from(site), size, pageable);
    }

    @Operation(summary = "Admin search/filter/sort products")
    @GetMapping("/admin-products")
    public Page<ProductOutput> getAdminProducts(@RequestHeader("Authorization") String accessToken,
                                                @RequestParam(value = "search", required = false) String search,
                                                @RequestParam(value = "categoryId", required = false) Long categoryId,
                                                @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
                                                @RequestParam(value = "direction", defaultValue = "desc") String direction,
                                                @RequestParam(value = "size", required = false) Integer size,
                                                @ParameterObject Pageable pageable) {
        return productService.getAdminProducts(accessToken, search, categoryId, sortBy, direction, size, pageable);
    }

    @Operation(summary = "Lay cau hinh so san pham moi trang")
    @GetMapping("/page-config")
    public ProductPageConfigOutput getPageConfig(@RequestHeader("Authorization") String accessToken) {
        return productPageConfigService.getPageConfig(accessToken);
    }

    @Operation(summary = "Cap nhat cau hinh so san pham moi trang")
    @PostMapping("/page-config")
    public ProductPageConfigOutput updatePageConfig(@RequestHeader("Authorization") String accessToken,
                                                    @Valid @RequestBody ProductPageConfigInput input) {
        return productPageConfigService.updatePageConfig(accessToken, input);
    }
}
