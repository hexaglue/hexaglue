package com.ecommerce.application;

import com.ecommerce.domain.order.Money;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.ProductId;
import com.ecommerce.ports.in.ManagingProducts;
import com.ecommerce.ports.out.ProductRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Application service implementing product management use cases.
 */
public class ProductService implements ManagingProducts {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product createProduct(String name, String description, Money price, int initialStock) {
        Product product = new Product(ProductId.generate(), name, description, price, initialStock);
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(ProductId productId, String name, String description, Money price) {
        Product product = findOrThrow(productId);
        product.updateDetails(name, description, price);
        return productRepository.save(product);
    }

    @Override
    public Product adjustStock(ProductId productId, int quantityDelta) {
        Product product = findOrThrow(productId);
        product.adjustStock(quantityDelta);
        return productRepository.save(product);
    }

    @Override
    public Product activateProduct(ProductId productId) {
        Product product = findOrThrow(productId);
        product.activate();
        return productRepository.save(product);
    }

    @Override
    public Product deactivateProduct(ProductId productId) {
        Product product = findOrThrow(productId);
        product.deactivate();
        return productRepository.save(product);
    }

    @Override
    public Optional<Product> findProduct(ProductId productId) {
        return productRepository.findById(productId);
    }

    @Override
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContaining(name);
    }

    @Override
    public List<Product> listActiveProducts() {
        return productRepository.findAllActive();
    }

    @Override
    public List<Product> listAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public void deleteProduct(ProductId productId) {
        Product product = findOrThrow(productId);
        productRepository.delete(product);
    }

    private Product findOrThrow(ProductId productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
    }
}
