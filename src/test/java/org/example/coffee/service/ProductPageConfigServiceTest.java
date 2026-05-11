package org.example.coffee.service;

import org.example.coffee.dto.product.ProductPageConfigInput;
import org.example.coffee.dto.product.ProductPageConfigOutput;
import org.example.coffee.common.ProductSite;
import org.example.coffee.entity.ProductPageConfigEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.ProductPageConfigRepository;
import org.example.coffee.token.TokenHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPageConfigServiceTest {
    @Mock private ProductPageConfigRepository productPageConfigRepository;
    @Mock private CustomRepository customRepository;

    @InjectMocks
    private ProductPageConfigService productPageConfigService;

    private static final Long SHOP_ID = 1L;
    private static final String ACCESS_TOKEN = "Bearer shop-token";

    @Test
    void getPageConfig_missingConfig_returnsDefault() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(UserEntity.builder().isShop(Boolean.TRUE).build());
            when(productPageConfigRepository.findById(ProductPageConfigService.CONFIG_ID)).thenReturn(Optional.empty());

            ProductPageConfigOutput output = productPageConfigService.getPageConfig(ACCESS_TOKEN);

            assertEquals(20, output.getUserPageSize());
            assertEquals(20, output.getAdminPageSize());
        }
    }

    @Test
    void updatePageConfig_shop_success() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(UserEntity.builder().isShop(Boolean.TRUE).build());
            when(productPageConfigRepository.findById(ProductPageConfigService.CONFIG_ID)).thenReturn(Optional.empty());
            when(productPageConfigRepository.save(any(ProductPageConfigEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ProductPageConfigInput input = new ProductPageConfigInput();
            input.setUserPageSize(12);
            input.setAdminPageSize(30);

            ProductPageConfigOutput output = productPageConfigService.updatePageConfig(ACCESS_TOKEN, input);

            assertEquals(12, output.getUserPageSize());
            assertEquals(30, output.getAdminPageSize());
            verify(productPageConfigRepository).save(any(ProductPageConfigEntity.class));
        }
    }

    @Test
    void updatePageConfig_notShop_forbidden() {
        try (MockedStatic<TokenHelper> tokenHelperMock = mockStatic(TokenHelper.class)) {
            tokenHelperMock.when(() -> TokenHelper.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(SHOP_ID);
            when(customRepository.getUserBy(SHOP_ID)).thenReturn(UserEntity.builder().isShop(Boolean.FALSE).build());

            ProductPageConfigInput input = new ProductPageConfigInput();
            input.setUserPageSize(12);
            input.setAdminPageSize(30);

            assertThrows(ForbiddenException.class, () -> productPageConfigService.updatePageConfig(ACCESS_TOKEN, input));
        }
    }

    @Test
    void resolvePageSize_requestedSize_overridesConfig() {
        int pageSize = productPageConfigService.resolvePageSize(ProductSite.USER, 44);

        assertEquals(44, pageSize);
    }

    @Test
    void resolvePageSize_adminSite_usesAdminPageSize() {
        when(productPageConfigRepository.findById(ProductPageConfigService.CONFIG_ID))
                .thenReturn(Optional.of(ProductPageConfigEntity.builder()
                        .id(ProductPageConfigService.CONFIG_ID)
                        .userPageSize(12)
                        .adminPageSize(30)
                        .build()));

        int pageSize = productPageConfigService.resolvePageSize(ProductSite.ADMIN, null);

        assertEquals(30, pageSize);
    }
}
