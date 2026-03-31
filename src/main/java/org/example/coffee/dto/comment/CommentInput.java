package org.example.coffee.dto.comment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentInput {
    @NotNull(message = "ProductId không được để trống")
    private Long productId;

    @NotNull(message = "ProductSizeId không được để trống")
    private Long productSizeId;

    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String comment;

    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Long rating;
}
