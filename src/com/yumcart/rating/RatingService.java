package com.yumcart.rating;

import com.yumcart.order.Order;
import com.yumcart.order.OrderStatus;

import java.util.UUID;

public class RatingService {
    private final RatingStore store;

    public RatingService(RatingStore store) {
        this.store = store;
    }

    public Rating rateRestaurant(Order order, int stars, String comment) {
        requireDelivered(order);
        Rating rating = new Rating(UUID.randomUUID().toString(), order.getOrderId(),
                order.getCustomerId(), order.getRestaurantId(), RatingTargetType.RESTAURANT, stars, comment);
        store.addRating(rating);
        return rating;
    }

    public Rating rateDeliveryPartner(Order order, String partnerId, int stars, String comment) {
        requireDelivered(order);
        Rating rating = new Rating(UUID.randomUUID().toString(), order.getOrderId(),
                order.getCustomerId(), partnerId, RatingTargetType.DELIVERY_PARTNER, stars, comment);
        store.addRating(rating);
        return rating;
    }

    private void requireDelivered(Order order) {
        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new RatingException("Cannot rate order " + order.getOrderId()
                    + " — it is not yet DELIVERED (current: " + order.getCurrentStatus() + ")");
        }
    }
}
