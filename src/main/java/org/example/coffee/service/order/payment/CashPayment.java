package org.example.coffee.service.order.payment;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.repository.UserOrderRepository;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CashPayment implements Payment {
    private final UserOrderRepository userOrderRepository;

    @Override
    public String payment(UserOrderEntity userOrderEntity, String ipAddress) {
        userOrderEntity.setPaymentMethod("Tiền mặt");
        userOrderEntity.setState(Common.PENDING_PAYMENT);
        userOrderRepository.save(userOrderEntity);
        return Common.SUCCESS;
    }
}
