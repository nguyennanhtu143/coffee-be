package org.example.coffee.service.order.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.common.Common;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.CartMapRepository;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.UserOrderRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {
    private final CustomRepository customRepository;
    private final PaymentMethod paymentMethod;
    private final UserOrderRepository userOrderRepository;
    private final CartMapRepository cartMapRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${sepay.api-key}")
    private String sePayApiKey;

    @Value("${sepay.transfer-prefix}")
    private String transferPrefix;

    public PaymentService(CustomRepository customRepository,
                          PaymentMethod paymentMethod,
                          UserOrderRepository userOrderRepository,
                          CartMapRepository cartMapRepository) {
        this.customRepository = customRepository;
        this.paymentMethod = paymentMethod;
        this.userOrderRepository = userOrderRepository;
        this.cartMapRepository = cartMapRepository;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public String processPayment(String accessToken, String method, Long orderId, String ipAddress) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserOrderEntity userOrderEntity = customRepository.getUserOrder(orderId);
        if (!userOrderEntity.getUserId().equals(userId)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        log.info("User {} thanh toán đơn #{} - phương thức: {}", userId, orderId, method);
        Payment payment = paymentMethod.findPaymentMethod(method);
        return payment.payment(userOrderEntity, ipAddress);
    }

    @Transactional
    public Map<String, Object> checkPaymentStatus(String accessToken, Long orderId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserOrderEntity order = customRepository.getUserOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);

        // Đã xác nhận thanh toán rồi
        if (order.getPaymentMethod() != null
                && order.getPaymentMethod().startsWith("SePay")
                && !order.getPaymentMethod().contains("Chờ thanh toán")) {
            result.put("paid", true);
            result.put("message", "Đã thanh toán");
            return result;
        }

        // Gọi SePay API kiểm tra
        String transferContent = transferPrefix + orderId;
        try {
            String url = "https://my.sepay.vn/userapi/transactions/list"
                    + "?transaction_content=" + transferContent
                    + "&limit=10";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + sePayApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseBody = objectMapper.readTree(response.body());

            if (responseBody.has("transactions") && responseBody.get("transactions").isArray()) {
                for (JsonNode tx : responseBody.get("transactions")) {
                    int txAmount = tx.has("amount_in") ? tx.get("amount_in").asInt() : 0;
                    String txContent = tx.has("transaction_content") ? tx.get("transaction_content").asText() : "";
                    String gateway = tx.has("gateway") ? tx.get("gateway").asText() : "";

                    if (txContent.toUpperCase().contains(transferContent.toUpperCase())
                            && order.getTotalPrice() != null
                            && txAmount >= order.getTotalPrice()) {

                        confirmPayment(order, gateway);

                        result.put("paid", true);
                        result.put("message", "Thanh toán thành công");
                        return result;
                    }
                }
            }

            result.put("paid", false);
            result.put("message", "Chưa nhận được thanh toán");
            return result;

        } catch (Exception e) {
            log.error("Lỗi kiểm tra SePay cho đơn #{}: {}", orderId, e.getMessage());
            result.put("paid", false);
            result.put("message", "Lỗi kiểm tra thanh toán");
            return result;
        }
    }

    @Transactional
    public String handleSePayWebhook(JsonNode body) {
        try {
            String content = body.has("content") ? body.get("content").asText() : "";
            int amount = body.has("transferAmount") ? body.get("transferAmount").asInt() : 0;
            String gateway = body.has("gateway") ? body.get("gateway").asText() : "";

            log.info("SePay webhook - content: {}, amount: {}, gateway: {}", content, amount, gateway);

            String prefix = transferPrefix.toUpperCase();
            if (!content.toUpperCase().contains(prefix)) {
                return "OK";
            }

            String orderIdStr = content.toUpperCase()
                    .substring(content.toUpperCase().indexOf(prefix) + prefix.length())
                    .replaceAll("[^0-9]", "");
            if (orderIdStr.isEmpty()) {
                return "OK";
            }

            Long orderId = Long.parseLong(orderIdStr);
            UserOrderEntity order = customRepository.getUserOrder(orderId);

            if (order.getTotalPrice() != null && amount >= order.getTotalPrice()) {
                confirmPayment(order, gateway);
            } else {
                log.warn("Đơn #{} - số tiền không khớp: chuyển {} - cần {}", orderId, amount, order.getTotalPrice());
            }

            return "OK";
        } catch (Exception e) {
            log.error("SePay webhook error: {}", e.getMessage());
            return "OK";
        }
    }

    /**
     * Xác nhận thanh toán: cập nhật trạng thái + xoá giỏ hàng pending
     */
    private void confirmPayment(UserOrderEntity order, String gateway) {
        order.setPaymentMethod("SePay - " + gateway);
        order.setState(Common.CONFIRMED);

        // Xoá giỏ hàng đã lưu trước đó
        if (order.getPendingCartIds() != null && !order.getPendingCartIds().isEmpty()) {
            List<Long> cartIds = Arrays.stream(order.getPendingCartIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            cartMapRepository.deleteAllByIdIn(cartIds);
            order.setPendingCartIds(null);
            log.info("Đơn #{} - đã xoá {} sản phẩm khỏi giỏ hàng", order.getId(), cartIds.size());
        }

        userOrderRepository.save(order);
        log.info("Đơn #{} thanh toán SePay thành công qua {} - tự động CONFIRMED", order.getId(), gateway);
    }
}
