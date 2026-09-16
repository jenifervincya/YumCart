package com.yumcart.payment;

public interface PaymentGateway {
    boolean charge(Payment payment);
}
