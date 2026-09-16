package com.yumcart.user;

public class DeliveryPartner extends User {
    private DeliveryPartnerStatus status;
    private double currentLat;
    private double currentLng;

    public DeliveryPartner(String id, String name, String email, String phone) {
        super(id, name, email, phone, UserRole.DELIVERY_PARTNER);
        this.status = DeliveryPartnerStatus.AVAILABLE;
    }

    public DeliveryPartnerStatus getStatus() { return status; }
    public void setStatus(DeliveryPartnerStatus status) { this.status = status; }

    public void updateLocation(double lat, double lng) {
        this.currentLat = lat;
        this.currentLng = lng;
    }

    public double getCurrentLat() { return currentLat; }
    public double getCurrentLng() { return currentLng; }
}
