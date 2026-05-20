package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.dto.ApiResponse;
import org.example.coffee.dto.chatbot.ChatbotRequest;
import org.example.coffee.service.ChatbotService;
import org.example.coffee.service.UserService;
import org.example.coffee.token.TokenHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@AllArgsConstructor
@Slf4j
public class ChatbotController {
    private final ChatbotService chatbotService;
	private final UserService userService;

	@Operation(summary = "Gửi câu hỏi tới chatbot Coffee Shop")
    @PostMapping("/ask")
    public ApiResponse<String> ask(@RequestBody ChatbotRequest request) {
        String answer = chatbotService.ask(request.getMessage(), request.getHistory());
        return ApiResponse.ok(answer);
    }
}
