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

package io.hexaglue.plugin.jpa.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.model.DerivedMethodSpec;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import io.hexaglue.plugin.jpa.util.JpaAnnotations;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/**
 * Generates Spring Data JPA repository interfaces using JavaPoet.
 *
 * <p>This generator creates repository interfaces that extend {@code JpaRepository<Entity, ID>}
 * for each aggregate root in the domain model.
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * package com.example.infrastructure.jpa;
 *
 * import jakarta.annotation.Generated;
 * import java.util.UUID;
 * import org.springframework.data.jpa.repository.JpaRepository;
 * import org.springframework.stereotype.Repository;
 *
 * @Generated(value = "io.hexaglue.plugin.jpa")
 * @Repository
 * public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
 * }
 * }</pre>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li><b>Empty interfaces:</b> Spring Data JPA provides CRUD methods automatically</li>
 *   <li><b>Public visibility:</b> Repositories need to be accessible across packages</li>
 *   <li><b>@Repository annotation:</b> Marks the bean for Spring component scanning</li>
 *   <li><b>@Generated annotation:</b> Documents the code generation source</li>
 *   <li><b>Stateless utility class:</b> All methods are static, no instance state</li>
 * </ul>
 *
 * <h3>Spring Data JPA Default Methods:</h3>
 * <p>By extending {@code JpaRepository}, the generated interface automatically inherits:
 * <ul>
 *   <li>{@code save(entity)}</li>
 *   <li>{@code findById(id)}</li>
 *   <li>{@code findAll()}</li>
 *   <li>{@code delete(entity)}</li>
 *   <li>{@code deleteById(id)}</li>
 *   <li>{@code count()}</li>
 *   <li>{@code existsById(id)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class JpaRepositoryCodegen {

    /**
     * The generator identifier used in {@code @Generated} annotations.
     */
    private static final String GENERATOR_ID = "io.hexaglue.plugin.jpa";

    /**
     * The plugin version, read from MANIFEST.MF at runtime.
     */
    private static final String PLUGIN_VERSION = Optional.ofNullable(
                    JpaRepositoryCodegen.class.getPackage().getImplementationVersion())
            .orElse("dev");

    /**
     * The fully qualified class name for Spring Data JPA's JpaRepository interface.
     */
    private static final ClassName JPA_REPOSITORY =
            ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");

    private JpaRepositoryCodegen() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a Spring Data JPA repository interface from a RepositorySpec.
     *
     * <p>The generated interface:
     * <ul>
     *   <li>Is public and marked as an interface</li>
     *   <li>Extends {@code JpaRepository<EntityType, IdType>}</li>
     *   <li>Has {@code @Generated} and {@code @Repository} annotations</li>
     *   <li>Contains derived query methods (findByProperty, existsByProperty, etc.)</li>
     * </ul>
     *
     * @param spec the repository specification containing package, name, entity type, and ID type
     * @return the generated TypeSpec for the repository interface
     * @throws IllegalArgumentException if spec is null
     */
    public static TypeSpec generate(RepositorySpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("RepositorySpec cannot be null");
        }

        // Build parameterized superinterface: JpaRepository<EntityType, IdType>
        ParameterizedTypeName superInterface =
                ParameterizedTypeName.get(JPA_REPOSITORY, spec.entityType(), spec.idType());

        // Build the repository interface
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(spec.interfaceName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JpaAnnotations.generated(GENERATOR_ID, PLUGIN_VERSION))
                .addAnnotation(JpaAnnotations.repository())
                .addSuperinterface(superInterface)
                .addJavadoc("Spring Data JPA repository for {@link $L}.\n\n", spec.domainSimpleName())
                .addJavadoc("<p>This interface provides CRUD operations for $L entities.\n", spec.entityType())
                .addJavadoc("Spring Data JPA automatically implements standard methods:\n")
                .addJavadoc("<ul>\n")
                .addJavadoc("  <li>{@code save(entity)}</li>\n")
                .addJavadoc("  <li>{@code findById(id)}</li>\n")
                .addJavadoc("  <li>{@code findAll()}</li>\n")
                .addJavadoc("  <li>{@code delete(entity)}</li>\n")
                .addJavadoc("  <li>{@code count()}</li>\n")
                .addJavadoc("</ul>\n");

        // Add derived query methods
        for (DerivedMethodSpec derivedMethod : spec.derivedMethods()) {
            builder.addMethod(generateDerivedMethod(derivedMethod));
        }

        return builder.build();
    }

    /**
     * Generates a derived query method declaration for the repository interface.
     *
     * <p>Spring Data JPA will automatically derive the query from the method name.
     * For example:
     * <ul>
     *   <li>{@code Optional<Entity> findByEmail(String email)} → find by email property</li>
     *   <li>{@code boolean existsByEmail(String email)} → check existence by email</li>
     *   <li>{@code List<Entity> findAllByStatus(String status)} → find all by status</li>
     * </ul>
     *
     * @param method the derived method specification
     * @return the generated MethodSpec for the derived query method
     */
    private static MethodSpec generateDerivedMethod(DerivedMethodSpec method) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.methodName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(method.returnType());

        // Add parameters
        for (DerivedMethodSpec.ParameterSpec param : method.parameters()) {
            builder.addParameter(
                    ParameterSpec.builder(param.type(), param.name()).build());
        }

        return builder.build();
    }

    /**
     * Generates a complete JavaFile for the repository interface.
     *
     * <p>This method wraps the generated TypeSpec in a JavaFile with:
     * <ul>
     *   <li>The package declaration from the spec</li>
     *   <li>Automatic import management (JavaPoet handles this)</li>
     *   <li>Proper formatting and indentation</li>
     * </ul>
     *
     * <p>The resulting JavaFile can be written directly to disk using:
     * <pre>{@code
     * JavaFile javaFile = JpaRepositoryCodegen.generateFile(spec);
     * javaFile.writeTo(outputDirectory);
     * }</pre>
     *
     * @param spec the repository specification
     * @return the generated JavaFile ready to be written
     * @throws IllegalArgumentException if spec is null
     */
    public static JavaFile generateFile(RepositorySpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("RepositorySpec cannot be null");
        }

        TypeSpec repositoryInterface = generate(spec);

        return JavaFile.builder(spec.packageName(), repositoryInterface)
                .indent("    ") // 4 spaces for indentation
                .build();
    }
}
