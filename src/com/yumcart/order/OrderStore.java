package com.yumcart.order;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OrderStore {
    private final Map<String, Order> orders = new HashMap<>();

    public void addOrder(Order order) {
        orders.put(order.getOrderId(), order);
    }

    public boolean exists(String orderId) {
        return orders.containsKey(orderId);
    }

    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Map<String, Order> getAll() { return orders; }
}
