package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Coupon;
import com.example.ecommerce.domain.model.CouponId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Coupon persistence.
 */
public interface CouponRepository {
    Coupon save(Coupon entity);

    Optional<Coupon> findById(CouponId id);

    List<Coupon> findAll();

    void deleteById(CouponId id);

    boolean existsById(CouponId id);
}
