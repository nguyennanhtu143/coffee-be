package org.example.coffee.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class TokenResponse {
    private String accessToken;

    @JsonProperty("isShop")
    private Boolean isShop;
}
