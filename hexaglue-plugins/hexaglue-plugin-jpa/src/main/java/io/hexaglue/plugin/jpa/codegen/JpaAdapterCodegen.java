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

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.model.AdapterMethodSpec;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.plugin.jpa.strategy.AdapterContext;
import io.hexaglue.plugin.jpa.strategy.MethodBodyStrategy;
import io.hexaglue.plugin.jpa.strategy.MethodStrategyFactory;
import io.hexaglue.plugin.jpa.util.JpaAnnotations;
import javax.lang.model.element.Modifier;

/**
 * JavaPoet-based code generator for JPA adapter implementations.
 *
 * <p>This class generates Spring component adapter classes that implement port
 * interfaces by delegating to JPA repositories and MapStruct mappers. It uses
 * the Strategy Pattern to generate method bodies based on detected patterns.
 *
 * <h3>Generated Code Structure:</h3>
 * <pre>{@code
 * package com.example.infrastructure.jpa;
 *
 * import org.springframework.stereotype.Component;
 * import javax.annotation.processing.Generated;
 *
 * @Generated("io.hexaglue.plugin.jpa.JpaPlugin")
 * @Component
 * public class OrderJpaAdapter implements OrderRepository {
 *
 *     private final OrderJpaRepository repository;
 *     private final OrderMapper mapper;
 *
 *     public OrderJpaAdapter(OrderJpaRepository repository, OrderMapper mapper) {
 *         this.repository = repository;
 *         this.mapper = mapper;
 *     }
 *
 *     @Override
 *     public Order save(Order domain) {
 *         var entity = mapper.toEntity(domain);
 *         var saved = repository.save(entity);
 *         return mapper.toDomain(saved);
 *     }
 *
 *     @Override
 *     public Optional<Order> findById(UUID id) {
 *         return repository.findById(id).map(mapper::toDomain);
 *     }
 *
 *     // ... other methods
 * }
 * }</pre>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li>Constructor Injection: Dependencies injected via constructor for testability</li>
 *   <li>Final Fields: Repository and mapper fields are final for immutability</li>
 *   <li>Strategy Pattern: Method generation delegated to specialized strategies</li>
 *   <li>Spring Integration: Uses @Component for automatic bean registration</li>
 *   <li>Metadata Annotations: @Generated marks classes as generated code</li>
 * </ul>
 *
 * @since 2.0.0
 * @see AdapterSpec
 * @see MethodStrategyFactory
 */
public final class JpaAdapterCodegen {

    /**
     * Generator identifier used in @Generated annotation.
     */
    private static final String GENERATOR_NAME = "io.hexaglue.plugin.jpa.JpaPlugin";

    /**
     * Strategy factory for method generation.
     */
    private static final MethodStrategyFactory STRATEGY_FACTORY = new MethodStrategyFactory();

    private JpaAdapterCodegen() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a complete JPA adapter class from the specification.
     *
     * <p>This method orchestrates the entire adapter generation process:
     * <ol>
     *   <li>Create class builder with annotations and interfaces</li>
     *   <li>Add dependency fields (repository, mapper)</li>
     *   <li>Generate constructor with dependency injection</li>
     *   <li>Generate all port interface methods using strategies</li>
     * </ol>
     *
     * <p>The generated class follows these conventions:
     * <ul>
     *   <li>Public visibility for Spring component scanning</li>
     *   <li>Implements all specified port interfaces</li>
     *   <li>Constructor-based dependency injection</li>
     *   <li>All methods annotated with @Override</li>
     * </ul>
     *
     * @param spec the complete adapter specification
     * @return the JavaPoet TypeSpec ready to be written to a file
     * @throws IllegalArgumentException if spec is null or invalid
     */
    public static TypeSpec generate(AdapterSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("AdapterSpec cannot be null");
        }
        if (spec.className() == null || spec.className().isEmpty()) {
            throw new IllegalArgumentException("Adapter class name cannot be null or empty");
        }
        if (spec.implementedPorts() == null || spec.implementedPorts().isEmpty()) {
            throw new IllegalArgumentException("Adapter must implement at least one port interface");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JpaAnnotations.generated(GENERATOR_NAME))
                .addAnnotation(JpaAnnotations.component());

        // Implement port interfaces
        for (var port : spec.implementedPorts()) {
            builder.addSuperinterface(port);
        }

        // Add dependency fields
        builder.addField(createRepositoryField(spec));
        builder.addField(createMapperField(spec));

        // Add constructor
        builder.addMethod(createConstructor(spec));

        // Generate methods using strategies
        AdapterContext context = createContext(spec);
        for (AdapterMethodSpec method : spec.methods()) {
            MethodBodyStrategy strategy = STRATEGY_FACTORY.strategyFor(method);
            builder.addMethod(strategy.generate(method, context));
        }

        return builder.build();
    }

    /**
     * Creates the repository field specification.
     *
     * <p>Generates: {@code private final OrderJpaRepository repository;}
     *
     * @param spec the adapter specification
     * @return the JavaPoet FieldSpec for the repository
     */
    private static FieldSpec createRepositoryField(AdapterSpec spec) {
        return FieldSpec.builder(spec.repositoryClass(), "repository", Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    /**
     * Creates the mapper field specification.
     *
     * <p>Generates: {@code private final OrderMapper mapper;}
     *
     * @param spec the adapter specification
     * @return the JavaPoet FieldSpec for the mapper
     */
    private static FieldSpec createMapperField(AdapterSpec spec) {
        return FieldSpec.builder(spec.mapperClass(), "mapper", Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    /**
     * Creates the constructor with dependency injection.
     *
     * <p>Generates:
     * <pre>{@code
     * public OrderJpaAdapter(OrderJpaRepository repository, OrderMapper mapper) {
     *     this.repository = repository;
     *     this.mapper = mapper;
     * }
     * }</pre>
     *
     * <h3>Design Decisions:</h3>
     * <ul>
     *   <li>Constructor injection: Preferred by Spring for testability and immutability</li>
     *   <li>No @Autowired annotation: Not needed with single constructor (Spring convention)</li>
     *   <li>Parameter order: Repository first, then mapper (alphabetical by role)</li>
     * </ul>
     *
     * @param spec the adapter specification
     * @return the JavaPoet MethodSpec for the constructor
     */
    private static MethodSpec createConstructor(AdapterSpec spec) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(spec.repositoryClass(), "repository")
                        .build())
                .addParameter(
                        ParameterSpec.builder(spec.mapperClass(), "mapper").build())
                .addStatement("this.repository = repository")
                .addStatement("this.mapper = mapper")
                .build();
    }

    /**
     * Generates a complete JavaFile for the adapter class.
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
     * JavaFile javaFile = JpaAdapterCodegen.generateFile(spec);
     * javaFile.writeTo(outputDirectory);
     * }</pre>
     *
     * @param spec the adapter specification
     * @return the generated JavaFile ready to be written
     * @throws IllegalArgumentException if spec is null
     */
    public static JavaFile generateFile(AdapterSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("AdapterSpec cannot be null");
        }

        TypeSpec adapterClass = generate(spec);

        return JavaFile.builder(spec.packageName(), adapterClass)
                .indent("    ") // 4 spaces for indentation
                .build();
    }

    /**
     * Creates the adapter context for method generation.
     *
     * <p>The context provides all metadata needed by strategies to generate
     * method bodies, including type information and field names.
     *
     * @param spec the adapter specification
     * @return the AdapterContext for strategy use
     */
    private static AdapterContext createContext(AdapterSpec spec) {
        return new AdapterContext(spec.domainClass(), spec.entityClass(), "repository", "mapper");
    }
}
