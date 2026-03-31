package org.example.coffee.service.order.payment;

import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.repository.UserOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.util.List;

@Component
@ConditionalOnProperty(name = "payos.client-id")
public class PayOSPayment implements Payment {
    private final UserOrderRepository userOrderRepository;
    private final PayOS payOS;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    public PayOSPayment(UserOrderRepository userOrderRepository, PayOS payOS) {
        this.userOrderRepository = userOrderRepository;
        this.payOS = payOS;
    }

    @Override
    public String payment(UserOrderEntity userOrderEntity, String ipAddress) {
        userOrderEntity.setPaymentMethod("PayOS");
        userOrderRepository.save(userOrderEntity);

        try {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Don hang #" + userOrderEntity.getId())
                    .quantity(1)
                    .price(userOrderEntity.getTotalPrice().longValue())
                    .build();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(userOrderEntity.getId())
                    .amount(userOrderEntity.getTotalPrice().longValue())
                    .description("DH" + userOrderEntity.getId())
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .items(List.of(item))
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            return response.getCheckoutUrl();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo link thanh toán PayOS: " + e.getMessage(), e);
        }
    }
}
