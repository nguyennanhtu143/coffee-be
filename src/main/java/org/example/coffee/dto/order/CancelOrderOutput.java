package org.example.coffee.dto.order;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CancelOrderOutput {
    private String name;
    private String reason;
}
