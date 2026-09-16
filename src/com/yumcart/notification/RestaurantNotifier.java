package com.yumcart.notification;

import com.yumcart.order.Order;
import com.yumcart.order.OrderStatus;
import com.yumcart.order.OrderStatusObserver;

public class RestaurantNotifier implements OrderStatusObserver {
    @Override
    public void onStatusChanged(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        System.out.println("[NOTIFY restaurant] Order " + order.getOrderId() + " moved to " + newStatus);
    }
}
