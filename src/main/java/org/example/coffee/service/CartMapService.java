package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.cart.CartInput;
import org.example.coffee.dto.cart.CartOutput;
import org.example.coffee.entity.CartMapEntity;
import org.example.coffee.entity.ProductEntity;
import org.example.coffee.entity.ProductSizeEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CartMapService {
    private final CartMapRepository cartMapRepository;
    private final CustomRepository customRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;

    @Transactional
    public void addProductToCart(String accessToken, CartInput cartInput) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        if (userEntity.getIsShop().equals(Boolean.TRUE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        // Verify productSize exists
        customRepository.getProductSize(cartInput.getProductSizeId());

        CartMapEntity existing = cartMapRepository.findByProductSizeIdAndUserId(cartInput.getProductSizeId(), userId);
        if (existing == null) {
            CartMapEntity cartMapEntity = CartMapEntity.builder()
                    .userId(userId)
                    .productSizeId(cartInput.getProductSizeId())
                    .quantityOrder(cartInput.getQuantityOrder())
                    .createdAt(LocalDateTime.now())
                    .build();
            cartMapRepository.save(cartMapEntity);
        } else {
            existing.setQuantityOrder(cartInput.getQuantityOrder() + existing.getQuantityOrder());
            cartMapRepository.save(existing);
        }
    }

    @Transactional
    public void removeProductFromCart(String accessToken, Long cartId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        CartMapEntity cartMapEntity = customRepository.getCartMap(cartId);
        if (userEntity.getIsShop().equals(Boolean.TRUE) || !cartMapEntity.getUserId().equals(userId)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
        cartMapRepository.deleteById(cartId);
    }

    @Transactional(readOnly = true)
    public Page<CartOutput> getProductsInCart(String accessToken, Pageable pageable) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        Page<CartMapEntity> cartMapEntities = cartMapRepository.findAllByUserId(userId, pageable);
        if (Objects.isNull(cartMapEntities) || cartMapEntities.isEmpty()) {
            return Page.empty();
        }

        // Lookup ProductSizeEntity
        List<Long> sizeIds = cartMapEntities.stream()
                .map(CartMapEntity::getProductSizeId).collect(Collectors.toList());
        Map<Long, ProductSizeEntity> sizeMap = productSizeRepository.findAllById(sizeIds)
                .stream().collect(Collectors.toMap(ProductSizeEntity::getId, Function.identity()));

        // Lookup ProductEntity
        List<Long> productIds = sizeMap.values().stream()
                .map(ProductSizeEntity::getProductId).distinct().collect(Collectors.toList());
        Map<Long, ProductEntity> productMap = productRepository.findAllByIdIn(productIds)
                .stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        return cartMapEntities.map(cartMapEntity -> {
            ProductSizeEntity sizeEntity = sizeMap.get(cartMapEntity.getProductSizeId());
            ProductEntity productEntity = productMap.get(sizeEntity.getProductId());
            return CartOutput.builder()
                    .cartId(cartMapEntity.getId())
                    .productSizeId(sizeEntity.getId())
                    .productId(productEntity.getId())
                    .nameProduct(productEntity.getName())
                    .size(sizeEntity.getSize())
                    .price(sizeEntity.getPrice())
                    .quantityOrder(cartMapEntity.getQuantityOrder())
                    .totalPrice(sizeEntity.getPrice() * cartMapEntity.getQuantityOrder())
                    .imageUrl(productEntity.getImage())
                    .build();
        });
    }

    @Transactional
    public List<CartOutput> getProductsBeforeOrdering(String accessToken, List<Long> cartIds) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        List<CartMapEntity> cartMapEntities = cartMapRepository.findAllByIdIn(cartIds);
        for (CartMapEntity cartMapEntity : cartMapEntities) {
            if (!cartMapEntity.getUserId().equals(userId)) {
                throw new ForbiddenException(Common.ACTION_FAIL);
            }
        }

        // Lookup ProductSizeEntity
        List<Long> sizeIds = cartMapEntities.stream()
                .map(CartMapEntity::getProductSizeId).collect(Collectors.toList());
        Map<Long, ProductSizeEntity> sizeMap = productSizeRepository.findAllById(sizeIds)
                .stream().collect(Collectors.toMap(ProductSizeEntity::getId, Function.identity()));

        // Lookup ProductEntity
        List<Long> productIds = sizeMap.values().stream()
                .map(ProductSizeEntity::getProductId).distinct().collect(Collectors.toList());
        Map<Long, ProductEntity> productMap = productRepository.findAllByIdIn(productIds)
                .stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        List<CartOutput> cartOutputs = new ArrayList<>();
        for (CartMapEntity cartMapEntity : cartMapEntities) {
            ProductSizeEntity sizeEntity = sizeMap.get(cartMapEntity.getProductSizeId());
            ProductEntity productEntity = productMap.get(sizeEntity.getProductId());
            cartOutputs.add(CartOutput.builder()
                    .cartId(cartMapEntity.getId())
                    .productSizeId(sizeEntity.getId())
                    .productId(productEntity.getId())
                    .nameProduct(productEntity.getName())
                    .size(sizeEntity.getSize())
                    .price(sizeEntity.getPrice())
                    .quantityOrder(cartMapEntity.getQuantityOrder())
                    .totalPrice(sizeEntity.getPrice() * cartMapEntity.getQuantityOrder())
                    .imageUrl(productEntity.getImage())
                    .build());
        }
        return cartOutputs;
    }
}
