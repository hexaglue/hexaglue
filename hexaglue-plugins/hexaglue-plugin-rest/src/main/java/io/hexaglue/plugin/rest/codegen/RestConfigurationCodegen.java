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

package io.hexaglue.plugin.rest.codegen;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.rest.model.ApplicationServiceBeanSpec;
import io.hexaglue.plugin.rest.model.BeanDependencySpec;
import io.hexaglue.plugin.rest.model.RestConfigurationSpec;
import io.hexaglue.plugin.rest.util.RestAnnotations;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/**
 * Generates a Spring {@code @Configuration} class from a {@link RestConfigurationSpec}.
 *
 * <p>Each {@link ApplicationServiceBeanSpec} produces a {@code @Bean} method that
 * instantiates the application service with its constructor dependencies and
 * exposes it as a Spring bean via the driving port interface type.
 *
 * <p>This is a Stage 2 (codegen) class: it performs pure mechanical transformation
 * from spec to JavaPoet TypeSpec, with no business logic.
 *
 * @since 3.1.0
 */
public final class RestConfigurationCodegen {

    private RestConfigurationCodegen() {
        /* utility class */
    }

    /**
     * Generates a TypeSpec for the {@code @Configuration} class.
     *
     * @param spec the configuration specification
     * @return the JavaPoet TypeSpec
     */
    public static TypeSpec generate(RestConfigurationSpec spec) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.configuration())
                .addAnnotation(RestAnnotations.generated());

        for (ApplicationServiceBeanSpec beanSpec : spec.beans()) {
            builder.addMethod(generateBeanMethod(beanSpec));
        }

        return builder.build();
    }

    private static MethodSpec generateBeanMethod(ApplicationServiceBeanSpec beanSpec) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(beanSpec.beanMethodName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestAnnotations.bean())
                .returns(beanSpec.portType());

        for (BeanDependencySpec dep : beanSpec.dependencies()) {
            method.addParameter(dep.type(), dep.paramName());
        }

        String constructorArgs = beanSpec.dependencies().stream()
                .map(BeanDependencySpec::paramName)
                .collect(Collectors.joining(", "));

        method.addStatement("return new $T($L)", beanSpec.implementationType(), constructorArgs);

        return method.build();
    }
}
