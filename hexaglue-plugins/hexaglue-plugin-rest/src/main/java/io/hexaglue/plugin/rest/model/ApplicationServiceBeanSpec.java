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
import java.util.List;
import java.util.Objects;

/**
 * Specification for an application service bean in the {@code @Configuration} class.
 *
 * <p>Each spec produces a {@code @Bean} method that instantiates an application service
 * and exposes it as a Spring bean via its driving port interface.
 *
 * @param portType           the driving port interface type (return type of the {@code @Bean} method)
 * @param implementationType the concrete application service type
 * @param beanMethodName     the {@code @Bean} method name (e.g., "taskUseCases")
 * @param dependencies       the constructor parameters of the application service
 * @since 3.1.0
 */
public record ApplicationServiceBeanSpec(
        ClassName portType,
        ClassName implementationType,
        String beanMethodName,
        List<BeanDependencySpec> dependencies) {

    /**
     * Creates a new ApplicationServiceBeanSpec with defensive copy.
     *
     * @param portType           the port type, must not be null
     * @param implementationType the implementation type, must not be null
     * @param beanMethodName     the bean method name, must not be null or blank
     * @param dependencies       the dependencies, must not be null
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if beanMethodName is blank
     */
    public ApplicationServiceBeanSpec {
        Objects.requireNonNull(portType, "portType must not be null");
        Objects.requireNonNull(implementationType, "implementationType must not be null");
        Objects.requireNonNull(beanMethodName, "beanMethodName must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        if (beanMethodName.isBlank()) {
            throw new IllegalArgumentException("beanMethodName must not be blank");
        }
        dependencies = List.copyOf(dependencies);
    }
}
