package org.example.coffee.service.order;

import org.example.coffee.common.Common;
import org.example.coffee.dto.order.CancelOrderInput;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopOrderServiceTest {

    @Mock private UserOrderRepository userOrderRepository;
    @Mock private CustomRepository customRepository;
    @Mock private StateGeneration stateGeneration;

    @InjectMocks
    private ShopOrderService shopOrderService;

    private static final Long SHOP_ID = 1L;
    private static final String ACCESS_TOKEN = "Bearer shop-token";

    @Test
    @DisplayName("Shop xác nhận đơn hàng: PENDING_PAYMENT -> CONFIRMED")
    void acceptOrder_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);

            UserEntity shopEntity = UserEntity.builder().id(SHOP_ID).isShop(Boolean.TRUE).build();
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(shopEntity);

            UserOrderEntity order = UserOrderEntity.builder().id(1L).state(Common.PENDING_PAYMENT).build();
            when(customRepository.getUserOrder(1L)).thenReturn(order);

            shopOrderService.acceptOrder(ACCESS_TOKEN, 1L);
            assertEquals(Common.CONFIRMED, order.getState());
            verify(userOrderRepository).save(order);
        }
    }

    @Test
    @DisplayName("Shop giao hàng: CONFIRMED -> SHIPPING")
    void shipOrder_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);

            UserEntity shopEntity = UserEntity.builder().id(SHOP_ID).isShop(Boolean.TRUE).build();
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(shopEntity);

            UserOrderEntity order = UserOrderEntity.builder().id(1L).state(Common.CONFIRMED).build();
            when(customRepository.getUserOrder(1L)).thenReturn(order);

            shopOrderService.shipOrder(ACCESS_TOKEN, 1L);
            assertEquals(Common.SHIPPING, order.getState());
        }
    }

    @Test
    @DisplayName("Khách xác nhận nhận hàng: SHIPPING -> COMPLETED")
    void completeOrder_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            Long userId = 2L;
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(userId);

            UserOrderEntity order = UserOrderEntity.builder().id(1L).userId(userId).state(Common.SHIPPING).build();
            when(customRepository.getUserOrder(1L)).thenReturn(order);

            shopOrderService.completeOrder(ACCESS_TOKEN, 1L);
            assertEquals(Common.COMPLETED, order.getState());
        }
    }

    @Test
    @DisplayName("Không phải shop -> ForbiddenException")
    void acceptOrder_notShop_forbidden() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);

            UserEntity normalUser = UserEntity.builder().id(SHOP_ID).isShop(Boolean.FALSE).build();
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(normalUser);

            assertThrows(ForbiddenException.class, () -> shopOrderService.acceptOrder(ACCESS_TOKEN, 1L));
        }
    }

    @Test
    @DisplayName("Ship đơn sai trạng thái -> BadRequest")
    void shipOrder_wrongState_fail() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);

            UserEntity shopEntity = UserEntity.builder().id(SHOP_ID).isShop(Boolean.TRUE).build();
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(shopEntity);

            UserOrderEntity order = UserOrderEntity.builder().id(1L).state(Common.PENDING_PAYMENT).build();
            when(customRepository.getUserOrder(1L)).thenReturn(order);

            assertThrows(BadRequestException.class, () -> shopOrderService.shipOrder(ACCESS_TOKEN, 1L));
        }
    }

    @Test
    @DisplayName("Shop hủy đơn thành công")
    void cancelOrderByShop_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);

            UserEntity shopEntity = UserEntity.builder().id(SHOP_ID).isShop(Boolean.TRUE).build();
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(shopEntity);

            UserOrderEntity order = UserOrderEntity.builder().id(1L).state(Common.PENDING_PAYMENT).build();
            when(customRepository.getUserOrder(1L)).thenReturn(order);

            CancelOrderInput cancelInput = new CancelOrderInput();
            cancelInput.setOrderId(1L);
            cancelInput.setReason("Hết nguyên liệu");

            shopOrderService.cancelOrderByShop(ACCESS_TOKEN, cancelInput);
            assertEquals(Common.CANCELED, order.getState());
        }
    }
}
