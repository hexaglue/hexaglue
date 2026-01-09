package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.CouponId;
import com.example.ecommerce.domain.model.Coupon;
import java.util.List;

/**
 * Driving port (primary) for coupon operations.
 */
public interface CouponService {
    CouponId create(CreateCouponCommand command);

    Coupon getCoupon(CouponId id);

    List<Coupon> getAll();

    void update(CouponId id, UpdateCouponCommand command);

    void delete(CouponId id);
}
