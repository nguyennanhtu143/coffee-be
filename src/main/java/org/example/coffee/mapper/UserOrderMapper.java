package org.example.coffee.mapper;

import org.example.coffee.dto.order.UserOrderInput;
import org.example.coffee.entity.UserOrderEntity;
import org.mapstruct.Mapper;

@Mapper
public interface UserOrderMapper {
    UserOrderEntity getEntityFromInput(UserOrderInput userOrderInput);
}
