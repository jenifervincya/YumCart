package com.yumcart.cart;

import com.yumcart.restaurant.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private final String customerId;
    private String restaurantId;
    private final List<CartItem> items = new ArrayList<>();

    public Cart(String customerId) {
        this.customerId = customerId;
    }

    public void addItem(String restaurantId, MenuItem menuItem, int quantity) {
        if (this.restaurantId != null && !this.restaurantId.equals(restaurantId)) {
            throw new CartException("Cart already has items from a different restaurant. Clear it first.");
        }
        if (!menuItem.isAvailable()) {
            throw new CartException("Item " + menuItem.getName() + " is currently unavailable.");
        }
        this.restaurantId = restaurantId;

        for (CartItem item : items) {
            if (item.getMenuItem().getId().equals(menuItem.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new CartItem(menuItem, quantity));
    }

    public void removeItem(String menuItemId) {
        items.removeIf(i -> i.getMenuItem().getId().equals(menuItemId));
        if (items.isEmpty()) restaurantId = null;
    }

    public void clear() {
        items.clear();
        restaurantId = null;
    }

    public double getSubtotal() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public List<CartItem> getItems() { return items; }
}
