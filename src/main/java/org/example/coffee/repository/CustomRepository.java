package org.example.coffee.repository;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.entity.*;
import org.example.coffee.exceptionhandler.NotFoundException;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CustomRepository {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final CategoryRepository categoryRepository;
    private final CartMapRepository cartMapRepository;
    private final UserOrderRepository userOrderRepository;
    private final CommentRepository commentRepository;

    public UserEntity getUserBy(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }

    public ProductEntity getProductBy(Long productId) {
        return productRepository.findById(productId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }

    public CategoryEntity getCategoryBy(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }

    public CartMapEntity getCartMap(Long cartMapId) {
        return cartMapRepository.findById(cartMapId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }

    public UserOrderEntity getUserOrder(Long orderId) {
        return userOrderRepository.findById(orderId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }

    public CommentEntity getCommentBy(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }

    public ProductSizeEntity getProductSize(Long productSizeId) {
        return productSizeRepository.findById(productSizeId).orElseThrow(
                () -> new NotFoundException(Common.RECORD_NOT_FOUND)
        );
    }
}
