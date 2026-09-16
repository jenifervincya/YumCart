package com.yumcart.user;

public class Customer extends User {
    private String deliveryAddress;

    public Customer(String id, String name, String email, String phone, String deliveryAddress) {
        super(id, name, email, phone, UserRole.CUSTOMER);
        this.deliveryAddress = deliveryAddress;
    }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
