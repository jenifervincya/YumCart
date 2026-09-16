package com.yumcart.order;

public class OrderItem {
    private final String menuItemId;
    private final String name;
    private final double priceAtOrderTime;
    private final int quantity;

    public OrderItem(String menuItemId, String name, double priceAtOrderTime, int quantity) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.priceAtOrderTime = priceAtOrderTime;
        this.quantity = quantity;
    }

    public String getMenuItemId() { return menuItemId; }
    public String getName() { return name; }
    public double getPriceAtOrderTime() { return priceAtOrderTime; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return priceAtOrderTime * quantity; }
}
