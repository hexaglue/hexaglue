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

import com.palantir.javapoet.ClassName;
import java.util.Objects;

/**
 * Specification for a constructor dependency of an application service bean.
 *
 * <p>Represents a single parameter of the application service constructor,
 * used to generate the {@code @Bean} method parameters in the {@code @Configuration} class.
 *
 * @param type      the fully qualified type of the dependency (e.g., TaskRepository)
 * @param paramName the parameter name (e.g., "taskRepository")
 * @since 3.1.0
 */
public record BeanDependencySpec(ClassName type, String paramName) {

    /**
     * Creates a new BeanDependencySpec.
     *
     * @param type      the dependency type, must not be null
     * @param paramName the parameter name, must not be null or blank
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if paramName is blank
     */
    public BeanDependencySpec {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(paramName, "paramName must not be null");
        if (paramName.isBlank()) {
            throw new IllegalArgumentException("paramName must not be blank");
        }
    }
}
