package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.CouponService;
import com.example.ecommerce.port.driving.CreateCouponCommand;
import com.example.ecommerce.port.driving.UpdateCouponCommand;
import com.example.ecommerce.port.driven.CouponRepository;
import com.example.ecommerce.domain.model.Coupon;
import com.example.ecommerce.domain.model.CouponId;
import java.util.List;

/**
 * Use case implementation for Coupon operations.
 */
public class ManageCouponUseCase implements CouponService {
    private final CouponRepository repository;

    public ManageCouponUseCase(CouponRepository repository) {
        this.repository = repository;
    }

    @Override
    public CouponId create(CreateCouponCommand command) {
        Coupon entity = new Coupon(
            CouponId.generate(),
            command.name()
        );
        Coupon saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public Coupon getCoupon(CouponId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
    }

    @Override
    public List<Coupon> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(CouponId id, UpdateCouponCommand command) {
        Coupon entity = getCoupon(id);
        repository.save(entity);
    }

    @Override
    public void delete(CouponId id) {
        repository.deleteById(id);
    }
}
