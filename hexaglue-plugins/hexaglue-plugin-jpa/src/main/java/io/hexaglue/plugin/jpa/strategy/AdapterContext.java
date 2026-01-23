/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.plugin.jpa.strategy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.ir.Identity;

/**
 * Context information for adapter method generation.
 *
 * <p>This immutable record encapsulates all the metadata needed by method
 * generation strategies to produce correct adapter implementations. It provides
 * type information and field names that strategies use when generating method bodies.
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Immutability: Record ensures thread-safety and clarity</li>
 *   <li>Context Object Pattern: Avoids passing many individual parameters</li>
 *   <li>Type Safety: Uses JavaPoet TypeName for compile-time correctness</li>
 *   <li>Naming: Field names match the adapter's actual field declarations</li>
 *   <li>IdInfo: Provides identity information from SPI to avoid inference in strategies</li>
 * </ul>
 *
 * <h3>Usage in Generated Code:</h3>
 * <p>For an Order aggregate with wrapped ID (OrderId), this context would contain:
 * <pre>{@code
 * AdapterContext(
 *     domainClass = Order.class,
 *     entityClass = OrderEntity.class,
 *     repositoryFieldName = "repository",
 *     mapperFieldName = "mapper",
 *     idInfo = IdInfo(wrappedType=OrderId, unwrappedType=UUID, isWrapped=true),
 *     isDomainRecord = true
 * )
 * }</pre>
 *
 * <p>Which enables strategies to generate correct ID handling:
 * <pre>{@code
 * // For wrapped ID:
 * return repository.findById(mapper.map(orderId)).map(mapper::toDomain);
 * // For unwrapped ID:
 * return repository.findById(id).map(mapper::toDomain);
 * }</pre>
 *
 * @param domainClass the JavaPoet type of the domain aggregate (e.g., Order)
 * @param entityClass the JavaPoet type of the JPA entity (e.g., OrderEntity)
 * @param repositoryFieldName the name of the repository field in the adapter (typically "repository")
 * @param mapperFieldName the name of the mapper field in the adapter (typically "mapper")
 * @param idInfo identity information for proper ID handling (may be null if no identity)
 * @param isDomainRecord true if the domain class is a Java record (uses .id()), false for classes (uses .getId())
 * @since 3.0.0
 */
public record AdapterContext(
        TypeName domainClass,
        TypeName entityClass,
        String repositoryFieldName,
        String mapperFieldName,
        IdInfo idInfo,
        boolean isDomainRecord) {

    /**
     * Identity information for adapter method generation.
     *
     * <p>This nested record provides all the information needed to correctly handle
     * ID parameters in generated adapter methods. It distinguishes between wrapped
     * and unwrapped identities to generate the appropriate code.
     *
     * @param wrappedType the wrapped type (e.g., OrderId), null if not wrapped
     * @param unwrappedType the underlying primitive/wrapper type (e.g., UUID)
     * @param isWrapped true if the identity uses a wrapper type
     */
    public record IdInfo(TypeName wrappedType, TypeName unwrappedType, boolean isWrapped) {

        /**
         * Creates IdInfo from an SPI Identity.
         *
         * @param identity the identity from SPI
         * @return IdInfo for adapter generation
         */
        public static IdInfo from(Identity identity) {
            if (identity == null) {
                return null;
            }
            TypeName wrapped =
                    identity.isWrapped() ? ClassName.bestGuess(identity.type().qualifiedName()) : null;
            TypeName unwrapped = ClassName.bestGuess(identity.unwrappedType().qualifiedName());
            return new IdInfo(wrapped, unwrapped, identity.isWrapped());
        }

        /**
         * Creates IdInfo for an unwrapped (primitive/simple) identifier.
         *
         * @param unwrappedType the primitive type (e.g., UUID)
         * @return IdInfo with isWrapped=false
         */
        public static IdInfo unwrapped(TypeName unwrappedType) {
            return new IdInfo(null, unwrappedType, false);
        }
    }

    /**
     * Creates an AdapterContext with all required metadata.
     *
     * @param domainClass the domain type
     * @param entityClass the entity type
     * @param repositoryFieldName the repository field name
     * @param mapperFieldName the mapper field name
     * @param idInfo the identity information (may be null)
     * @throws IllegalArgumentException if any required parameter is null or if field names are empty
     */
    public AdapterContext {
        if (domainClass == null) {
            throw new IllegalArgumentException("Domain class cannot be null");
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        if (repositoryFieldName == null || repositoryFieldName.isEmpty()) {
            throw new IllegalArgumentException("Repository field name cannot be null or empty");
        }
        if (mapperFieldName == null || mapperFieldName.isEmpty()) {
            throw new IllegalArgumentException("Mapper field name cannot be null or empty");
        }
        // idInfo can be null for types without identity (e.g., value objects)
    }

    /**
     * Returns true if this context has identity information.
     *
     * @return true if idInfo is not null
     */
    public boolean hasIdInfo() {
        return idInfo != null;
    }

    /**
     * Returns true if the aggregate uses a wrapped identity.
     *
     * @return true if idInfo is present and isWrapped is true
     */
    public boolean hasWrappedId() {
        return idInfo != null && idInfo.isWrapped();
    }

    /**
     * Returns the method name to access the identity field on domain objects.
     *
     * <p>For Java records, this returns "id" (accessor method is {@code id()}).
     * For regular classes, this returns "getId" (getter method is {@code getId()}).
     *
     * @return "id" for records, "getId" for classes
     * @since 3.0.0
     */
    public String idAccessorMethod() {
        return isDomainRecord ? "id" : "getId";
    }
}
