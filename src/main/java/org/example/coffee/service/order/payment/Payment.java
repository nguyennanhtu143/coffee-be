package org.example.coffee.service.order.payment;

import org.example.coffee.entity.UserOrderEntity;

public interface Payment {
    String payment(UserOrderEntity userOrderEntity, String ipAddress);
}
