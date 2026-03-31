package org.example.coffee.mapper;

import org.example.coffee.dto.order.ProductOrderInput;
import org.example.coffee.entity.ProductOrderMapEntity;
import org.mapstruct.Mapper;

@Mapper
public interface ProductOrderMapper {
    ProductOrderMapEntity getEntityFromInput(ProductOrderInput productOrderInput);
}
