package org.example.coffee.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ChatbotRequest {
    private String message;
    private Long userId;

    /**
     * Conversation history gửi từ frontend, format:
     * [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
     * Frontend tự giới hạn tối đa 6 lượt gần nhất để tránh overflow token.
     */
    private List<Map<String, String>> history;
}
