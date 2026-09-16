package com.yumcart.pricing;

public class PricingService {
    private static final double DELIVERY_FEE = 30.0;
    private static final double TAX_RATE = 0.05;

    public double calculateTotal(double subtotal, DiscountStrategy discountStrategy) {
        double discounted = discountStrategy != null ? discountStrategy.apply(subtotal) : subtotal;
        double tax = discounted * TAX_RATE;
        return discounted + tax + DELIVERY_FEE;
    }
}
