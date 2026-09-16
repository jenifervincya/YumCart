package com.yumcart.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private final String orderId;
    private final String customerId;
    private final String restaurantId;
    private final List<OrderItem> items;
    private OrderStatus currentStatus;
    private final List<OrderStatus> history = new ArrayList<>();
    private double totalAmount;

    // Full constructor — used by the real order flow (restaurant + cart checkout)
    public Order(String orderId, String customerId, String restaurantId, List<OrderItem> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.currentStatus = OrderStatus.PLACED;
        this.history.add(OrderStatus.PLACED);
        this.totalAmount = items.stream().mapToDouble(OrderItem::getLineTotal).sum();
    }

    // Simple constructor — kept for the sample-order generator / manual CLI testing
    public Order(String orderId, OrderStatus initialStatus) {
        this.orderId = orderId;
        this.customerId = null;
        this.restaurantId = null;
        this.items = Collections.emptyList();
        this.currentStatus = initialStatus;
        this.history.add(initialStatus);
        this.totalAmount = 0;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getCurrentStatus() { return currentStatus; }
    public List<OrderStatus> getHistory() { return history; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public void updateStatus(OrderStatus newStatus) {
        this.currentStatus = newStatus;
        this.history.add(newStatus);
    }
}
