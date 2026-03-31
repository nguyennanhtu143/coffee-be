package org.example.coffee.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.comment.CommentInput;
import org.example.coffee.dto.comment.CommentOutput;
import org.example.coffee.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@AllArgsConstructor

@RequestMapping("/api/v1/comment")
public class CommentController {
    private static final Logger log = LoggerFactory.getLogger(CommentController.class);
    private final CommentService commentService;

    @Operation(summary = "tạo comment")
    @PostMapping("/create")
    public void createComment(@RequestHeader("Authorization") String accessToken,
                              @RequestPart(name = "commentInput") String commentInputString,
                              @RequestPart(name = "images", required = false) List<MultipartFile> multipartFiles) throws JsonProcessingException {
        CommentInput commentInput;
        ObjectMapper objectMapper = new ObjectMapper();
        commentInput = objectMapper.readValue(commentInputString, CommentInput.class);
        commentService.createComment(accessToken, commentInput, multipartFiles);
    }

    @Operation(summary = "Update comment")
    @PostMapping("/update")
    public void updateComment(@RequestHeader("Authorization") String accessToken,
                              @RequestParam Long commentId,
                              @RequestPart(name = "commentInput") String commentInputString,
                              @RequestPart(name = "images", required = false) List<MultipartFile> multipartFiles) throws JsonProcessingException {
        CommentInput commentInput;
        ObjectMapper objectMapper = new ObjectMapper();
        commentInput = objectMapper.readValue(commentInputString, CommentInput.class);
        commentService.updateComment(accessToken, commentId, commentInput, multipartFiles);
    }

    @Operation(summary = "Lấy ra comment")
    @GetMapping("/get-comments")
    public Page<CommentOutput> getCommentsByProduct(@RequestParam Long productId,
                                                    @ParameterObject Pageable pageable) {
        return commentService.getCommentsByProduct(productId, pageable);
    }

    @Operation(summary = "Xóa comment")
    @DeleteMapping("/delete")
    public void deleteComment(@RequestHeader("Authorization") String accessToken,
                              @RequestParam Long commentId
    ) {
        log.info("delete comment {}", commentId);
        commentService.deleteComment(accessToken, commentId);
    }
}
