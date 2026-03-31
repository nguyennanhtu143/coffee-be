package org.example.coffee.service.order.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coffee.entity.UserOrderEntity;
import org.example.coffee.repository.UserOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SePayPayment implements Payment {
    private final UserOrderRepository userOrderRepository;

    @Value("${sepay.bank-name}")
    private String bankName;

    @Value("${sepay.account-number}")
    private String accountNumber;

    @Value("${sepay.account-name}")
    private String accountName;

    @Value("${sepay.transfer-prefix}")
    private String transferPrefix;

    public SePayPayment(UserOrderRepository userOrderRepository) {
        this.userOrderRepository = userOrderRepository;
    }

    @Override
    public String payment(UserOrderEntity userOrderEntity, String ipAddress) {
        userOrderEntity.setPaymentMethod("SePay - Chờ thanh toán");
        userOrderRepository.save(userOrderEntity);

        // Nội dung chuyển khoản = prefix + orderId
        String transferContent = transferPrefix + userOrderEntity.getId();

        // Tạo QR URL theo chuẩn VietQR
        String qrUrl = "https://qr.sepay.vn/img?acc=" + accountNumber
                + "&bank=" + bankName
                + "&amount=" + userOrderEntity.getTotalPrice()
                + "&des=" + transferContent;

        // Trả về JSON chứa thông tin chuyển khoản
        try {
            Map<String, Object> bankInfo = new LinkedHashMap<>();
            bankInfo.put("bankName", bankName);
            bankInfo.put("accountNumber", accountNumber);
            bankInfo.put("accountName", accountName);
            bankInfo.put("amount", userOrderEntity.getTotalPrice());
            bankInfo.put("transferContent", transferContent);
            bankInfo.put("qrUrl", qrUrl);
            bankInfo.put("orderId", userOrderEntity.getId());
            return new ObjectMapper().writeValueAsString(bankInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
