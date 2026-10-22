package com.yumcart.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yumcart.cart.Cart;
import com.yumcart.cart.CartException;
import com.yumcart.delivery.DeliveryAssignmentProcessor;
import com.yumcart.delivery.DeliveryAssignmentRequest;
import com.yumcart.order.*;
import com.yumcart.payment.*;
import com.yumcart.pricing.*;
import com.yumcart.rating.RatingStore;
import com.yumcart.rating.RatingTargetType;
import com.yumcart.restaurant.MenuItem;
import com.yumcart.restaurant.Restaurant;
import com.yumcart.restaurant.RestaurantStore;
import com.yumcart.user.DeliveryPartner;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// REST layer over the YumCart modules, using only JDK built-ins.
//   GET  /api/restaurants               -> restaurants + menus + average ratings
//   POST /api/checkout                  -> cart -> pricing -> payment -> order -> delivery partner
//   GET  /api/orders/{id}               -> order status, history, items, total, partner
//   POST /api/orders/{id}               -> create a bare order if missing
//   POST /api/orders/{id}/status        -> push a status change through the queue
public class OrderApiServer {
    private final OrderStore store;
    private final StateProcessor processor;
    private final RestaurantStore restaurants;
    private final RatingStore ratings;
    private final PricingService pricing;
    private final PaymentProcessor payments;
    private final DeliveryAssignmentProcessor assignments;

    private final Map<String, String> partnerByOrder = new ConcurrentHashMap<>();
    private final AtomicInteger orderCounter = new AtomicInteger(1001);

    private static final Pattern STATUS_FIELD = Pattern.compile("\"status\"\\s*:\\s*\"([A-Z_]+)\"");
    private static final Pattern ITEM = Pattern.compile("\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"qty\"\\s*:\\s*(\\d+)\\s*\\}");

    private interface Route {
        void run(HttpExchange exchange) throws Exception;
    }

    public OrderApiServer(OrderStore store, StateProcessor processor, RestaurantStore restaurants,
                          RatingStore ratings, PricingService pricing, PaymentProcessor payments,
                          DeliveryAssignmentProcessor assignments) {
        this.store = store;
        this.processor = processor;
        this.restaurants = restaurants;
        this.ratings = ratings;
        this.pricing = pricing;
        this.payments = payments;
        this.assignments = assignments;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/orders", ex -> guard(ex, this::handleOrders));
        server.createContext("/api/restaurants", ex -> guard(ex, this::handleRestaurants));
        server.createContext("/api/checkout", ex -> guard(ex, this::handleCheckout));
        server.setExecutor(null);
        server.start();
        System.out.println("YumCart API running at http://localhost:" + port + "/api");
    }

    private void guard(HttpExchange exchange, Route route) throws IOException {
        addCorsHeaders(exchange);
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            route.run(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"success\":false,\"message\":" + q(String.valueOf(e.getMessage())) + "}");
        }
    }

    // ---------- /api/restaurants ----------
    private void handleRestaurants(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        boolean firstR = true;
        List<Restaurant> sorted = restaurants.getAll().values().stream()
                .sorted(Comparator.comparing(Restaurant::getId)).toList();
        for (Restaurant r : sorted) {
            if (!firstR) sb.append(",");
            firstR = false;
            sb.append("{\"id\":").append(q(r.getId()))
              .append(",\"name\":").append(q(r.getName()))
              .append(",\"address\":").append(q(r.getAddress()))
              .append(",\"open\":").append(r.isOpen())
              .append(",\"avgRating\":").append(String.format(Locale.ROOT, "%.1f",
                      ratings.getAverageRating(r.getId(), RatingTargetType.RESTAURANT)))
              .append(",\"ratingCount\":").append(ratings.getRatingCount(r.getId(), RatingTargetType.RESTAURANT))
              .append(",\"menu\":[");
            boolean firstM = true;
            for (MenuItem m : r.getMenu()) {
                if (!firstM) sb.append(",");
                firstM = false;
                sb.append("{\"id\":").append(q(m.getId()))
                  .append(",\"name\":").append(q(m.getName()))
                  .append(",\"price\":").append(String.format(Locale.ROOT, "%.2f", m.getPrice()))
                  .append(",\"category\":").append(q(m.getCategory()))
                  .append(",\"available\":").append(m.isAvailable()).append("}");
            }
            sb.append("]}");
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    // ---------- /api/checkout ----------
    private void handleCheckout(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String restaurantId = stringField(body, "restaurantId");
        String paymentName = stringField(body, "payment");
        String coupon = stringField(body, "coupon");

        Restaurant restaurant = restaurants.getRestaurant(restaurantId).orElse(null);
        if (restaurant == null) { fail(exchange, 400, "Unknown restaurant."); return; }
        if (!restaurant.isOpen()) { fail(exchange, 400, restaurant.getName() + " is closed right now."); return; }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentName);
        } catch (IllegalArgumentException e) {
            fail(exchange, 400, "Choose a valid payment method.");
            return;
        }

        Cart cart = new Cart("guest");
        Matcher m = ITEM.matcher(body);
        try {
            while (m.find()) {
                int qty = Integer.parseInt(m.group(2));
                MenuItem item = restaurant.getMenuItem(m.group(1)).orElse(null);
                if (item == null || qty < 1) { fail(exchange, 400, "Invalid item in cart."); return; }
                cart.addItem(restaurant.getId(), item, qty);
            }
        } catch (CartException e) {
            fail(exchange, 400, e.getMessage());
            return;
        }
        if (cart.getItems().isEmpty()) { fail(exchange, 400, "Your cart is empty."); return; }

        DiscountStrategy discount = null;
        if ("YUM10".equalsIgnoreCase(coupon)) discount = new PercentageDiscount(10);
        else if ("FLAT50".equalsIgnoreCase(coupon)) discount = new FlatDiscount(50);

        double total = Math.round(pricing.calculateTotal(cart.getSubtotal(), discount) * 100.0) / 100.0;

        String orderId = "YC" + orderCounter.getAndIncrement();
        Payment payment = new Payment("PAY-" + orderId, orderId, total, method);
        if (!payments.process(payment)) {
            fail(exchange, 200, "Payment failed. Please try again.");
            return;
        }

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(ci -> new OrderItem(ci.getMenuItem().getId(), ci.getMenuItem().getName(),
                        ci.getMenuItem().getPrice(), ci.getQuantity()))
                .toList();
        Order order = new Order(orderId, "guest", restaurant.getId(), orderItems);
        order.setTotalAmount(total);
        store.addOrder(order);

        processor.enqueueRequest(new StateChangeRequest(orderId, OrderStatus.CONFIRMED));
        processor.processQueue();

        assignments.enqueueRequest(new DeliveryAssignmentRequest(orderId, 12.97, 77.59, 12.98, 77.60));
        Optional<DeliveryPartner> partner = assignments.processNext();
        partner.ifPresent(p -> partnerByOrder.put(orderId, p.getName()));

        sendJson(exchange, 200, "{\"success\":true,\"orderId\":" + q(orderId)
                + ",\"total\":" + String.format(Locale.ROOT, "%.2f", total)
                + ",\"partner\":" + q(partner.map(DeliveryPartner::getName).orElse("")) + "}");
    }

    // ---------- /api/orders ----------
    private void handleOrders(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        String method = exchange.getRequestMethod();
        // ["", "api", "orders", "ID"] or ["", "api", "orders", "ID", "status"]
        if (parts.length == 4 && method.equalsIgnoreCase("GET")) {
            Order order = store.getOrder(parts[3]).orElse(null);
            if (order == null) {
                sendJson(exchange, 404, "{\"found\":false,\"orderId\":" + q(parts[3]) + "}");
            } else {
                sendJson(exchange, 200, orderToJson(order));
            }
        } else if (parts.length == 4 && method.equalsIgnoreCase("POST")) {
            Order order = store.getOrder(parts[3]).orElse(null);
            if (order == null) {
                order = new Order(parts[3], OrderStatus.PLACED);
                store.addOrder(order);
            }
            sendJson(exchange, 200, orderToJson(order));
        } else if (parts.length == 5 && parts[4].equals("status") && method.equalsIgnoreCase("POST")) {
            handleStatusChange(exchange, parts[3]);
        } else {
            sendJson(exchange, 404, "{\"error\":\"not found\"}");
        }
    }

    private void handleStatusChange(HttpExchange exchange, String orderId) throws IOException {
        Order order = store.getOrder(orderId).orElse(null);
        if (order == null) {
            sendJson(exchange, 404, "{\"found\":false,\"orderId\":" + q(orderId) + "}");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher m = STATUS_FIELD.matcher(body);
        if (!m.find()) {
            sendJson(exchange, 400, "{\"applied\":false,\"message\":\"missing or invalid status\"}");
            return;
        }
        OrderStatus requested;
        try {
            requested = OrderStatus.valueOf(m.group(1));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"applied\":false,\"message\":\"unknown status\"}");
            return;
        }
        OrderStatus before = order.getCurrentStatus();
        processor.enqueueRequest(new StateChangeRequest(orderId, requested));
        processor.processQueue();
        OrderStatus after = order.getCurrentStatus();
        boolean applied = after != before;
        sendJson(exchange, 200, "{\"applied\":" + applied + ",\"status\":\"" + after + "\",\"message\":"
                + q(applied ? "Moved to " + after : "Cannot move from " + before + " to " + requested) + "}");
    }

    private String orderToJson(Order order) {
        StringBuilder history = new StringBuilder("[");
        List<OrderStatus> h = order.getHistory();
        for (int i = 0; i < h.size(); i++) {
            history.append("\"").append(h.get(i)).append("\"");
            if (i < h.size() - 1) history.append(",");
        }
        history.append("]");

        StringBuilder items = new StringBuilder("[");
        List<OrderItem> list = order.getItems();
        for (int i = 0; i < list.size(); i++) {
            OrderItem it = list.get(i);
            items.append("{\"name\":").append(q(it.getName()))
                 .append(",\"qty\":").append(it.getQuantity())
                 .append(",\"price\":").append(String.format(Locale.ROOT, "%.2f", it.getPriceAtOrderTime())).append("}");
            if (i < list.size() - 1) items.append(",");
        }
        items.append("]");

        return "{\"found\":true,\"orderId\":" + q(order.getOrderId())
                + ",\"status\":\"" + order.getCurrentStatus() + "\""
                + ",\"history\":" + history
                + ",\"restaurantId\":" + q(order.getRestaurantId() == null ? "" : order.getRestaurantId())
                + ",\"total\":" + String.format(Locale.ROOT, "%.2f", order.getTotalAmount())
                + ",\"partner\":" + q(partnerByOrder.getOrDefault(order.getOrderId(), ""))
                + ",\"items\":" + items + "}";
    }

    // ---------- helpers ----------
    private String stringField(String body, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private void fail(HttpExchange exchange, int code, String message) throws IOException {
        sendJson(exchange, code, "{\"success\":false,\"message\":" + q(message) + "}");
    }

    private String q(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
