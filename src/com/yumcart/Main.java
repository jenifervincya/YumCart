package com.yumcart;

import com.yumcart.order.*;

import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        StateChangeValidator validator = new StateChangeValidator();
        OrderStore store = new OrderStore();
        StateProcessor processor = new StateProcessor(validator, store);

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
