package com.example.ecommerce.application;

import com.example.ecommerce.port.driving.ProductReviewService;
import com.example.ecommerce.port.driving.CreateProductReviewCommand;
import com.example.ecommerce.port.driving.UpdateProductReviewCommand;
import com.example.ecommerce.port.driven.ProductReviewRepository;
import com.example.ecommerce.domain.model.ProductReview;
import com.example.ecommerce.domain.model.ProductReviewId;
import java.util.List;

/**
 * Use case implementation for ProductReview operations.
 */
public class ManageProductReviewUseCase implements ProductReviewService {
    private final ProductReviewRepository repository;

    public ManageProductReviewUseCase(ProductReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductReviewId create(CreateProductReviewCommand command) {
        ProductReview entity = new ProductReview(
            ProductReviewId.generate(),
            command.name()
        );
        ProductReview saved = repository.save(entity);
        return saved.getId();
    }

    @Override
    public ProductReview getProductReview(ProductReviewId id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ProductReview not found: " + id));
    }

    @Override
    public List<ProductReview> getAll() {
        return repository.findAll();
    }

    @Override
    public void update(ProductReviewId id, UpdateProductReviewCommand command) {
        ProductReview entity = getProductReview(id);
        repository.save(entity);
    }

    @Override
    public void delete(ProductReviewId id) {
        repository.deleteById(id);
    }
}
