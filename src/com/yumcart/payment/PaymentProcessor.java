package com.yumcart.payment;

public class PaymentProcessor {
    private final PaymentValidator validator;
    private final PaymentGateway gateway;

    public PaymentProcessor(PaymentValidator validator, PaymentGateway gateway) {
        this.validator = validator;
        this.gateway = gateway;
    }

    public boolean process(Payment payment) {
        boolean success = gateway.charge(payment);
        PaymentStatus target = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        if (!validator.isValid(payment.getStatus(), target)) {
            System.out.println("[REJECTED] Payment transition " + payment.getStatus() + " -> " + target);
            return false;
        }
        payment.setStatus(target);
        System.out.println("[PAYMENT] " + payment.getPaymentId() + " -> " + target);
        return success;
    }
}
