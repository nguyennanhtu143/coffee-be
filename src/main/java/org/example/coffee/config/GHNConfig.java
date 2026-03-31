package org.example.coffee.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GHNConfig {
    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private String shopId;

    @Value("${ghn.base-url}")
    private String baseUrl;

    @Value("${ghn.from-name}")
    private String fromName;

    @Value("${ghn.from-phone}")
    private String fromPhone;

    @Value("${ghn.from-address}")
    private String fromAddress;

    @Value("${ghn.from-district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.from-ward-code}")
    private String fromWardCode;

    @Value("${ghn.default-weight}")
    private Integer defaultWeight;

    @Value("${ghn.default-length}")
    private Integer defaultLength;

    @Value("${ghn.default-width}")
    private Integer defaultWidth;

    @Value("${ghn.default-height}")
    private Integer defaultHeight;
}
