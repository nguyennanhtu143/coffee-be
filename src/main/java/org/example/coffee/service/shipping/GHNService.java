package org.example.coffee.service.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.config.GHNConfig;
import org.example.coffee.entity.UserOrderEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class GHNService {
    private final GHNConfig ghnConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GHNService(GHNConfig ghnConfig) {
        this.ghnConfig = ghnConfig;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== MASTER DATA ====================

    public JsonNode getProvinces() {
        return callGHN("GET", "/shiip/public-api/master-data/province", null, false);
    }

    public JsonNode getDistricts(Integer provinceId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("province_id", provinceId);
        return callGHN("POST", "/shiip/public-api/master-data/district", body, false);
    }

    public JsonNode getWards(Integer districtId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("district_id", districtId);
        return callGHN("POST", "/shiip/public-api/master-data/ward", body, false);
    }

    // ==================== SHIPPING FEE ====================

    public JsonNode getAvailableServices(Integer toDistrictId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("shop_id", Integer.parseInt(ghnConfig.getShopId()));
        body.put("from_district", ghnConfig.getFromDistrictId());
        body.put("to_district", toDistrictId);
        return callGHN("POST", "/shiip/public-api/v2/shipping-order/available-services", body, true);
    }

    public JsonNode calculateFee(Integer toDistrictId, String toWardCode, Integer serviceTypeId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("shop_id", Integer.parseInt(ghnConfig.getShopId()));
        body.put("service_type_id", serviceTypeId);
        body.put("from_district_id", ghnConfig.getFromDistrictId());
        body.put("from_ward_code", ghnConfig.getFromWardCode());
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("weight", ghnConfig.getDefaultWeight());
        body.put("length", ghnConfig.getDefaultLength());
        body.put("width", ghnConfig.getDefaultWidth());
        body.put("height", ghnConfig.getDefaultHeight());
        body.put("insurance_value", 0);
        return callGHN("POST", "/shiip/public-api/v2/shipping-order/fee", body, true);
    }

    // ==================== SHIPPING ORDER ====================

    public JsonNode createShippingOrder(UserOrderEntity order, boolean isCOD) {
        ObjectNode body = objectMapper.createObjectNode();

				body.put("from_name", ghnConfig.getFromName());
				body.put("from_phone", ghnConfig.getFromPhone());
				body.put("from_address", ghnConfig.getFromAddress());
				body.put("from_district_id", ghnConfig.getFromDistrictId());
				body.put("from_ward_code", ghnConfig.getFromWardCode());

        // Receiver info
        body.put("to_name", order.getFullName());
        body.put("to_phone", order.getPhoneNumber());
        body.put("to_address", order.getAddress());
        body.put("to_district_id", order.getToDistrictId());
        body.put("to_ward_code", order.getToWardCode());

        // Package info
        body.put("weight", ghnConfig.getDefaultWeight());
        body.put("length", ghnConfig.getDefaultLength());
        body.put("width", ghnConfig.getDefaultWidth());
        body.put("height", ghnConfig.getDefaultHeight());

        // Service
        body.put("service_type_id", 2); // Standard
        body.put("payment_type_id", 1); // Shop trả phí ship

        // COD
        body.put("cod_amount", isCOD ? order.getTotalPrice() : 0);

        // Notes
        body.put("required_note", "CHOXEMHANGKHONGTHU");
        body.put("client_order_code", "DH" + order.getId());
        body.put("content", "Don hang ca phe #" + order.getId());

        // Items
        ArrayNode items = objectMapper.createArrayNode();
        ObjectNode item = objectMapper.createObjectNode();
        item.put("name", "Ca phe - Don hang #" + order.getId());
        item.put("quantity", 1);
        item.put("weight", ghnConfig.getDefaultWeight());
        items.add(item);
        body.set("items", items);

        return callGHN("POST", "/shiip/public-api/v2/shipping-order/create", body, true);
    }

    public JsonNode trackOrder(String ghnOrderCode) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("order_code", ghnOrderCode);
        return callGHN("POST", "/shiip/public-api/v2/shipping-order/detail", body, true);
    }

    public JsonNode cancelOrder(String ghnOrderCode) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode codes = objectMapper.createArrayNode();
        codes.add(ghnOrderCode);
        body.set("order_codes", codes);
        return callGHN("POST", "/shiip/public-api/v2/switch-status/cancel", body, true);
    }

    // ==================== HTTP HELPER ====================

    private JsonNode callGHN(String method, String path, JsonNode body, boolean requireShopId) {
        try {
            String url = ghnConfig.getBaseUrl() + path;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Token", ghnConfig.getToken())
                    .header("Content-Type", "application/json");

            if (requireShopId) {
                requestBuilder.header("ShopId", ghnConfig.getShopId());
            }
						log.info("GHN Request - Token: {}, ShopId: {}, Endpoint: {}", ghnConfig.getToken(), ghnConfig.getShopId(), url);

            if ("GET".equals(method)) {
                requestBuilder.GET();
            } else {
                String bodyStr = body != null ? objectMapper.writeValueAsString(body) : "{}";
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyStr));
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode responseNode = objectMapper.readTree(response.body());

            int code = responseNode.has("code") ? responseNode.get("code").asInt() : -1;
            if (code != 200) {
                String message = responseNode.has("message") ? responseNode.get("message").asText() : "Unknown error";
                log.error("GHN API error: {} - {} - {}", path, code, message);
            }

            return responseNode;
        } catch (Exception e) {
            log.error("GHN API call failed: {} - {}", path, e.getMessage());
            throw new RuntimeException("Lỗi kết nối GHN: " + e.getMessage(), e);
        }
    }
}
