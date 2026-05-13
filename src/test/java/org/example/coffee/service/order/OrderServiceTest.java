package org.example.coffee.service.order;

import org.example.coffee.common.Common;
import org.example.coffee.dto.order.CancelOrderInput;
import org.example.coffee.dto.order.ProductOrderInput;
import org.example.coffee.dto.order.UserOrderInput;
import org.example.coffee.dto.order.UserOrderOutput;
import org.example.coffee.entity.CartMapEntity;
import org.example.coffee.entity.ProductOrderMapEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.mapper.ProductOrderMapper;
import org.example.coffee.mapper.UserOrderMapper;
import org.example.coffee.repository.*;
import org.example.coffee.token.TokenHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private UserOrderRepository userOrderRepository;
    @Mock private ProductOrderMapRepository productOrderMapRepository;
    @Mock private ProductOrderMapper productOrderMapper;
    @Mock private UserOrderMapper userOrderMapper;
    @Mock private CartMapRepository cartMapRepository;
    @Mock private CustomRepository customRepository;
    @Mock private StateGeneration stateGeneration;
    @Mock private UserAddressRepository userAddressRepository;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "Bearer test-token";

    @Test
    @DisplayName("Đặt hàng thành công - trả về orderId và amount")
    void orderProducts_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);

            ProductOrderInput productOrderInput = new ProductOrderInput();
            productOrderInput.setCartId(1L);
            productOrderInput.setProductSizeId(10L);
            productOrderInput.setQuantityOrder(2);

            UserOrderInput input = new UserOrderInput();
            input.setFullName("Test User");
            input.setPhoneNumber("0123456789");
            input.setEmail("test@test.com");
            input.setAddress("123 Test St");
            input.setPaymentMethod("cash");
            input.setProductOrderInputs(List.of(productOrderInput));
            input.setTotalPrice(100000);
            input.setProvinceId(202);
            input.setProvinceName("Ho Chi Minh");
            input.setToDistrictId(1542);
            input.setDistrictName("Quan 1");
            input.setToWardCode("1B1515");
            input.setWardName("Phuong Ben Nghe");

            UserOrderEntity orderEntity = UserOrderEntity.builder().id(1L).totalPrice(100000).build();
            when(userOrderMapper.getEntityFromInput(input)).thenReturn(orderEntity);
            when(userOrderRepository.save(any(UserOrderEntity.class))).thenReturn(orderEntity);

            CartMapEntity cart = CartMapEntity.builder().userId(USER_ID).productSizeId(10L).build();
            when(cartMapRepository.findAllByIdIn(anyList())).thenReturn(List.of(cart));

            ProductOrderMapEntity orderMapEntity = new ProductOrderMapEntity();
            when(productOrderMapper.getEntityFromInput(any())).thenReturn(orderMapEntity);

            UserOrderOutput result = orderService.orderProducts(ACCESS_TOKEN, input);

            assertNotNull(result);
            assertEquals(1L, result.getOrderId());
            assertEquals(100000, result.getAmount());
        }
    }

    @Test
    @DisplayName("Hủy đơn thành công - trạng thái PENDING_PAYMENT")
    void cancelOrder_pendingPayment_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);

            CancelOrderInput cancelInput = new CancelOrderInput();
            cancelInput.setOrderId(1L);
            cancelInput.setReason("Đổi ý");

            UserOrderEntity orderEntity = UserOrderEntity.builder()
                    .id(1L).userId(USER_ID).state(Common.PENDING_PAYMENT).build();
            when(customRepository.getUserOrder(1L)).thenReturn(orderEntity);

            orderService.cancelOrder(ACCESS_TOKEN, cancelInput);

            assertEquals(Common.CANCELED, orderEntity.getState());
            assertEquals(USER_ID, orderEntity.getCancelerId());
        }
    }

    @Test
    @DisplayName("Hủy đơn thành công - trạng thái CONFIRMED")
    void cancelOrder_confirmed_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);

            CancelOrderInput cancelInput = new CancelOrderInput();
            cancelInput.setOrderId(1L);
            cancelInput.setReason("Đổi ý");

            UserOrderEntity orderEntity = UserOrderEntity.builder()
                    .id(1L).userId(USER_ID).state(Common.CONFIRMED).build();
            when(customRepository.getUserOrder(1L)).thenReturn(orderEntity);

            orderService.cancelOrder(ACCESS_TOKEN, cancelInput);

            assertEquals(Common.CANCELED, orderEntity.getState());
        }
    }

    @Test
    @DisplayName("Hủy đơn thất bại - trạng thái SHIPPING không cho hủy")
    void cancelOrder_shipping_fail() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);

            CancelOrderInput cancelInput = new CancelOrderInput();
            cancelInput.setOrderId(1L);
            cancelInput.setReason("Đổi ý");

            UserOrderEntity orderEntity = UserOrderEntity.builder()
                    .id(1L).userId(USER_ID).state(Common.SHIPPING).build();
            when(customRepository.getUserOrder(1L)).thenReturn(orderEntity);

            assertThrows(BadRequestException.class, () -> orderService.cancelOrder(ACCESS_TOKEN, cancelInput));
        }
    }

    @Test
    @DisplayName("Hủy đơn thất bại - không phải chủ đơn")
    void cancelOrder_notOwner_fail() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);

            CancelOrderInput cancelInput = new CancelOrderInput();
            cancelInput.setOrderId(1L);
            cancelInput.setReason("Đổi ý");

            UserOrderEntity orderEntity = UserOrderEntity.builder()
                    .id(1L).userId(999L).state(Common.PENDING_PAYMENT).build();
            when(customRepository.getUserOrder(1L)).thenReturn(orderEntity);

            assertThrows(BadRequestException.class, () -> orderService.cancelOrder(ACCESS_TOKEN, cancelInput));
        }
    }
}
