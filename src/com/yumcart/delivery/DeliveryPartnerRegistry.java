package com.yumcart.delivery;

import com.yumcart.user.DeliveryPartner;
import com.yumcart.user.DeliveryPartnerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeliveryPartnerRegistry {
    private final List<DeliveryPartner> partners = new ArrayList<>();

    public void register(DeliveryPartner partner) {
        partners.add(partner);
    }

    public Optional<DeliveryPartner> findNearestAvailable(double lat, double lng) {
        return partners.stream()
                .filter(p -> p.getStatus() == DeliveryPartnerStatus.AVAILABLE)
                .min((a, b) -> Double.compare(distance(a, lat, lng), distance(b, lat, lng)));
    }

    private double distance(DeliveryPartner p, double lat, double lng) {
        double dx = p.getCurrentLat() - lat;
        double dy = p.getCurrentLng() - lng;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
