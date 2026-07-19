import java.util.*;

public class OTS {

    enum OrderStatus {
        PLACED, CONFIRMED, SHIPPED, OUT_FOR_DELIVERY, NOT_DELIVERED, DELIVERED, CANCELLED, RETURNED
    }

    public static void main(String[] args) {

        // ---- Graph (adjacency list): node = status, edges = legal next statuses ----
        Map<OrderStatus, Set<OrderStatus>> graph = new HashMap<>();
        graph.put(OrderStatus.PLACED,           EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        graph.put(OrderStatus.CONFIRMED,        EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        graph.put(OrderStatus.SHIPPED,          EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED));
        graph.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.NOT_DELIVERED));
        graph.put(OrderStatus.NOT_DELIVERED,    EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED));
        graph.put(OrderStatus.DELIVERED,        EnumSet.of(OrderStatus.RETURNED));
        graph.put(OrderStatus.CANCELLED,        EnumSet.noneOf(OrderStatus.class));
        graph.put(OrderStatus.RETURNED,         EnumSet.noneOf(OrderStatus.class));

        // ---- Generate sample orders with random 4-digit IDs, all unique ----
        int totalOrders = 1000;
        Map<String, OrderStatus> orderState = new HashMap<>();

        Random random = new Random();
        while (orderState.size() < totalOrders) {
            String orderId = String.valueOf(1000 + random.nextInt(9000)); // 1000..9999
            orderState.putIfAbsent(orderId, OrderStatus.PLACED);
        }

        System.out.println(totalOrders + " sample orders created, all starting at PLACED.");

        // ---- Interactive loop ----
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter Order ID (or 'exit' to quit): ");
            String orderId = scanner.nextLine().trim();

            if (orderId.equalsIgnoreCase("exit")) {
                break;
            }

            if (orderId.isEmpty()) {
                continue;
            }

            if (orderState.containsKey(orderId)) {
                handleExistingOrder(orderId, orderState, graph, scanner);
            } else {
                handleUnknownOrder(orderId, orderState, scanner);
            }
        }

        scanner.close();
        System.out.println("Goodbye.");
    }

    // Order exists: show status, optionally change it
    static void handleExistingOrder(String orderId,
                                     Map<String, OrderStatus> orderState,
                                     Map<OrderStatus, Set<OrderStatus>> graph,
                                     Scanner scanner) {

        OrderStatus current = orderState.get(orderId);
        System.out.println(orderId + " exists. Current status: " + current);

        Set<OrderStatus> allowedNext = graph.get(current);

        // Check dead-end FIRST, before asking anything
        if (allowedNext.isEmpty()) {
            System.out.println(current + " is a dead end. No further transitions allowed.");
            return;
        }

        System.out.print("Do you want to change its status? (yes/no): ");
        String choice = scanner.nextLine().trim();

        if (!choice.equalsIgnoreCase("yes")) {
            return;
        }

        System.out.println("Allowed next statuses: " + allowedNext);
        System.out.print("Enter new status: ");
        String requestedInput = scanner.nextLine().trim().toUpperCase();

        OrderStatus requested = parseStatus(requestedInput);

        if (requested == null) {
            System.out.println("ERROR: '" + requestedInput + "' is not a recognized status.");
            return;
        }

        if (allowedNext.contains(requested)) {
            orderState.put(orderId, requested);
            System.out.println("[OK] " + orderId + ": " + current + " -> " + requested);
        } else {
            System.out.println("[INVALID] Cannot move from " + current + " to " + requested + ". Skipped.");
        }
    }

    // Order doesn't exist: offer to create it
    static void handleUnknownOrder(String orderId,
                                    Map<String, OrderStatus> orderState,
                                    Scanner scanner) {

        System.out.println("Order " + orderId + " does not exist.");
        System.out.print("Create it as a new order? (yes/no): ");
        String choice = scanner.nextLine().trim();

        if (choice.equalsIgnoreCase("yes")) {
            orderState.put(orderId, OrderStatus.PLACED);
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