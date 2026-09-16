package com.yumcart.rating;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RatingStore {
    private final List<Rating> ratings = new ArrayList<>();

    public void addRating(Rating rating) {
        ratings.add(rating);
    }

    public List<Rating> getRatingsFor(String targetId, RatingTargetType type) {
        return ratings.stream()
                .filter(r -> r.getTargetId().equals(targetId) && r.getTargetType() == type)
                .collect(Collectors.toList());
    }

    public double getAverageRating(String targetId, RatingTargetType type) {
        List<Rating> forTarget = getRatingsFor(targetId, type);
        if (forTarget.isEmpty()) return 0.0;
        return forTarget.stream().mapToInt(Rating::getStars).average().orElse(0.0);
    }

    public int getRatingCount(String targetId, RatingTargetType type) {
        return getRatingsFor(targetId, type).size();
    }
}
