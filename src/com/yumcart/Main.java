package com.yumcart;

import com.yumcart.cart.Cart;
import com.yumcart.delivery.DeliveryAssignmentProcessor;
import com.yumcart.delivery.DeliveryAssignmentRequest;
import com.yumcart.delivery.DeliveryPartnerRegistry;
import com.yumcart.notification.CustomerNotifier;
import com.yumcart.notification.RestaurantNotifier;
import com.yumcart.order.*;
import com.yumcart.payment.*;
import com.yumcart.pricing.PercentageDiscount;
import com.yumcart.pricing.PricingService;
import com.yumcart.restaurant.MenuItem;
import com.yumcart.restaurant.Restaurant;
import com.yumcart.restaurant.RestaurantStore;
import com.yumcart.user.Customer;
import com.yumcart.user.DeliveryPartner;

import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        StateChangeValidator validator = new StateChangeValidator();
        OrderStore store = new OrderStore();
        StateProcessor processor = new StateProcessor(validator, store);
        processor.addObserver(new CustomerNotifier());
        processor.addObserver(new RestaurantNotifier());

        System.out.println("=== YumCart end-to-end demo ===\n");
        runFullOrderFlow(store, processor);

        System.out.println("\n=== Original order-tracking CLI (manual testing) ===");
        runInteractiveCli(store, validator, processor);
    }

    // Demonstrates restaurant -> cart -> order -> payment -> delivery assignment
    static void runFullOrderFlow(OrderStore orderStore, StateProcessor processor) {
        RestaurantStore restaurantStore = new RestaurantStore();
        Restaurant restaurant = new Restaurant("R1", "Spice Villa", "12 MG Road");
        MenuItem biryani = new MenuItem("M1", "Chicken Biryani", 220.0, "Main");
        MenuItem lassi = new MenuItem("M2", "Mango Lassi", 80.0, "Beverage");
        restaurant.addMenuItem(biryani);
        restaurant.addMenuItem(lassi);
        restaurantStore.addRestaurant(restaurant);

        Customer customer = new Customer("C1", "Jeni", "jeni@example.com", "9999999999", "45 Park Street");

        Cart cart = new Cart(customer.getId());
        cart.addItem(restaurant.getId(), biryani, 2);
        cart.addItem(restaurant.getId(), lassi, 1);
        System.out.println("Cart subtotal: " + cart.getSubtotal());

        PricingService pricing = new PricingService();
        double total = pricing.calculateTotal(cart.getSubtotal(), new PercentageDiscount(10));
        System.out.println("Total after 10% discount, tax, delivery fee: " + total);

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(ci -> new OrderItem(ci.getMenuItem().getId(), ci.getMenuItem().getName(),
                        ci.getMenuItem().getPrice(), ci.getQuantity()))
                .toList();

        Order order = new Order("ORD-1001", customer.getId(), restaurant.getId(), orderItems);
        order.setTotalAmount(total);
        orderStore.addOrder(order);
        System.out.println("Order placed: " + order.getOrderId() + " total=" + order.getTotalAmount());

        PaymentValidator paymentValidator = new PaymentValidator();
        PaymentGateway gateway = new MockPaymentGateway();
        PaymentProcessor paymentProcessor = new PaymentProcessor(paymentValidator, gateway);
        Payment payment = new Payment("PAY-1", order.getOrderId(), order.getTotalAmount(), PaymentMethod.UPI);
        boolean paid = paymentProcessor.process(payment);

        if (!paid) {
            System.out.println("Payment failed — order stays PLACED, not confirmed.");
            return;
        }

        processor.enqueueRequest(new StateChangeRequest(order.getOrderId(), OrderStatus.CONFIRMED));
        processor.processQueue();

        DeliveryPartnerRegistry registry = new DeliveryPartnerRegistry();
        DeliveryPartner partner = new DeliveryPartner("D1", "Ravi", "ravi@example.com", "8888888888");
        partner.updateLocation(12.97, 77.59);
        registry.register(partner);

        DeliveryAssignmentProcessor assignmentProcessor = new DeliveryAssignmentProcessor(registry);
        assignmentProcessor.enqueueRequest(new DeliveryAssignmentRequest(order.getOrderId(), 12.97, 77.59, 12.98, 77.60));
        assignmentProcessor.processQueue();

        processor.enqueueRequest(new StateChangeRequest(order.getOrderId(), OrderStatus.SHIPPED));
        processor.processQueue();
    }

    // ---- Original interactive CLI, unchanged ----
    static void runInteractiveCli(OrderStore store, StateChangeValidator validator, StateProcessor processor) {
        int totalOrders = 1000;
        Random random = new Random();
        while (store.getAll().size() < totalOrders) {
            String orderId = String.valueOf(1000 + random.nextInt(9000));
            if (!store.exists(orderId)) {
                store.addOrder(new Order(orderId, OrderStatus.PLACED));
            }
        }
        System.out.println(totalOrders + " sample orders created, all starting at PLACED.");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter Order ID (or 'exit' to quit): ");
            String orderId = scanner.nextLine().trim();

            if (orderId.equalsIgnoreCase("exit")) break;
            if (orderId.isEmpty()) continue;

            if (store.exists(orderId)) {
                handleExistingOrder(orderId, store, validator, processor, scanner);
            } else {
                handleUnknownOrder(orderId, store, scanner);
            }
        }

        scanner.close();
        System.out.println("\nFinal order histories:");
        for (Order o : store.getAll().values()) {
            if (o.getHistory().size() > 1) {
                System.out.println(o.getOrderId() + ": " + o.getHistory());
            }
        }
        System.out.println("Goodbye.");
    }

    static void handleExistingOrder(String orderId, OrderStore store, StateChangeValidator validator,
                                     StateProcessor processor, Scanner scanner) {
        Order order = store.getOrder(orderId).get();
        OrderStatus current = order.getCurrentStatus();
        System.out.println(orderId + " exists. Current status: " + current);

        Set<OrderStatus> allowedNext = validator.allowedNext(current);
        if (allowedNext.isEmpty()) {
            System.out.println(current + " is a dead end. No further transitions allowed.");
            return;
        }

        System.out.print("Do you want to change its status? (yes/no): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) return;

        System.out.println("Allowed next statuses: " + allowedNext);
        System.out.print("Enter new status: ");
        String requestedInput = scanner.nextLine().trim().toUpperCase();

        OrderStatus requested = parseStatus(requestedInput);
        if (requested == null) {
            System.out.println("ERROR: '" + requestedInput + "' is not a recognized status.");
            return;
        }

        processor.enqueueRequest(new StateChangeRequest(orderId, requested));
        processor.processQueue();
    }

    static void handleUnknownOrder(String orderId, OrderStore store, Scanner scanner) {
        System.out.println("Order " + orderId + " does not exist.");
        System.out.print("Create it as a new order? (yes/no): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            store.addOrder(new Order(orderId, OrderStatus.PLACED));
            System.out.println("[CREATED] " + orderId + " -> PLACED");
        } else {
            System.out.println("Skipped. " + orderId + " was not created.");
        }
    }

    static OrderStatus parseStatus(String text) {
        try {
            return OrderStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
