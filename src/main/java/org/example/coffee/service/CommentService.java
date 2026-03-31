package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.comment.CommentInput;
import org.example.coffee.dto.comment.CommentOutput;
import org.example.coffee.entity.CommentEntity;
import org.example.coffee.entity.ProductSizeEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.helper.FileHelper;
import org.example.coffee.helper.StringUtils;
import org.example.coffee.mapper.CommentMapper;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CustomRepository customRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final ProductOrderMapRepository productOrderMapRepository;
    private final ProductSizeRepository productSizeRepository;

    @Transactional
    public void createComment(String accessToken, CommentInput commentInput, List<MultipartFile> multipartFiles) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);

        // Shop không được đánh giá
        if (userEntity.getIsShop().equals(Boolean.TRUE)) {
            throw new ForbiddenException("Shop không được phép đánh giá sản phẩm");
        }

        // Kiểm tra đã mua sản phẩm (size này) và đơn hàng COMPLETED chưa
        Boolean hasPurchased = productOrderMapRepository.existsPurchase(userId, commentInput.getProductSizeId());
        if (!Boolean.TRUE.equals(hasPurchased)) {
            throw new BadRequestException("Bạn cần mua và nhận hàng thành công trước khi đánh giá sản phẩm này");
        }

        // Kiểm tra đã đánh giá size này chưa (1 user chỉ đánh giá 1 lần mỗi size)
        Boolean alreadyCommented = commentRepository.existsByUserIdAndProductSizeId(userId, commentInput.getProductSizeId());
        if (Boolean.TRUE.equals(alreadyCommented)) {
            throw new BadRequestException("Bạn đã đánh giá size này rồi");
        }

        // Lấy thông tin size
        ProductSizeEntity sizeEntity = customRepository.getProductSize(commentInput.getProductSizeId());

        CommentEntity commentEntity = CommentEntity.builder()
                .userId(userId)
                .productId(commentInput.getProductId())
                .productSizeId(commentInput.getProductSizeId())
                .size(sizeEntity.getSize())
                .comment(commentInput.getComment())
                .rating(commentInput.getRating())
                .images(
                        Objects.nonNull(multipartFiles) && !multipartFiles.isEmpty() ?
                                StringUtils.getStringFromList(FileHelper.getImageUrls(multipartFiles)) : null
                )
                .createAt(LocalDateTime.now())
                .build();
        commentRepository.save(commentEntity);
    }

    @Transactional
    public void updateComment(String accessToken,
                              Long commentId,
                              CommentInput commentInput,
                              List<MultipartFile> multipartFiles) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        if (userEntity.getIsShop().equals(Boolean.TRUE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        CommentEntity commentEntity = customRepository.getCommentBy(commentId);
        if (!commentEntity.getUserId().equals(userId)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        // Kiểm tra thời hạn chỉnh sửa: tối đa 1 tháng sau ngày tạo
        LocalDateTime editDeadline = commentEntity.getCreateAt().plusMonths(1);
        if (LocalDateTime.now().isAfter(editDeadline)) {
            throw new BadRequestException("Đã quá thời hạn chỉnh sửa đánh giá (tối đa 1 tháng sau ngày đánh giá)");
        }

        commentMapper.updateEntityFromInput(commentEntity, commentInput);
        if (Objects.nonNull(multipartFiles) && !multipartFiles.isEmpty()) {
            commentEntity.setImages(StringUtils.getStringFromList(FileHelper.getImageUrls(multipartFiles)));
        }
        commentRepository.save(commentEntity);
    }

    @Transactional(readOnly = true)
    public Page<CommentOutput> getCommentsByProduct(Long productId, Pageable pageable) {
        Page<CommentEntity> commentEntities = commentRepository.findAllByProductId(productId, pageable);
        if (Objects.isNull(commentEntities) || commentEntities.isEmpty()) {
            return Page.empty();
        }

        Set<Long> userIds = commentEntities.stream().map(CommentEntity::getUserId).collect(Collectors.toSet());
        Map<Long, UserEntity> userEntityMap = userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, userEntity -> userEntity));

        return commentEntities.map(
                commentEntity -> {
                    UserEntity userEntity = userEntityMap.get(commentEntity.getUserId());
                    return CommentOutput.builder()
                            .id(commentEntity.getId())
                            .userId(commentEntity.getUserId())
                            .nameUser(userEntity.getFullName())
                            .commentId(commentEntity.getId())
                            .size(commentEntity.getSize())
                            .image(userEntity.getImage())
                            .comment(commentEntity.getComment())
                            .rating(commentEntity.getRating())
                            .commentImages(StringUtils.getListFromString(commentEntity.getImages()))
                            .createdAt(commentEntity.getCreateAt())
                            .build();
                }
        );
    }

    @Transactional
    public void deleteComment(String accessToken, Long commentId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        CommentEntity commentEntity = customRepository.getCommentBy(commentId);
        if (!commentEntity.getUserId().equals(userId)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
        commentRepository.deleteById(commentId);
    }
}
