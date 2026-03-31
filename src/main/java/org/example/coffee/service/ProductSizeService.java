package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.productsize.ProductSizeInput;
import org.example.coffee.dto.productsize.ProductSizeOutput;
import org.example.coffee.entity.ProductSizeEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.exceptionhandler.NotFoundException;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.ProductSizeRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductSizeService {
    private final ProductSizeRepository productSizeRepository;
    private final CustomRepository customRepository;

    private void verifyShopAccess(String accessToken) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
    }

    @Transactional
    public ProductSizeOutput createSize(String accessToken, ProductSizeInput input) {
        verifyShopAccess(accessToken);
        customRepository.getProductBy(input.getProductId());

        if (Boolean.TRUE.equals(productSizeRepository.existsByProductIdAndSize(input.getProductId(), input.getSize()))) {
            throw new BadRequestException("Size " + input.getSize() + " đã tồn tại cho sản phẩm này");
        }

        ProductSizeEntity entity = ProductSizeEntity.builder()
                .productId(input.getProductId())
                .size(input.getSize())
                .price(input.getPrice())
                .description(input.getDescription())
                .build();
        productSizeRepository.save(entity);
        return toOutput(entity);
    }

    @Transactional
    public ProductSizeOutput updateSize(String accessToken, Long sizeId, ProductSizeInput input) {
        verifyShopAccess(accessToken);
        ProductSizeEntity entity = productSizeRepository.findById(sizeId)
                .orElseThrow(() -> new NotFoundException(Common.RECORD_NOT_FOUND));
        entity.setSize(input.getSize());
        entity.setPrice(input.getPrice());
        entity.setDescription(input.getDescription());
        productSizeRepository.save(entity);
        return toOutput(entity);
    }

    @Transactional
    public void deleteSize(String accessToken, Long sizeId) {
        verifyShopAccess(accessToken);
        productSizeRepository.findById(sizeId)
                .orElseThrow(() -> new NotFoundException(Common.RECORD_NOT_FOUND));
        productSizeRepository.deleteById(sizeId);
    }

    @Transactional(readOnly = true)
    public List<ProductSizeOutput> getSizesByProduct(Long productId) {
        return productSizeRepository.findAllByProductId(productId).stream()
                .map(this::toOutput)
                .collect(Collectors.toList());
    }

    private ProductSizeOutput toOutput(ProductSizeEntity entity) {
        return ProductSizeOutput.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .size(entity.getSize())
                .price(entity.getPrice())
                .description(entity.getDescription())
                .build();
    }
}
