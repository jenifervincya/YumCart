package com.yumcart;

import com.yumcart.api.OrderApiServer;
import com.yumcart.delivery.DeliveryAssignmentProcessor;
import com.yumcart.delivery.DeliveryPartnerRegistry;
import com.yumcart.notification.CustomerNotifier;
import com.yumcart.notification.RestaurantNotifier;
import com.yumcart.order.*;
import com.yumcart.payment.MockPaymentGateway;
import com.yumcart.payment.PaymentProcessor;
import com.yumcart.payment.PaymentValidator;
import com.yumcart.pricing.PricingService;
import com.yumcart.rating.Rating;
import com.yumcart.rating.RatingStore;
import com.yumcart.rating.RatingTargetType;
import com.yumcart.restaurant.MenuItem;
import com.yumcart.restaurant.Restaurant;
import com.yumcart.restaurant.RestaurantStore;
import com.yumcart.user.DeliveryPartner;

import java.util.UUID;

public class ApiServerMain {

    public static void main(String[] args) throws Exception {
        StateChangeValidator validator = new StateChangeValidator();
        OrderStore store = new OrderStore();
        StateProcessor processor = new StateProcessor(validator, store);
        processor.addObserver(new CustomerNotifier());
        processor.addObserver(new RestaurantNotifier());

        RestaurantStore restaurants = new RestaurantStore();
        seedRestaurants(restaurants);

        RatingStore ratings = new RatingStore();
        seedRatings(ratings, "R1", 5, 4, 5, 4, 5, 4, 4);
        seedRatings(ratings, "R2", 4, 4, 5, 3, 4);
        seedRatings(ratings, "R3", 4, 5, 4, 4, 3, 5);
        seedRatings(ratings, "R4", 4, 3, 4, 4);
        seedRatings(ratings, "R5", 5, 5, 4, 5, 5, 4);
        seedRatings(ratings, "R6", 5, 4, 5, 5);

        DeliveryPartnerRegistry registry = new DeliveryPartnerRegistry();
        for (int i = 1; i <= 50; i++) {
            DeliveryPartner p = new DeliveryPartner("D" + i, "Rider " + i, "rider" + i + "@yumcart.com", "90000000" + String.format("%02d", i));
            p.updateLocation(12.97, 77.59);
            registry.register(p);
        }
        DeliveryAssignmentProcessor assignments = new DeliveryAssignmentProcessor(registry);

        PaymentProcessor payments = new PaymentProcessor(new PaymentValidator(), new MockPaymentGateway());

        OrderApiServer server = new OrderApiServer(store, processor, restaurants, ratings,
                new PricingService(), payments, assignments);
        server.start(8787);

        System.out.println("Leave this terminal open. Serve docs/ and open it in your browser.");
        System.out.println("Press Ctrl+C to stop.");
        Thread.currentThread().join();
    }

    private static void seedRestaurants(RestaurantStore rs) {
        addRestaurant(rs, "R1", "Spice Villa", "12 MG Road", new String[][]{
                {"R1M1", "Chicken Biryani", "220", "Main"},
                {"R1M2", "Paneer Butter Masala", "240", "Main"},
                {"R1M3", "Butter Naan", "45", "Breads"},
                {"R1M4", "Mango Lassi", "80", "Beverage"},
                {"R1M5", "Gulab Jamun", "90", "Dessert"}});
        addRestaurant(rs, "R2", "Pizza Palace", "21 Brigade Road", new String[][]{
                {"R2M1", "Margherita Pizza", "249", "Pizza"},
                {"R2M2", "Farmhouse Pizza", "349", "Pizza"},
                {"R2M3", "Garlic Bread", "129", "Starters"},
                {"R2M4", "Choco Lava Cake", "109", "Dessert"},
                {"R2M5", "Coke", "60", "Beverage"}});
        addRestaurant(rs, "R3", "Burger Barn", "5 Church Street", new String[][]{
                {"R3M1", "Classic Veg Burger", "129", "Burgers"},
                {"R3M2", "Chicken Zinger Burger", "189", "Burgers"},
                {"R3M3", "Peri Peri Fries", "99", "Sides"},
                {"R3M4", "Oreo Shake", "139", "Beverage"}});
        addRestaurant(rs, "R4", "Dragon Wok", "77 Lake View Road", new String[][]{
                {"R4M1", "Spring Rolls", "129", "Starters"},
                {"R4M2", "Chicken Manchurian", "219", "Starters"},
                {"R4M3", "Veg Hakka Noodles", "179", "Noodles"},
                {"R4M4", "Schezwan Fried Rice", "169", "Rice"}});
        addRestaurant(rs, "R5", "South Spice", "8 Temple Street", new String[][]{
                {"R5M1", "Masala Dosa", "99", "Tiffin"},
                {"R5M2", "Idli Sambar (3 pcs)", "69", "Tiffin"},
                {"R5M3", "Medu Vada (2 pcs)", "59", "Tiffin"},
                {"R5M4", "Filter Coffee", "40", "Beverage"}});
        addRestaurant(rs, "R6", "Sweet Tooth", "3 Park Avenue", new String[][]{
                {"R6M1", "Walnut Brownie", "99", "Dessert"},
                {"R6M2", "Rasmalai", "120", "Dessert"},
                {"R6M3", "Ice Cream Sundae", "149", "Dessert"},
                {"R6M4", "Cold Coffee", "110", "Beverage"}});
    }

    private static void addRestaurant(RestaurantStore rs, String id, String name, String address, String[][] items) {
        Restaurant r = new Restaurant(id, name, address);
        for (String[] it : items) {
            r.addMenuItem(new MenuItem(it[0], it[1], Double.parseDouble(it[2]), it[3]));
        }
        rs.addRestaurant(r);
    }

    private static void seedRatings(RatingStore store, String restaurantId, int... stars) {
        for (int s : stars) {
            store.addRating(new Rating(UUID.randomUUID().toString(), "seed", "seed",
                    restaurantId, RatingTargetType.RESTAURANT, s, ""));
        }
    }
}
