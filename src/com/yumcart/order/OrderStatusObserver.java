package com.yumcart.order;

public interface OrderStatusObserver {
    void onStatusChanged(Order order, OrderStatus previousStatus, OrderStatus newStatus);
}
