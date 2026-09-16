package com.yumcart.delivery;

public class DeliveryAssignmentRequest {
    private final String orderId;
    private final double pickupLat;
    private final double pickupLng;
    private final double dropLat;
    private final double dropLng;

    public DeliveryAssignmentRequest(String orderId, double pickupLat, double pickupLng,
                                      double dropLat, double dropLng) {
        this.orderId = orderId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropLat = dropLat;
        this.dropLng = dropLng;
    }

    public String getOrderId() { return orderId; }
    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public double getDropLat() { return dropLat; }
    public double getDropLng() { return dropLng; }
}
