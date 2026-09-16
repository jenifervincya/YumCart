package com.yumcart.order;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private final String orderId;
    private OrderStatus currentStatus;
    private final List<OrderStatus> history = new ArrayList<>();

    public Order(String orderId, OrderStatus initialStatus) {
        this.orderId = orderId;
        this.currentStatus = initialStatus;
        this.history.add(initialStatus);
    }

    public String getOrderId() { return orderId; }
    public OrderStatus getCurrentStatus() { return currentStatus; }
    public List<OrderStatus> getHistory() { return history; }

    public void updateStatus(OrderStatus newStatus) {
        this.currentStatus = newStatus;
        this.history.add(newStatus);
    }
}
