package com.yumcart.notification;

import com.yumcart.order.Order;
import com.yumcart.order.OrderStatus;
import com.yumcart.order.OrderStatusObserver;

public class CustomerNotifier implements OrderStatusObserver {
    @Override
    public void onStatusChanged(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        System.out.println("[NOTIFY customer] Order " + order.getOrderId() + " is now " + newStatus);
    }
}
