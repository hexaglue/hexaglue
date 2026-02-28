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

package io.hexaglue.plugin.rest.builder;

import com.palantir.javapoet.ClassName;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ResponseDtoSpec;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;

/**
 * Builds a {@link ResponseDtoSpec} from a domain type (AggregateRoot or ValueObject).
 *
 * <p>Projects domain fields to DTO fields with accessor chains for the {@code from()} factory.
 *
 * @since 3.1.0
 */
public final class ResponseDtoSpecBuilder {

    private ResponseDtoSpecBuilder() {
        /* utility class */
    }

    /**
     * Builds a response DTO spec from a domain type.
     *
     * @param returnTypeRef the use case's return type reference
     * @param domainIndex   domain type index
     * @param config        plugin configuration
     * @param dtoPackage    target package for DTOs
     * @return the response DTO spec, or empty if the return type is not a domain type
     */
    public static Optional<ResponseDtoSpec> build(
            TypeRef returnTypeRef, DomainIndex domainIndex, RestConfig config, String dtoPackage) {

        TypeId typeId = TypeId.of(returnTypeRef.qualifiedName());

        // Check if AggregateRoot
        Optional<AggregateRoot> aggregate = domainIndex.aggregateRoot(typeId);
        if (aggregate.isPresent()) {
            return Optional.of(buildFromDomainType(
                    aggregate.get().id(), aggregate.get().structure(), domainIndex, config, dtoPackage));
        }

        // Check if ValueObject
        Optional<ValueObject> vo =
                domainIndex.valueObjects().filter(v -> v.id().equals(typeId)).findFirst();
        if (vo.isPresent()) {
            return Optional.of(
                    buildFromDomainType(vo.get().id(), vo.get().structure(), domainIndex, config, dtoPackage));
        }

        // Not a domain type
        return Optional.empty();
    }

    private static ResponseDtoSpec buildFromDomainType(
            TypeId typeId, TypeStructure structure, DomainIndex domainIndex, RestConfig config, String dtoPackage) {
        String simpleName = typeId.simpleName();
        String className = simpleName + config.responseDtoSuffix();
        ClassName domainType = ClassName.get(typeId.packageName(), simpleName);

        List<DtoFieldSpec> fields = structure.fields().stream()
                .flatMap(f -> DtoFieldMapper.mapForResponse(f, domainIndex, config, structure).stream())
                .toList();

        return new ResponseDtoSpec(className, dtoPackage, fields, domainType, simpleName);
    }
}
