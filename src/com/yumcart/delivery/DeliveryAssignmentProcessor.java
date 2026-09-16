package com.yumcart.delivery;

import com.yumcart.user.DeliveryPartner;
import com.yumcart.user.DeliveryPartnerStatus;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

// Mirrors the Queue-drain pattern from StateProcessor: assignment requests
// queue up, one gets processed at a time, and an unassignable request is
// logged and skipped rather than blocking the ones behind it.
public class DeliveryAssignmentProcessor {
    private final Queue<DeliveryAssignmentRequest> queue = new LinkedList<>();
    private final DeliveryPartnerRegistry registry;

    public DeliveryAssignmentProcessor(DeliveryPartnerRegistry registry) {
        this.registry = registry;
    }

    public void enqueueRequest(DeliveryAssignmentRequest request) {
        queue.add(request);
    }

    public Optional<DeliveryPartner> processNext() {
        DeliveryAssignmentRequest request = queue.poll();
        if (request == null) return Optional.empty();

        Optional<DeliveryPartner> match = registry.findNearestAvailable(
                request.getPickupLat(), request.getPickupLng());

        if (match.isEmpty()) {
            System.out.println("[NO PARTNER] No available partner for order " + request.getOrderId());
            return Optional.empty();
        }

        DeliveryPartner partner = match.get();
        partner.setStatus(DeliveryPartnerStatus.ASSIGNED);
        System.out.println("[ASSIGNED] Order " + request.getOrderId() + " -> partner " + partner.getId());
        return Optional.of(partner);
    }

    public void processQueue() {
        while (!queue.isEmpty()) {
            processNext();
        }
    }
}
