package org.example.coffee.mapper;

import org.example.coffee.dto.product.ProductInput;
import org.example.coffee.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface ProductMapper {
    ProductEntity getEntityFromInput(ProductInput productInput);

    void updateEntityFromInput(@MappingTarget ProductEntity productEntity, ProductInput productInput);
}
