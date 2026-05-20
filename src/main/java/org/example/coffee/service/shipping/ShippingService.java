package org.example.coffee.service.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.common.Common;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.ForbiddenException;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.UserOrderRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class ShippingService {
    private final GHNService ghnService;
    private final CustomRepository customRepository;
    private final UserOrderRepository userOrderRepository;

    // ==================== ADDRESS DATA ====================

    public JsonNode getProvinces() {
        JsonNode response = ghnService.getProvinces();
        return response.has("data") ? response.get("data") : response;
    }

    public JsonNode getDistricts(Integer provinceId) {
        JsonNode response = ghnService.getDistricts(provinceId);
        return response.has("data") ? response.get("data") : response;
    }

    public JsonNode getWards(Integer districtId) {
        JsonNode response = ghnService.getWards(districtId);
        return response.has("data") ? response.get("data") : response;
    }

    // ==================== SHIPPING FEE ====================

    public Map<String, Object> calculateShippingFee(Integer toDistrictId, String toWardCode) {
        // Lấy service_type_id từ available-services
        JsonNode servicesResponse = ghnService.getAvailableServices(toDistrictId);
        int serviceTypeId = 2; // fallback
        if (servicesResponse.has("data") && servicesResponse.get("data").isArray() && servicesResponse.get("data").size() > 0) {
            serviceTypeId = servicesResponse.get("data").get(0).get("service_type_id").asInt();
        }

        JsonNode response = ghnService.calculateFee(toDistrictId, toWardCode, serviceTypeId);

        Map<String, Object> result = new LinkedHashMap<>();
        if (response.has("data") && response.get("data").has("total")) {
            result.put("shippingFee", response.get("data").get("total").asInt());
            result.put("serviceFee", response.get("data").has("service_fee") ? response.get("data").get("service_fee").asInt() : 0);
        } else {
            String msg = response.has("message") ? response.get("message").asText() : "Không tính được phí";
            throw new BadRequestException("GHN: " + msg);
        }
        return result;
    }

    // ==================== CREATE SHIPPING ====================

    @Transactional
    public Map<String, Object> createShipping(String accessToken, Long orderId) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        UserOrderEntity order = customRepository.getUserOrder(orderId);
        if (!Common.CONFIRMED.equals(order.getState())) {
            throw new BadRequestException("Chỉ có thể tạo đơn giao hàng cho đơn đã xác nhận (CONFIRMED)");
        }
        if (order.getGhnOrderCode() != null) {
            throw new BadRequestException("Đơn hàng đã có mã vận đơn GHN: " + order.getGhnOrderCode());
        }
        if (order.getToDistrictId() == null || order.getToWardCode() == null) {
            throw new BadRequestException("Đơn hàng thiếu thông tin địa chỉ giao hàng");
        }

        // COD nếu thanh toán tiền mặt hoặc chưa thanh toán online
        boolean isCOD = order.getPaymentMethod() == null
                || "Tiền mặt".equals(order.getPaymentMethod())
                || (order.getPaymentMethod().contains("Chờ thanh toán"));

        JsonNode response = ghnService.createShippingOrder(order, isCOD);

        Map<String, Object> result = new LinkedHashMap<>();
        int code = response.has("code") ? response.get("code").asInt() : -1;
        if (code == 200 && response.has("data") && !response.get("data").isNull()) {
            JsonNode data = response.get("data");
            String ghnCode = data.has("order_code") ? data.get("order_code").asText() : null;
            String expectedTime = data.has("expected_delivery_time") ? data.get("expected_delivery_time").asText() : null;
            int totalFee = data.has("total_fee") ? data.get("total_fee").asInt() : 0;

            order.setGhnOrderCode(ghnCode);
            order.setShippingFee(totalFee);
            order.setShippingStatus("ready_to_pick");
            order.setExpectedDelivery(expectedTime);
            order.setState(Common.SHIPPING);
            userOrderRepository.save(order);

            log.info("Đơn #{} - tạo GHN thành công: {} - COD: {}", orderId, ghnCode, isCOD);

            result.put("ghnOrderCode", ghnCode);
            result.put("shippingFee", totalFee);
            result.put("expectedDelivery", expectedTime);
            result.put("state", Common.SHIPPING);
        } else {
            String msg = response.has("message") ? response.get("message").asText() : "Lỗi tạo đơn GHN";
            throw new BadRequestException("GHN: " + msg);
        }
        return result;
    }

    // ==================== TRACKING ====================

    public Map<String, Object> trackShipping(Long orderId) {
        UserOrderEntity order = customRepository.getUserOrder(orderId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("state", order.getState());
        result.put("ghnOrderCode", order.getGhnOrderCode());
        result.put("shippingFee", order.getShippingFee());
        result.put("shippingStatus", order.getShippingStatus());
        result.put("expectedDelivery", order.getExpectedDelivery());

        // Gọi GHN lấy trạng thái mới nhất nếu có mã vận đơn
        if (order.getGhnOrderCode() != null) {
            try {
                JsonNode ghnDetail = ghnService.trackOrder(order.getGhnOrderCode());
                if (ghnDetail.has("data")) {
                    JsonNode data = ghnDetail.get("data");
                    result.put("ghnStatus", data.has("status") ? data.get("status").asText() : null);
                    result.put("ghnLog", data.has("log") ? data.get("log") : null);
                }
            } catch (Exception e) {
                log.warn("Không thể track GHN cho đơn #{}: {}", orderId, e.getMessage());
            }
        }
        return result;
    }

    // ==================== CANCEL ====================

    @Transactional
    public Map<String, Object> cancelShipping(String accessToken, Long orderId) {
        Long shopId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity shopEntity = customRepository.getUserBy(shopId);
        if (shopEntity.getIsShop().equals(Boolean.FALSE)) {
            throw new ForbiddenException(Common.ACTION_FAIL);
        }

        UserOrderEntity order = customRepository.getUserOrder(orderId);
        if (order.getGhnOrderCode() == null) {
            throw new BadRequestException("Đơn hàng chưa có mã vận đơn GHN");
        }

        JsonNode response = ghnService.cancelOrder(order.getGhnOrderCode());
        order.setShippingStatus("cancel");
        order.setState(Common.CANCELED);
        order.setReasonCancellation("Shop hủy giao hàng");
        order.setCancelerId(shopId);
        userOrderRepository.save(order);

        log.info("Đơn #{} - hủy GHN: {}", orderId, order.getGhnOrderCode());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("state", Common.CANCELED);
        return result;
    }

    // ==================== GHN WEBHOOK ====================

//    @Transactional
//    public void handleGHNWebhook(JsonNode body) {
//        try {
//            String ghnOrderCode = body.has("OrderCode") ? body.get("OrderCode").asText() : null;
//            String status = body.has("Status") ? body.get("Status").asText() : null;
//
//            if (ghnOrderCode == null || status == null) {
//                log.warn("GHN webhook - thiếu OrderCode hoặc Status");
//                return;
//            }
//
//            log.info("GHN webhook - {} - status: {}", ghnOrderCode, status);
//
//            // Tìm đơn hàng theo ghnOrderCode
//            UserOrderEntity order = userOrderRepository.findByGhnOrderCode(ghnOrderCode);
//            if (order == null) {
//                log.warn("GHN webhook - không tìm thấy đơn cho mã: {}", ghnOrderCode);
//                return;
//            }
//
//            order.setShippingStatus(status);
//
//            // Map GHN status → hệ thống state
//            switch (status) {
//                case "ready_to_pick":
//                case "picking":
//                case "picked":
//                case "storing":
//                case "transporting":
//                case "sorting":
//                    order.setState(Common.SHIPPING);
//                    break;
//                case "delivering":
//                    order.setState(Common.DELIVERING);
//                    break;
//                case "delivered":
//                    order.setState(Common.COMPLETED);
//                    log.info("Đơn #{} - GHN giao thành công", order.getId());
//                    break;
//                case "delivery_fail":
//                    order.setState(Common.DELIVERY_FAILED);
//                    break;
//                case "waiting_to_return":
//                case "return":
//                case "return_transporting":
//                case "return_sorting":
//                case "returning":
//                    order.setState(Common.RETURNING);
//                    break;
//                case "returned":
//                    order.setState(Common.RETURNED);
//                    break;
//                case "cancel":
//                    order.setState(Common.CANCELED);
//                    order.setReasonCancellation("GHN hủy đơn");
//                    break;
//                default:
//                    log.warn("GHN webhook - trạng thái không xử lý: {}", status);
//                    break;
//            }
//
//            userOrderRepository.save(order);
//        } catch (Exception e) {
//            log.error("GHN webhook error: {}", e.getMessage());
//        }
//    }
}
