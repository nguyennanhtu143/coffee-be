package org.example.coffee.service.order;

import org.example.coffee.dto.order.ProductOrdersOutput;
import org.example.coffee.entity.UserOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StateOrder {
    Page<ProductOrdersOutput> getOrders(List<UserOrderEntity> userOrderEntities, Pageable pageable, String state);
}
