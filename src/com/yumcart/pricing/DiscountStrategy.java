package com.yumcart.pricing;

public interface DiscountStrategy {
    double apply(double amount);
}
