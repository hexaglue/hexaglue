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

package io.hexaglue.plugin.jpa.model;

import com.palantir.javapoet.TypeName;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;

/**
 * Intermediate representation of an identity field for JPA entity generation.
 *
 * <p>This record captures the identity metadata needed to generate JPA {@code @Id}
 * and {@code @GeneratedValue} annotations, as well as handle wrapped identity types
 * (e.g., {@code record OrderId(UUID value) {}}).
 *
 * <p>Design decision: Identity fields require special handling because they:
 * <ul>
 *   <li>May be wrapped in custom types for type safety</li>
 *   <li>Require ID generation strategy annotations</li>
 *   <li>Need AttributeConverter for wrapped types in JPA</li>
 * </ul>
 *
 * @param fieldName the name of the identity field in the generated entity
 * @param javaType the JavaPoet type of the declared identity (possibly wrapped)
 * @param unwrappedType the JavaPoet type of the underlying primitive/wrapper
 * @param strategy the identity generation strategy
 * @param wrapperKind the kind of wrapper (RECORD, CLASS, or NONE)
 * @since 2.0.0
 */
public record IdFieldSpec(
        String fieldName,
        TypeName javaType,
        TypeName unwrappedType,
        IdentityStrategy strategy,
        IdentityWrapperKind wrapperKind) {

    /**
     * Creates an IdFieldSpec from a SPI Identity.
     *
     * <p>This factory method converts the SPI identity model to the JavaPoet-based
     * generation model. It handles both wrapped and unwrapped identity types.
     *
     * <p>Examples:
     * <ul>
     *   <li>Wrapped: {@code OrderId orderId} → javaType=OrderId, unwrappedType=UUID</li>
     *   <li>Unwrapped: {@code UUID id} → javaType=UUID, unwrappedType=UUID</li>
     * </ul>
     *
     * @param identity the identity from the SPI
     * @return an IdFieldSpec ready for code generation
     */
    public static IdFieldSpec from(Identity identity) {
        TypeName javaType = JpaModelUtils.resolveTypeName(identity.type().qualifiedName());
        TypeName unwrappedType =
                JpaModelUtils.resolveTypeName(identity.unwrappedType().qualifiedName());

        return new IdFieldSpec(
                identity.fieldName(), javaType, unwrappedType, identity.strategy(), identity.wrapperKind());
    }

    /**
     * Returns true if the identity is wrapped in a custom type.
     *
     * <p>Wrapped identities require AttributeConverter generation for JPA mapping.
     *
     * @return true if wrapperKind is RECORD or CLASS
     */
    public boolean isWrapped() {
        return wrapperKind != IdentityWrapperKind.NONE;
    }

    /**
     * Returns true if this identity requires {@code @GeneratedValue} annotation.
     *
     * <p>Generated value strategies include AUTO, IDENTITY, SEQUENCE, TABLE, and UUID.
     * Natural and assigned identities do not require this annotation.
     *
     * @return true if the strategy requires generated value annotation
     */
    public boolean requiresGeneratedValue() {
        return strategy.requiresGeneratedValue();
    }

    /**
     * Returns the JPA GenerationType string if applicable.
     *
     * <p>Used for generating the {@code @GeneratedValue(strategy = GenerationType.XXX)} annotation.
     *
     * @return the GenerationType name, or null if not applicable
     */
    public String jpaGenerationType() {
        return strategy.toJpaGenerationType();
    }

    /**
     * Returns true if the identity uses a database-generated strategy.
     *
     * <p>Database-generated strategies include IDENTITY, SEQUENCE, TABLE, AUTO.
     *
     * @return true if the database generates the ID
     */
    public boolean isDatabaseGenerated() {
        return strategy.isGenerated() && strategy != IdentityStrategy.UUID;
    }

    /**
     * Returns true if the identity uses UUID generation.
     *
     * <p>UUID generation is typically handled by application code or JPA 3.1+ UUID generator.
     *
     * @return true if the strategy is UUID
     */
    public boolean isUuidGenerated() {
        return strategy == IdentityStrategy.UUID;
    }
}
