package com.yumcart.restaurant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Restaurant {
    private final String id;
    private String name;
    private String address;
    private boolean open;
    private final List<MenuItem> menu = new ArrayList<>();

    public Restaurant(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.open = true;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    public void addMenuItem(MenuItem item) { menu.add(item); }

    public void removeMenuItem(String itemId) {
        menu.removeIf(m -> m.getId().equals(itemId));
    }

    public Optional<MenuItem> getMenuItem(String itemId) {
        return menu.stream().filter(m -> m.getId().equals(itemId)).findFirst();
    }

    public List<MenuItem> getMenu() { return menu; }
}
