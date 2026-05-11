package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.common.ProductSite;
import org.example.coffee.dto.product.ProductPageConfigInput;
import org.example.coffee.dto.product.ProductPageConfigOutput;
import org.example.coffee.entity.ProductPageConfigEntity;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.ProductPageConfigRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@AllArgsConstructor
public class ProductPageConfigService {
    public static final Long CONFIG_ID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final ProductPageConfigRepository productPageConfigRepository;
    private final CustomRepository customRepository;

    @Transactional(readOnly = true)
    public ProductPageConfigOutput getPageConfig(String accessToken) {
        validateShop(accessToken);
        return buildOutput(getConfigOrDefault());
    }

    @Transactional
    public ProductPageConfigOutput updatePageConfig(String accessToken, ProductPageConfigInput input) {
        validateShop(accessToken);
        ProductPageConfigEntity config = productPageConfigRepository.findById(CONFIG_ID)
                .orElse(ProductPageConfigEntity.builder().id(CONFIG_ID).build());
        config.setUserPageSize(input.getUserPageSize());
        config.setAdminPageSize(input.getAdminPageSize());
        return buildOutput(productPageConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public int resolvePageSize(ProductSite site, Integer requestedSize) {
        if (Objects.nonNull(requestedSize)) {
            return requestedSize;
        }

        ProductPageConfigEntity config = getConfigOrDefault();
        return ProductSite.ADMIN.equals(site) ? config.getAdminPageSize() : config.getUserPageSize();
    }

    private ProductPageConfigEntity getConfigOrDefault() {
        return productPageConfigRepository.findById(CONFIG_ID)
                .orElse(ProductPageConfigEntity.builder()
                        .id(CONFIG_ID)
                        .userPageSize(DEFAULT_PAGE_SIZE)
                        .adminPageSize(DEFAULT_PAGE_SIZE)
                        .build());
    }

    private ProductPageConfigOutput buildOutput(ProductPageConfigEntity config) {
        return ProductPageConfigOutput.builder()
                .userPageSize(config.getUserPageSize())
                .adminPageSize(config.getAdminPageSize())
                .build();
    }

    private void validateShop(String accessToken) {
        if (!isShop(accessToken)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }
    }

    private boolean isShop(String accessToken) {
        if (Objects.isNull(accessToken) || !accessToken.startsWith("Bearer ")) {
            return false;
        }

        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        return Boolean.TRUE.equals(userEntity.getIsShop());
    }
}
