package org.example.coffee.service;

import org.example.coffee.common.ProductSite;
import org.example.coffee.entity.ProductEntity;
import org.example.coffee.mapper.ProductMapper;
import org.example.coffee.repository.CommentRepository;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.ProductCategoryRepository;
import org.example.coffee.repository.ProductRepository;
import org.example.coffee.repository.ProductSizeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;
    @Mock private CustomRepository customRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ProductSizeRepository productSizeRepository;
    @Mock private ProductPageConfigService productPageConfigService;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProducts_withoutSize_usesConfiguredUserPageSize() {
        Pageable inputPageable = PageRequest.of(2, 20, Sort.by("name").ascending());
        when(productPageConfigService.resolvePageSize(ProductSite.USER, null)).thenReturn(12);
        when(productRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), inputPageable, 1));
        when(productSizeRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

        productService.getProducts(ProductSite.USER, null, inputPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(pageableCaptor.capture());
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(12, pageableCaptor.getValue().getPageSize());
        assertEquals(Sort.by("name").ascending(), pageableCaptor.getValue().getSort());
    }

    @Test
    void getProducts_withSize_usesClientOverride() {
        Pageable inputPageable = PageRequest.of(0, 20);
        when(productPageConfigService.resolvePageSize(ProductSite.ADMIN, 50)).thenReturn(50);
        when(productRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), inputPageable, 1));
        when(productSizeRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

        productService.getProducts(ProductSite.ADMIN, 50, inputPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(pageableCaptor.capture());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getProductsBySearch_usesResolvedPageSize() {
        Pageable inputPageable = PageRequest.of(1, 20);
        when(productPageConfigService.resolvePageSize(ProductSite.ADMIN, null)).thenReturn(30);
        when(productRepository.searchProductEntitiesByString(
                org.mockito.ArgumentMatchers.eq("coffee"),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product()), inputPageable, 1));
        when(productSizeRepository.findAllByProductIdIn(anyList())).thenReturn(List.of());

        productService.getProductsBySearch("coffee", ProductSite.ADMIN, null, inputPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchProductEntitiesByString(
                org.mockito.ArgumentMatchers.eq("coffee"),
                pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(30, pageableCaptor.getValue().getPageSize());
    }

    private ProductEntity product() {
        return ProductEntity.builder()
                .id(1L)
                .name("Coffee")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
