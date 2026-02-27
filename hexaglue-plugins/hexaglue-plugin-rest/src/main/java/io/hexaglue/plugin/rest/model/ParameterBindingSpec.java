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

package io.hexaglue.plugin.rest.model;

import com.palantir.javapoet.TypeName;
import java.util.List;

/**
 * Describes how a port method parameter is reconstructed from DTO fields.
 *
 * <p>Each binding maps one port parameter to one or more DTO source fields,
 * along with the reconstruction strategy (direct, constructor wrap, factory wrap).
 *
 * @param parameterName the port method parameter name (e.g., "customerId")
 * @param domainType    the JavaPoet TypeName of the domain type (e.g., CustomerId)
 * @param kind          the reconstruction strategy
 * @param sourceFields  the DTO field names used for reconstruction (immutable)
 * @since 3.1.0
 */
public record ParameterBindingSpec(
        String parameterName, TypeName domainType, BindingKind kind, List<String> sourceFields) {

    /**
     * Creates a new ParameterBindingSpec with defensive copies.
     */
    public ParameterBindingSpec {
        sourceFields = List.copyOf(sourceFields);
    }
}
