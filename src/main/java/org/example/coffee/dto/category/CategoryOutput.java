package org.example.coffee.dto.category;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CategoryOutput {
    private Long id;
    private Long categoryId;
    private String name;
}
