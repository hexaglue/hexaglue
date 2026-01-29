package com.example.ecommerce.domain.shared;

/**
 * Abstract base class for all domain entities in the e-commerce system.
 *
 * <p>Provides identity-based equality semantics: two entities are considered
 * equal if and only if they share the same type and the same non-null identifier.
 * Subclasses must implement {@link #getId()} to return their unique identity.
 */
public abstract class Entity<ID> {

    public abstract ID getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return getId() != null && getId().equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
