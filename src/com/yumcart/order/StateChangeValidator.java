package com.yumcart.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class StateChangeValidator {
    private final Map<OrderStatus, Set<OrderStatus>> transitionTable = new EnumMap<>(OrderStatus.class);

    public StateChangeValidator() {
        transitionTable.put(OrderStatus.PLACED,           EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        transitionTable.put(OrderStatus.CONFIRMED,        EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        transitionTable.put(OrderStatus.SHIPPED,          EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED));
        transitionTable.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.NOT_DELIVERED));
        transitionTable.put(OrderStatus.NOT_DELIVERED,    EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED));
        transitionTable.put(OrderStatus.DELIVERED,        EnumSet.of(OrderStatus.RETURNED));
        transitionTable.put(OrderStatus.CANCELLED,        EnumSet.noneOf(OrderStatus.class));
        transitionTable.put(OrderStatus.RETURNED,         EnumSet.noneOf(OrderStatus.class));
    }

    public boolean isValid(OrderStatus current, OrderStatus requested) {
        return transitionTable.getOrDefault(current, EnumSet.noneOf(OrderStatus.class)).contains(requested);
    }

    public java.util.Set<OrderStatus> allowedNext(OrderStatus current) {
        return transitionTable.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
    }
}
