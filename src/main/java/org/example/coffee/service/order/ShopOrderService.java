package org.example.coffee.service.order;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.common.Common;
import org.example.coffee.dto.order.CancelOrderInput;
import org.example.coffee.dto.order.ProductOrdersOutput;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ShopOrderService {
    private final UserOrderRepository userOrderRepository;
    private final CustomRepository customRepository;
    private final StateGeneration stateGeneration;

    @Transactional(readOnly = true)
    public Page<ProductOrdersOutput> getProductOrdersByState(String accessToken, Pageable pageable, String state) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
        List<UserOrderEntity> userOrderEntities = userOrderRepository.findAllByState(state);
        StateOrder stateOrder = stateGeneration.findSateBy(state);
        return stateOrder.getOrders(userOrderEntities, pageable, state);
    }

    @Transactional
    public void acceptOrder(String accessToken, Long orderId) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(shopId);
        if (userEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        UserOrderEntity userOrderEntity = customRepository.getUserOrder(orderId);
        if(!userOrderEntity.getState().equals(Common.PENDING_PAYMENT)) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }
        userOrderEntity.setState(Common.CONFIRMED);
        userOrderRepository.save(userOrderEntity);
        log.info("Shop {} xác nhận đơn hàng #{}", shopId, orderId);
    }

    @Transactional
    public void shipOrder(String accessToken, Long orderId) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(shopId);
        if (userEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        UserOrderEntity userOrderEntity = customRepository.getUserOrder(orderId);
        if(!userOrderEntity.getState().equals(Common.CONFIRMED)) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }
        userOrderEntity.setState(Common.SHIPPING);
        userOrderRepository.save(userOrderEntity);
        log.info("Shop {} giao đơn hàng #{}", shopId, orderId);
    }

    @Transactional
    public void completeOrder(String accessToken, Long orderId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserOrderEntity userOrderEntity = customRepository.getUserOrder(orderId);
        if(!userOrderEntity.getState().equals(Common.SHIPPING) || !userOrderEntity.getUserId().equals(userId)) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }
        userOrderEntity.setState(Common.COMPLETED);
        userOrderRepository.save(userOrderEntity);
        log.info("User {} xác nhận nhận hàng - đơn #{} hoàn thành", userId, orderId);
    }

    @Transactional
    public void cancelOrderByShop(String accessToken, CancelOrderInput cancelOrderInput) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(shopId);
        if (userEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        UserOrderEntity userOrderEntity = customRepository.getUserOrder(cancelOrderInput.getOrderId());
        String state = userOrderEntity.getState();
        if(!state.equals(Common.PENDING_PAYMENT) && !state.equals(Common.CONFIRMED)) {
            throw new BadRequestException(Common.ACTION_FAIL);
        }

        // Không cho hủy đơn đã thanh toán online
        String paymentMethod = userOrderEntity.getPaymentMethod();
        boolean isPaidOnline = paymentMethod != null
                && paymentMethod.startsWith("SePay")
                && !paymentMethod.contains("Chờ thanh toán");
        if (isPaidOnline) {
            throw new BadRequestException("Không thể hủy đơn hàng đã thanh toán chuyển khoản. Vui lòng liên hệ khách hàng để hoàn tiền.");
        }

        userOrderEntity.setState(Common.CANCELED);
        userOrderEntity.setCancelerId(shopId);
        userOrderEntity.setReasonCancellation(cancelOrderInput.getReason());
        userOrderRepository.save(userOrderEntity);
    }
}
