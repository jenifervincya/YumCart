package com.yumcart.payment;

import java.util.Random;

public class MockPaymentGateway implements PaymentGateway {
    private final Random random = new Random();

    @Override
    public boolean charge(Payment payment) {
        if (payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            return true;
        }
        return random.nextInt(10) != 0; // ~90% success, for demo purposes
    }
}
