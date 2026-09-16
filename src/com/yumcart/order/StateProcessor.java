package com.yumcart.order;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class StateProcessor {
    private final Queue<StateChangeRequest> queue = new LinkedList<>();
    private final StateChangeValidator validator;
    private final OrderStore store;
    private final List<OrderStatusObserver> observers = new ArrayList<>();

    public StateProcessor(StateChangeValidator validator, OrderStore store) {
        this.validator = validator;
        this.store = store;
    }

    public void enqueueRequest(StateChangeRequest request) {
        queue.add(request);
    }

    public void addObserver(OrderStatusObserver observer) {
        observers.add(observer);
    }

    public void processQueue() {
        while (!queue.isEmpty()) {
            StateChangeRequest request = queue.poll();
            Order order = store.getOrder(request.getOrderId()).orElse(null);
            if (order == null) {
                System.out.println("[REJECTED] No such order " + request.getOrderId());
                continue;
            }
            OrderStatus previous = order.getCurrentStatus();
            if (!validator.isValid(previous, request.getRequestedStatus())) {
                System.out.println("[INVALID] Cannot move " + order.getOrderId()
                        + " from " + previous + " to " + request.getRequestedStatus() + ". Skipped.");
                continue;
            }
            order.updateStatus(request.getRequestedStatus());
            System.out.println("[OK] " + order.getOrderId() + ": " + previous + " -> " + request.getRequestedStatus());
            for (OrderStatusObserver observer : observers) {
                observer.onStatusChanged(order, previous, request.getRequestedStatus());
            }
        }
    }
}
