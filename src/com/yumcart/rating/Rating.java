package com.yumcart.rating;

public class Rating {
    private final String ratingId;
    private final String orderId;
    private final String customerId;
    private final String targetId;
    private final RatingTargetType targetType;
    private final int stars; // 1-5
    private final String comment;

    public Rating(String ratingId, String orderId, String customerId,
                   String targetId, RatingTargetType targetType, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be between 1 and 5");
        }
        this.ratingId = ratingId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.stars = stars;
        this.comment = comment;
    }

    public String getRatingId() { return ratingId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getTargetId() { return targetId; }
    public RatingTargetType getTargetType() { return targetType; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
}
