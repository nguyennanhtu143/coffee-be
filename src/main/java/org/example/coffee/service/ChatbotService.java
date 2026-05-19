package org.example.coffee.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.entity.ProductEntity;
import org.example.coffee.entity.ProductSizeEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.repository.ProductRepository;
import org.example.coffee.repository.ProductSizeRepository;
import org.example.coffee.repository.UserOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotService {
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final UserOrderRepository userOrderRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ChatbotService(
            ProductRepository productRepository,
            ProductSizeRepository productSizeRepository,
            UserOrderRepository userOrderRepository,
            @Value("${OPENAI_API_KEY:}") String openAiApiKey) {
        this.productRepository = productRepository;
        this.productSizeRepository = productSizeRepository;
        this.userOrderRepository = userOrderRepository;
        this.objectMapper = new ObjectMapper();

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is required for chatbot service");
        }

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Transactional(readOnly = true)
    public String ask(String userMessage, Long userId) {
        List<ProductEntity> products = productRepository.findAll();
        List<Long> productIds = products.stream()
                .map(ProductEntity::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        List<ProductSizeEntity> productSizes = productIds.isEmpty()
                ? Collections.emptyList()
                : productSizeRepository.findAllByProductIdIn(productIds);

        String menuSection = buildMenuSection(products, productSizes);
        String orderSection = buildOrderSection(userId);
        String systemPrompt = buildSystemPrompt(menuSection, orderSection);

        return callOpenAi(systemPrompt, userMessage);
    }

    private String buildMenuSection(List<ProductEntity> products, List<ProductSizeEntity> productSizes) {
        if (products.isEmpty()) {
            return "Menu hiện tại không có sản phẩm nào.";
        }

        Map<Long, List<ProductSizeEntity>> sizesByProduct = productSizes.stream()
                .collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

        return products.stream()
                .map(product -> {
                    String sizesText = sizesByProduct.getOrDefault(product.getId(), Collections.emptyList()).stream()
                            .map(size -> String.format("%s: %dđ (%s)", size.getSize(), size.getPrice(), size.getDescription() == null ? "" : size.getDescription()))
                            .collect(Collectors.joining("; "));
                    if (sizesText.isBlank()) {
                        sizesText = "Giá hiện tại chưa được cập nhật.";
                    }
                    return String.format("- %s: %s. %s", product.getName(), product.getDescription() == null ? "Không có mô tả." : product.getDescription(), sizesText);
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildOrderSection(Long userId) {
        if (userId == null) {
            return "Người dùng không đăng nhập, vì vậy không có thông tin đơn hàng để tham chiếu.";
        }

        List<UserOrderEntity> orders = userOrderRepository.findAll().stream()
                .filter(order -> userId.equals(order.getUserId()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                        return 0;
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        if (orders.isEmpty()) {
            return "Người dùng đã đăng nhập nhưng chưa có lịch sử đơn hàng.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return orders.stream()
                .limit(5)
                .map(order -> {
                    String createdAt = order.getCreatedAt() == null ? "Không có thời gian" : order.getCreatedAt().format(formatter);
                    String status = order.getState() == null ? "Không xác định" : order.getState();
                    String shippingInfo = order.getShippingStatus() != null ? String.format("Trạng thái giao hàng: %s", order.getShippingStatus()) : "Chưa có thông tin giao hàng";
                    String expected = order.getExpectedDelivery() != null ? String.format("Dự kiến giao: %s", order.getExpectedDelivery()) : "Không có ngày giao dự kiến";
                    return String.format("- Đơn #%d: %s, %s, %s, %s", order.getId(), createdAt, status, shippingInfo, expected);
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildSystemPrompt(String menuSection, String orderSection) {
        return "Bạn là một trợ lý ảo chuyên nghiệp cho cửa hàng Coffee Shop. " +
                "Chỉ sử dụng thông tin thoả mãn trong phần menu và đơn hàng bên dưới để trả lời khách hàng. " +
                "Nếu câu hỏi nằm ngoài nội dung này, hãy trả lời rằng bạn chỉ biết về menu, gợi ý sản phẩm và thông tin đơn hàng. " +
                "Không phán đoán thêm thông tin ngoài thực tế." +
                "\n\nMENU:\n" + menuSection +
                "\n\nTHÔNG TIN ĐƠN HÀNG NGƯỜI DÙNG:\n" + orderSection +
                "\n\nHãy trả lời bằng tiếng Việt một cách thân thiện và ngắn gọn.";
    }

    private String callOpenAi(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "gpt-3.5-turbo");
            payload.put("temperature", 0.5);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
            ));

            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null) {
                throw new IllegalStateException("Không nhận được phản hồi từ OpenAI");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException("OpenAI trả về dữ liệu không hợp lệ");
            }
            return content.asText().trim();
        } catch (WebClientResponseException ex) {
            log.error("OpenAI error status {} body {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Không thể kết nối đến OpenAI: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Lỗi khi gọi OpenAI", ex);
            throw new IllegalStateException("Lỗi xử lý chatbot. Vui lòng thử lại sau.", ex);
        }
    }
}
