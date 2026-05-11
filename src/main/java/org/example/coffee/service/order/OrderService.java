package org.example.coffee.service.order;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.common.Common;
import org.example.coffee.dto.order.*;
import org.example.coffee.entity.*;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.mapper.ProductOrderMapper;
import org.example.coffee.mapper.UserOrderMapper;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class OrderService {
    private final UserOrderRepository userOrderRepository;
    private final ProductOrderMapRepository productOrderMapRepository;
    private final ProductOrderMapper productOrderMapper;
    private final UserOrderMapper userOrderMapper;
    private final CartMapRepository cartMapRepository;
    private final CustomRepository customRepository;
    private final StateGeneration stateGeneration;
    private final UserAddressRepository userAddressRepository;

    @Transactional
    public UserOrderOutput orderProducts(String accessToken, UserOrderInput userOrderInput) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        log.info("User {} đặt hàng - tổng tiền: {}", userId, userOrderInput.getTotalPrice());

        applySavedOrManualAddress(userId, userOrderInput);
        UserOrderEntity userOrderEntity = userOrderMapper.getEntityFromInput(userOrderInput);
        userOrderEntity.setState(Common.PENDING_PAYMENT);
        userOrderEntity.setUserId(userId);
        userOrderEntity.setCreatedAt(LocalDateTime.now());

        List<Long> cartIds = userOrderInput.getProductOrderInputs().stream()
                .map(ProductOrderInput::getCartId).collect(Collectors.toList());

        List<CartMapEntity> cartMapEntities = cartMapRepository.findAllByIdIn(cartIds);
        if (cartMapEntities == null || cartMapEntities.isEmpty()) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }

        boolean isOnlinePayment = !"cash".equalsIgnoreCase(userOrderInput.getPaymentMethod());

        if (isOnlinePayment) {
            // Thanh toán online: KHÔNG xoá giỏ hàng, lưu cartIds để xoá sau khi thanh toán
            String cartIdsStr = cartIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            userOrderEntity.setPendingCartIds(cartIdsStr);
        } else {
            // Tiền mặt: xoá giỏ hàng ngay
            cartMapRepository.deleteAllByIdIn(cartIds);
        }

        userOrderRepository.save(userOrderEntity);

        for (ProductOrderInput productOrderInput : userOrderInput.getProductOrderInputs()) {
            ProductOrderMapEntity productOrderMapEntity = productOrderMapper.getEntityFromInput(productOrderInput);
            productOrderMapEntity.setOrderId(userOrderEntity.getId());
            productOrderMapRepository.save(productOrderMapEntity);
        }

        log.info("Đơn hàng #{} tạo thành công cho user {} - payment: {}", userOrderEntity.getId(), userId, userOrderInput.getPaymentMethod());
        return UserOrderOutput.builder()
                .orderId(userOrderEntity.getId())
                .amount(userOrderEntity.getTotalPrice())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ProductOrdersOutput> getProductOrdersByState(String accessToken, String state, Pageable pageable) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        List<UserOrderEntity> userOrderEntities = userOrderRepository.findAllByUserIdAndState(userId, state);
        StateOrder stateOrder = stateGeneration.findSateBy(state);
        return stateOrder.getOrders(userOrderEntities, pageable, state);
    }

    @Transactional
    public void cancelOrder(String accessToken, CancelOrderInput cancelOrderInput) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserOrderEntity userOrderEntity = customRepository.getUserOrder(cancelOrderInput.getOrderId());
        String state = userOrderEntity.getState();
        boolean canCancel = state.equals(Common.PENDING_PAYMENT) || state.equals(Common.CONFIRMED);
        if (!canCancel || !userId.equals(userOrderEntity.getUserId())) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }

        // Không cho hủy đơn đã thanh toán online
        String paymentMethod = userOrderEntity.getPaymentMethod();
        boolean isPaidOnline = paymentMethod != null
                && paymentMethod.startsWith("SePay")
                && !paymentMethod.contains("Chờ thanh toán");
        if (isPaidOnline) {
            throw new BadRequestException("Không thể hủy đơn đã thanh toán chuyển khoản. Vui lòng liên hệ shop để được hoàn tiền.");
        }

        userOrderEntity.setState(Common.CANCELED);
        userOrderEntity.setCancelerId(userId);
        userOrderEntity.setReasonCancellation(cancelOrderInput.getReason());
        userOrderEntity.setPendingCartIds(null);
        userOrderRepository.save(userOrderEntity);
        log.info("User {} hủy đơn hàng #{} - lý do: {}", userId, cancelOrderInput.getOrderId(), cancelOrderInput.getReason());
    }

    /**
     * Hủy đơn chưa thanh toán online (gọi khi user rời trang thanh toán).
     * Chỉ hủy nếu đơn đang ở trạng thái PENDING_PAYMENT VÀ paymentMethod = "SePay - Chờ thanh toán".
     */
    @Transactional
    public void cancelUnpaidOrder(Long orderId) {
        UserOrderEntity order = customRepository.getUserOrder(orderId);

        // Chỉ cho hủy đơn đang chờ thanh toán online
        boolean isUnpaidOnline = Common.PENDING_PAYMENT.equals(order.getState())
                && order.getPaymentMethod() != null
                && order.getPaymentMethod().contains("Chờ thanh toán");

        if (!isUnpaidOnline) {
            return;
        }

        order.setState(Common.CANCELED);
        order.setReasonCancellation("Khách rời trang thanh toán");
        order.setPendingCartIds(null);
        userOrderRepository.save(order);
        log.info("Auto hủy đơn #{} - khách rời trang thanh toán chưa chuyển khoản", orderId);
    }
    private void applySavedOrManualAddress(Long userId, UserOrderInput userOrderInput) {
        if (Objects.nonNull(userOrderInput.getAddressId())) {
            UserAddressEntity address = userAddressRepository.findByIdAndUserId(userOrderInput.getAddressId(), userId)
                    .orElseThrow(() -> new BadRequestException(Common.ACTION_FAIL));
            userOrderInput.setFullName(address.getFullName());
            userOrderInput.setPhoneNumber(address.getPhoneNumber());
            userOrderInput.setEmail(address.getEmail());
            userOrderInput.setAddress(address.getAddress());
            userOrderInput.setToDistrictId(address.getToDistrictId());
            userOrderInput.setToWardCode(address.getToWardCode());
            return;
        }

        if (!hasText(userOrderInput.getFullName())
                || !hasText(userOrderInput.getPhoneNumber())
                || !hasText(userOrderInput.getEmail())
                || !hasText(userOrderInput.getAddress())
                || Objects.isNull(userOrderInput.getToDistrictId())
                || !hasText(userOrderInput.getToWardCode())) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }
    }

    private boolean hasText(String value) {
        return Objects.nonNull(value) && !value.trim().isEmpty();
    }
}
