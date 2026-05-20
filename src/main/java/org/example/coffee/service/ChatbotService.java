package org.example.coffee.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.common.Common;
import org.example.coffee.entity.ProductEntity;
import org.example.coffee.entity.ProductSizeEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.repository.ProductRepository;
import org.example.coffee.repository.ProductSizeRepository;
import org.example.coffee.repository.UserOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotService {
	private final ProductRepository productRepository;
	private final ProductSizeRepository productSizeRepository;
	private final UserOrderRepository userOrderRepository;
	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;
	private final int maxOutputTokens;
	private final double temperature;

	/**
	 * Tối đa bao nhiêu lượt lịch sử được inject vào Gemini context
	 */
	private static final int MAX_HISTORY_TURNS = 6;

	/**
	 * Mapping trạng thái nội bộ → tiếng Việt thân thiện
	 */
	private static final Map<String, String> STATE_LABELS = Map.of(
				"PENDING_PAYMENT", "Chờ thanh toán",
				"CONFIRMED", "Đã xác nhận",
				"SHIPPING", "Đang vận chuyển",
				"DELIVERING", "Đang giao hàng",
				"COMPLETED", "Đã hoàn thành",
				"CANCELED", "Đã hủy",
				"DELIVERY_FAILED", "Giao thất bại",
				"RETURNING", "Đang hoàn hàng",
				"RETURNED", "Đã hoàn hàng"
	);

	public ChatbotService(
				ProductRepository productRepository,
				ProductSizeRepository productSizeRepository,
				UserOrderRepository userOrderRepository,
				@Value("${chatbot.api-key}") String apiKey,
				@Value("${chatbot.base-url}") String baseUrl,
				@Value("${chatbot.model}") String model,
				@Value("${chatbot.max-output-tokens:500}") int maxOutputTokens,
				@Value("${chatbot.temperature:0.6}") double temperature) {

		this.productRepository = productRepository;
		this.productSizeRepository = productSizeRepository;
		this.userOrderRepository = userOrderRepository;
		this.objectMapper = new ObjectMapper();
		this.model = model;
		this.maxOutputTokens = maxOutputTokens;
		this.temperature = temperature;

		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("chatbot.api-key is required");
		}
		this.apiKey = apiKey;

		this.webClient = WebClient.builder()
					.baseUrl(baseUrl)
					.defaultHeader("Content-Type", "application/json")
					.build();

		log.info("ChatbotService khởi động — model: {}, baseUrl: {}", model, baseUrl);
	}

	// ==================== PUBLIC API ====================

	@Transactional(readOnly = true)
	public String ask(String userMessage, List<Map<String, String>> history) {
		Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		List<ProductEntity> products = productRepository.findAll();
		List<Long> productIds = products.stream()
					.map(ProductEntity::getId)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

		List<ProductSizeEntity> productSizes = productIds.isEmpty()
					? Collections.emptyList()
					: productSizeRepository.findAllByProductIdIn(productIds);

		String menuSection = buildMenuSection(products, productSizes);
		String orderSection = buildOrderSection(userId);
		String systemPrompt = buildSystemPrompt(menuSection, orderSection);

		return callGemini(systemPrompt, userMessage, history);
	}

	// ==================== CONTEXT BUILDERS ====================

	private String buildMenuSection(List<ProductEntity> products, List<ProductSizeEntity> productSizes) {
		if (products.isEmpty()) {
			return "Menu hiện tại không có sản phẩm nào.";
		}

		Map<Long, List<ProductSizeEntity>> sizesByProduct = productSizes.stream()
					.collect(Collectors.groupingBy(ProductSizeEntity::getProductId));

		return products.stream()
					.map(product -> {
						String sizesText = sizesByProduct
									.getOrDefault(product.getId(), Collections.emptyList())
									.stream()
									.map(size -> {
										String desc = (size.getDescription() == null || size.getDescription().isBlank())
													? "" : " - " + size.getDescription();
										return String.format("%s: %,dđ%s", size.getSize(), size.getPrice(), desc);
									})
									.collect(Collectors.joining(" | "));
						if (sizesText.isBlank()) {
							sizesText = "Giá đang được cập nhật.";
						}
						String desc = (product.getDescription() == null || product.getDescription().isBlank())
									? "" : " — " + product.getDescription();
						return String.format("• %s%s [%s]", product.getName(), desc, sizesText);
					})
					.collect(Collectors.joining("\n"));
	}

	/**
	 * Xây dựng context đơn hàng 3 tầng:
	 * 1. Thống kê tổng hợp (COUNT theo state) — toàn bộ lịch sử, không bỏ sót
	 * 2. Danh sách đơn đang hoạt động kèm ID cụ thể
	 * 3. Chi tiết 3 đơn gần nhất
	 */
	private String buildOrderSection(Long userId) {
		if (userId == null) {
			return "Khách chưa đăng nhập — không có thông tin đơn hàng.";
		}

		// ── Tầng 1: Thống kê tổng hợp toàn bộ ──
		List<Object[]> stateCounts = userOrderRepository.countByStateForUser(userId);
		if (stateCounts.isEmpty()) {
			return "Khách đã đăng nhập nhưng chưa có đơn hàng nào.";
		}

		// Dùng LinkedHashMap để giữ thứ tự hiển thị có nghĩa
		Map<String, Long> countByState = new LinkedHashMap<>();
		long totalOrders = 0;
		for (Object[] row : stateCounts) {
			String state = (String) row[0];
			Long count  = (Long) row[1];
			countByState.put(state, count);
			totalOrders += count;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("THỐNG KÊ TOÀN BỘ ĐƠN HÀNG (chính xác, không bỏ sót):\n");
		sb.append("Tổng: ").append(totalOrders).append(" đơn\n");
		countByState.forEach((state, count) -> {
			String label = STATE_LABELS.getOrDefault(state, state);
			sb.append("- ").append(label).append(": ").append(count).append(" đơn\n");
		});

		// ── Tầng 2: Đơn đang hoạt động (kèm mã đơn để tham chiếu) ──
		List<String> activeStates = List.of(
				Common.PENDING_PAYMENT, Common.CONFIRMED,
				Common.SHIPPING, Common.DELIVERING, Common.WAITING_DELIVERY
		);
		List<UserOrderEntity> activeOrders = userOrderRepository
				.findByUserIdAndStateInOrderByCreatedAtDesc(userId, activeStates);

		if (!activeOrders.isEmpty()) {
			sb.append("\nĐƠN ĐANG XỬ LÝ (có thể tra cứu):\n");
			for (UserOrderEntity o : activeOrders) {
				String label = STATE_LABELS.getOrDefault(o.getState(), o.getState());
				String shipping = o.getShippingStatus() != null
						? " [GHN: " + o.getShippingStatus() + "]" : "";
				sb.append("- Đơn #").append(o.getId())
				  .append(" — ").append(label).append(shipping).append("\n");
			}
		}

		// ── Tầng 3: Chi tiết 3 đơn gần nhất ──
		List<UserOrderEntity> recent = userOrderRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
		if (!recent.isEmpty()) {
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
			sb.append("\n3 ĐƠN GẦN NHẤT (chi tiết):\n");
			recent.stream().limit(3).forEach(o -> {
				String createdAt = o.getCreatedAt() == null ? "?" : o.getCreatedAt().format(fmt);
				String label     = STATE_LABELS.getOrDefault(o.getState(), o.getState());
				String price     = o.getTotalPrice() != null
						? String.format("%,dđ", o.getTotalPrice()) : "?";
				sb.append("• Đơn #").append(o.getId())
				  .append(" — ").append(createdAt)
				  .append(" — ").append(label)
				  .append(" — ").append(price).append("\n");
			});
		}

		return sb.toString();
	}

	private String buildSystemPrompt(String menuSection, String orderSection) {
		return """
					Bạn là trợ lý ảo ☕ của Coffee Shop — một cửa hàng cà phê chất lượng cao.
					
					THÔNG TIN CỬA HÀNG:
					- Giờ mở cửa: 7:00 - 22:00 hàng ngày
					- Hỗ trợ đặt hàng online, giao hàng tận nơi qua GHN Express
					- Thanh toán: Tiền mặt (COD) hoặc chuyển khoản ngân hàng
					- Chính sách hủy đơn: Khách có thể hủy khi đơn ở trạng thái "Chờ thanh toán" hoặc "Đã xác nhận"
					- Liên hệ shop: Nhắn tin qua fanpage hoặc hotline
					
					QUY TẮC TRẢ LỜI:
					- Chỉ trả lời về menu, đơn hàng của khách, chính sách cửa hàng và gợi ý cà phê
					- Nếu câu hỏi ngoài phạm vi này, hướng dẫn khách liên hệ shop trực tiếp
					- Dùng emoji phù hợp để thân thiện (☕ 🧋 📦 🚚 💳)
					- Trả lời bằng tiếng Việt, ngắn gọn và thân thiện
					- Khi gợi ý sản phẩm, hãy đề xuất 2-3 sản phẩm phù hợp kèm giá
					- QUAN TRỌNG: Khi trả lời về số lượng đơn hàng, LUÔN dùng số liệu từ phần
					  "THỐNG KÊ TOÀN BỘ ĐƠN HÀNG" — đây là số chính xác 100%.
					  Phần "3 ĐƠN GẦN NHẤT" chỉ dùng để biết chi tiết, không dùng để đếm.
					- Không bịa đặt thông tin ngoài dữ liệu được cung cấp
					
					MENU HIỆN TẠI:
					""" + menuSection + """
					
					
					DỮ LIỆU ĐƠN HÀNG CỦA KHÁCH:
					""" + orderSection;
	}

	// ==================== GEMINI API CALLER ====================

	/**
	 * Gọi Gemini API theo format:
	 * POST /v1beta/models/{model}:generateContent?key={apiKey}
	 * <p>
	 * Request body:
	 * {
	 * "system_instruction": { "parts": [{ "text": "..." }] },
	 * "contents": [
	 * { "role": "user",  "parts": [{ "text": "..." }] },
	 * { "role": "model", "parts": [{ "text": "..." }] },
	 * ...
	 * ],
	 * "generationConfig": { "maxOutputTokens": 500, "temperature": 0.6 }
	 * }
	 * <p>
	 * Lưu ý: Gemini dùng "model" thay vì "assistant" cho role của bot.
	 */
	private String callGemini(String systemPrompt, String userMessage, List<Map<String, String>> history) {
		try {
			// System instruction
			Map<String, Object> systemInstruction = Map.of(
						"parts", List.of(Map.of("text", systemPrompt))
			);

			// Xây dựng contents: history → user message hiện tại
			List<Map<String, Object>> contents = new ArrayList<>();

			if (history != null && !history.isEmpty()) {
				history.stream()
							.filter(h -> h.containsKey("role") && h.containsKey("content"))
							.filter(h -> "user".equals(h.get("role")) || "assistant".equals(h.get("role")))
							.limit(MAX_HISTORY_TURNS)
							.forEach(h -> {
								// Gemini dùng "model" thay vì "assistant"
								String geminiRole = "assistant".equals(h.get("role")) ? "model" : "user";
								contents.add(Map.of(
											"role", geminiRole,
											"parts", List.of(Map.of("text", h.get("content")))
								));
							});
			}

			// Câu hỏi hiện tại
			contents.add(Map.of(
						"role", "user",
						"parts", List.of(Map.of("text", userMessage))
			));

			// Generation config
			Map<String, Object> generationConfig = new HashMap<>();
			generationConfig.put("maxOutputTokens", maxOutputTokens);
			generationConfig.put("temperature", temperature);

			// Full payload
			Map<String, Object> payload = new HashMap<>();
			payload.put("system_instruction", systemInstruction);
			payload.put("contents", contents);
			payload.put("generationConfig", generationConfig);

			// Endpoint: /v1beta/models/{model}:generateContent?key={apiKey}
			String uri = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

			log.debug("Gọi Gemini model={} với {} contents (history: {} lượt)",
						model, contents.size(), history == null ? 0 : Math.min(history.size(), MAX_HISTORY_TURNS));

			String responseBody = webClient.post()
						.uri(uri)
						.bodyValue(payload)
						.retrieve()
						.bodyToMono(String.class)
						.block();

			if (responseBody == null) {
				throw new IllegalStateException("Không nhận được phản hồi từ Gemini");
			}

			// Parse response: candidates[0].content.parts[0].text
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode text = root.path("candidates").path(0)
						.path("content").path("parts").path(0).path("text");

			if (text.isMissingNode() || text.isNull()) {
				// Log full response để debug nếu cần
				log.warn("Gemini trả về response không đúng format: {}", responseBody);
				throw new IllegalStateException("Gemini trả về dữ liệu không hợp lệ");
			}

			return text.asText().trim();

		} catch (WebClientResponseException ex) {
			log.error("Gemini API error {} — {}", ex.getStatusCode(), ex.getResponseBodyAsString());
			throw new IllegalStateException("Không thể kết nối đến Gemini: " + ex.getResponseBodyAsString(), ex);
		} catch (Exception ex) {
			log.error("Lỗi khi gọi Gemini", ex);
			throw new IllegalStateException("Lỗi xử lý chatbot. Vui lòng thử lại sau.", ex);
		}
	}
}
