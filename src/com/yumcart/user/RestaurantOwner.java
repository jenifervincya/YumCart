package com.yumcart.user;

public class RestaurantOwner extends User {
    private String restaurantId;

    public RestaurantOwner(String id, String name, String email, String phone, String restaurantId) {
        super(id, name, email, phone, UserRole.RESTAURANT_OWNER);
        this.restaurantId = restaurantId;
    }

    public String getRestaurantId() { return restaurantId; }
}
