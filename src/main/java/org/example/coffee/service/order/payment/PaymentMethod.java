package org.example.coffee.service.order.payment;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentMethod {
    private final SePayPayment sePayPayment;
    private final CashPayment cashPayment;

    public Payment findPaymentMethod(String paymentMethod) {
        if ("cash".equals(paymentMethod)) {
            return cashPayment;
        }
        return sePayPayment;
    }
}
