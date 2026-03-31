package org.example.coffee.service.order;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.order.CancelOrderOutput;
import org.example.coffee.dto.order.ProductOrderOutput;
import org.example.coffee.dto.order.ProductOrdersOutput;
import org.example.coffee.entity.ProductOrderMapEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.repository.ProductOrderMapRepository;
import org.example.coffee.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CancellationState implements StateOrder{
    private final ProductOrderMapRepository productOrderMapRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ProductOrdersOutput> getOrders(List<UserOrderEntity> userOrderEntities, Pageable pageable, String state) {
        List<UserOrderEntity> sortedUserOrderEntities = new ArrayList<>(userOrderEntities);
        sortedUserOrderEntities.sort(
                (o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())
        );
        List<Long> orderIds = userOrderEntities.stream().map(UserOrderEntity::getId).collect(Collectors.toList());
        Page<ProductOrderMapEntity> productOrderMapEntities = productOrderMapRepository
                .findAllByOrderIdIn(orderIds, pageable);
        if (Objects.isNull(productOrderMapEntities) || productOrderMapEntities.isEmpty()) {
            return Page.empty();
        }

        Map<Long, List<ProductOrderMapEntity>> productOrderMapEntityMap = productOrderMapEntities
                .stream().collect(Collectors.groupingBy(ProductOrderMapEntity::getOrderId));

        Map<Long, UserEntity> userEntityMap = userRepository.findAllByIdIn(
                userOrderEntities.stream().map(UserOrderEntity::getCancelerId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<ProductOrdersOutput> productOrdersOutputs = new ArrayList<>();
        for (UserOrderEntity userOrderEntity : sortedUserOrderEntities) {
            List<ProductOrderMapEntity> productOrderMapEntityList = productOrderMapEntityMap.get(userOrderEntity.getId());
            if (productOrderMapEntityList == null) {
                continue;
            }
            List<ProductOrderOutput> productOrderOutputs = new ArrayList<>();
            int totalPrice = 0;
            for (ProductOrderMapEntity productOrderMapEntity : productOrderMapEntityList) {
                ProductOrderOutput productOrderOutput = ProductOrderOutput.builder()
                        .productSizeId(productOrderMapEntity.getProductSizeId())
                        .productName(productOrderMapEntity.getNameProduct())
                        .size(productOrderMapEntity.getSize())
                        .image(productOrderMapEntity.getImage())
                        .quantityOrder(productOrderMapEntity.getQuantityOrder())
                        .price(productOrderMapEntity.getPrice())
                        .totalPrice(productOrderMapEntity.getTotalPrice())
                        .build();
                totalPrice += productOrderOutput.getTotalPrice();
                productOrderOutputs.add(productOrderOutput);
            }

            UserEntity userEntity = userEntityMap.get(userOrderEntity.getCancelerId());
            CancelOrderOutput cancelOrderOutput = CancelOrderOutput.builder()
                    .reason(userOrderEntity.getReasonCancellation())
                    .name(userEntity.getFullName())
                    .build();
            ProductOrdersOutput productOrdersOutput = ProductOrdersOutput.builder()
                    .orderId(userOrderEntity.getId())
                    .productOrderOutputs(productOrderOutputs)
                    .state(Common.CANCELED)
                    .totalPrice(totalPrice)
                    .cancelOrderOutput(cancelOrderOutput)
                    .build();
            productOrdersOutputs.add(productOrdersOutput);
        }

        return new PageImpl<>(productOrdersOutputs, pageable, productOrderMapEntities.getTotalElements());
    }
}
