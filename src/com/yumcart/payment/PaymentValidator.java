package com.yumcart.payment;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PaymentValidator {
    private final Map<PaymentStatus, Set<PaymentStatus>> transitionTable = new EnumMap<>(PaymentStatus.class);

    public PaymentValidator() {
        transitionTable.put(PaymentStatus.PENDING, EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED));
        transitionTable.put(PaymentStatus.SUCCESS, EnumSet.of(PaymentStatus.REFUNDED));
        transitionTable.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        transitionTable.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
    }

    public boolean isValid(PaymentStatus current, PaymentStatus requested) {
        return transitionTable.getOrDefault(current, EnumSet.noneOf(PaymentStatus.class)).contains(requested);
    }
}
