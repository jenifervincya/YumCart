package com.yumcart.order;

public class StateChangeRequest {
    private final String orderId;
    private final OrderStatus requestedStatus;

    public StateChangeRequest(String orderId, OrderStatus requestedStatus) {
        this.orderId = orderId;
        this.requestedStatus = requestedStatus;
    }

    public String getOrderId() { return orderId; }
    public OrderStatus getRequestedStatus() { return requestedStatus; }
}
