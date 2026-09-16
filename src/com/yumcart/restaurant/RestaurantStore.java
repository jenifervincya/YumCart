package com.yumcart.restaurant;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RestaurantStore {
    private final Map<String, Restaurant> restaurants = new HashMap<>();

    public void addRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.getId(), restaurant);
    }

    public Optional<Restaurant> getRestaurant(String id) {
        return Optional.ofNullable(restaurants.get(id));
    }

    public Map<String, Restaurant> getAll() { return restaurants; }
}
