package org.example.coffee.dto.comment;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CommentOutput {
    private Long id;
    private Long userId;
    private String nameUser;
    private Long commentId;
    private String size;
    private String image;
    private String comment;
    private Long rating;
    private List<String> commentImages;
    private LocalDateTime createdAt;
}
