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

package io.hexaglue.plugin.audit.util;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builder utility for creating test ArchitecturalModel instances with v5 API.
 *
 * <p>This utility provides fluent methods for creating mock architectural models
 * for testing validators and metric calculators. It builds models with proper
 * v5 indices (DomainIndex, PortIndex) from the registered types.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a model with an aggregate and entities
 * ArchitecturalModel model = new TestModelBuilder()
 *     .addAggregateRoot("com.example.Order")
 *     .addEntity("com.example.OrderLine", "com.example.Order")
 *     .addValueObject("com.example.Money")
 *     .addDrivenPort("com.example.OrderRepository", DrivenPortType.REPOSITORY)
 *     .build();
 *
 * // Use in tests
 * validator.validate(model, codebase, query);
 * }</pre>
 *
 * @since 5.0.0
 */
public class TestModelBuilder {

    private final List<AggregateRoot> aggregateRoots = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<ValueObject> valueObjects = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private final List<DrivingPort> drivingPorts = new ArrayList<>();
    private final List<DrivenPort> drivenPorts = new ArrayList<>();

    private String projectName = "test-project";
    private String basePackage = "com.example";

    /**
     * Sets the project name.
     *
     * @param name the project name
     * @return this builder
     */
    public TestModelBuilder projectName(String name) {
        this.projectName = name;
        return this;
    }

    /**
     * Sets the base package.
     *
     * @param basePackage the base package
     * @return this builder
     */
    public TestModelBuilder basePackage(String basePackage) {
        this.basePackage = basePackage;
        return this;
    }

    /**
     * Adds an aggregate root to the model.
     *
     * @param qualifiedName the fully qualified name
     * @return this builder
     */
    public TestModelBuilder addAggregateRoot(String qualifiedName) {
        return addAggregateRoot(qualifiedName, List.of(), List.of());
    }

    /**
     * Adds an aggregate root with entities and value objects.
     *
     * @param qualifiedName the fully qualified name
     * @param entityRefs the entity qualified names in this aggregate
     * @param valueObjectRefs the value object qualified names in this aggregate
     * @return this builder
     */
    public TestModelBuilder addAggregateRoot(
            String qualifiedName, List<String> entityRefs, List<String> valueObjectRefs) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = defaultClassStructure();
        ClassificationTrace trace = defaultTrace(ElementKind.AGGREGATE_ROOT);

        Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                .modifiers(Set.of(Modifier.PRIVATE))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        AggregateRoot aggregate = AggregateRoot.builder(id, structure, trace, idField)
                .entities(entityRefs.stream().map(TypeRef::of).toList())
                .valueObjects(valueObjectRefs.stream().map(TypeRef::of).toList())
                .build();

        aggregateRoots.add(aggregate);
        return this;
    }

    /**
     * Adds an entity to the model.
     *
     * @param qualifiedName the fully qualified name
     * @return this builder
     */
    public TestModelBuilder addEntity(String qualifiedName) {
        return addEntity(qualifiedName, null);
    }

    /**
     * Adds an entity to the model with an owning aggregate.
     *
     * @param qualifiedName the fully qualified name
     * @param owningAggregateQName the owning aggregate's qualified name (may be null)
     * @return this builder
     */
    public TestModelBuilder addEntity(String qualifiedName, String owningAggregateQName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = defaultClassStructure();
        ClassificationTrace trace = defaultTrace(ElementKind.ENTITY);

        Field idField = Field.builder("id", TypeRef.of("java.lang.Long"))
                .modifiers(Set.of(Modifier.PRIVATE))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Optional<TypeRef> owningAggregate =
                owningAggregateQName != null ? Optional.of(TypeRef.of(owningAggregateQName)) : Optional.empty();

        Entity entity = Entity.of(id, structure, trace, Optional.of(idField), owningAggregate);

        entities.add(entity);
        return this;
    }

    /**
     * Adds a value object to the model.
     *
     * @param qualifiedName the fully qualified name
     * @return this builder
     */
    public TestModelBuilder addValueObject(String qualifiedName) {
        return addValueObject(qualifiedName, false);
    }

    /**
     * Adds a value object to the model.
     *
     * @param qualifiedName the fully qualified name
     * @param hasSetter whether the value object has setters (violates immutability)
     * @return this builder
     */
    public TestModelBuilder addValueObject(String qualifiedName, boolean hasSetter) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = hasSetter ? structureWithSetter() : defaultClassStructure();
        ClassificationTrace trace = defaultTrace(ElementKind.VALUE_OBJECT);

        ValueObject vo = ValueObject.of(id, structure, trace);
        valueObjects.add(vo);
        return this;
    }

    /**
     * Adds a domain event to the model.
     *
     * @param qualifiedName the fully qualified name
     * @return this builder
     */
    public TestModelBuilder addDomainEvent(String qualifiedName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = defaultClassStructure();
        ClassificationTrace trace = defaultTrace(ElementKind.DOMAIN_EVENT);

        DomainEvent event = DomainEvent.of(id, structure, trace);
        domainEvents.add(event);
        return this;
    }

    /**
     * Adds a driving port (use case interface) to the model.
     *
     * @param qualifiedName the fully qualified name
     * @return this builder
     */
    public TestModelBuilder addDrivingPort(String qualifiedName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = defaultInterfaceStructure();
        ClassificationTrace trace = defaultTrace(ElementKind.DRIVING_PORT);

        DrivingPort port = DrivingPort.of(id, structure, trace);
        drivingPorts.add(port);
        return this;
    }

    /**
     * Adds a driven port to the model.
     *
     * @param qualifiedName the fully qualified name
     * @param portType the type of driven port
     * @return this builder
     */
    public TestModelBuilder addDrivenPort(String qualifiedName, DrivenPortType portType) {
        return addDrivenPort(qualifiedName, portType, null);
    }

    /**
     * Adds a driven port (repository/gateway) to the model.
     *
     * @param qualifiedName the fully qualified name
     * @param portType the type of driven port
     * @param managedAggregateQName the managed aggregate (for repositories)
     * @return this builder
     */
    public TestModelBuilder addDrivenPort(String qualifiedName, DrivenPortType portType, String managedAggregateQName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = defaultInterfaceStructure();
        ClassificationTrace trace = defaultTrace(ElementKind.DRIVEN_PORT);

        DrivenPort port;
        if (portType == DrivenPortType.REPOSITORY && managedAggregateQName != null) {
            port = DrivenPort.repository(id, structure, trace, TypeRef.of(managedAggregateQName));
        } else {
            port = DrivenPort.of(id, structure, trace, portType);
        }
        drivenPorts.add(port);
        return this;
    }

    /**
     * Builds the ArchitecturalModel with v5 indices.
     *
     * @return a new ArchitecturalModel
     */
    public ArchitecturalModel build() {
        // Build type registry
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        aggregateRoots.forEach(registryBuilder::add);
        entities.forEach(registryBuilder::add);
        valueObjects.forEach(registryBuilder::add);
        domainEvents.forEach(registryBuilder::add);
        drivingPorts.forEach(registryBuilder::add);
        drivenPorts.forEach(registryBuilder::add);
        TypeRegistry typeRegistry = registryBuilder.build();

        // Build indices
        DomainIndex domainIndex = DomainIndex.from(typeRegistry);
        PortIndex portIndex = PortIndex.from(typeRegistry);

        // Build model using the builder API
        ProjectContext project = ProjectContext.of(projectName, basePackage, java.nio.file.Path.of("."));

        return ArchitecturalModel.builder(project)
                .typeRegistry(typeRegistry)
                .domainIndex(domainIndex)
                .portIndex(portIndex)
                .build();
    }

    // === Static Factory Methods for common scenarios ===

    /**
     * Creates an empty model.
     *
     * @return an empty ArchitecturalModel
     */
    public static ArchitecturalModel emptyModel() {
        return new TestModelBuilder().build();
    }

    /**
     * Creates a model with a single aggregate root.
     *
     * @param qualifiedName the aggregate root's qualified name
     * @return a new ArchitecturalModel
     */
    public static ArchitecturalModel withAggregate(String qualifiedName) {
        return new TestModelBuilder().addAggregateRoot(qualifiedName).build();
    }

    /**
     * Creates a model with aggregates.
     *
     * @param qualifiedNames the aggregate root qualified names
     * @return a new ArchitecturalModel
     */
    public static ArchitecturalModel withAggregates(String... qualifiedNames) {
        TestModelBuilder builder = new TestModelBuilder();
        for (String name : qualifiedNames) {
            builder.addAggregateRoot(name);
        }
        return builder.build();
    }

    /**
     * Creates a model with driving ports.
     *
     * @param qualifiedNames the driving port qualified names
     * @return a new ArchitecturalModel
     */
    public static ArchitecturalModel withDrivingPorts(String... qualifiedNames) {
        TestModelBuilder builder = new TestModelBuilder();
        for (String name : qualifiedNames) {
            builder.addDrivingPort(name);
        }
        return builder.build();
    }

    /**
     * Creates a model with driven ports (repositories).
     *
     * @param qualifiedNames the driven port qualified names
     * @return a new ArchitecturalModel
     */
    public static ArchitecturalModel withRepositories(String... qualifiedNames) {
        TestModelBuilder builder = new TestModelBuilder();
        for (String name : qualifiedNames) {
            builder.addDrivenPort(name, DrivenPortType.REPOSITORY);
        }
        return builder.build();
    }

    /**
     * Creates a model with domain events.
     *
     * @param qualifiedNames the domain event qualified names
     * @return a new ArchitecturalModel
     */
    public static ArchitecturalModel withDomainEvents(String... qualifiedNames) {
        TestModelBuilder builder = new TestModelBuilder();
        for (String name : qualifiedNames) {
            builder.addDomainEvent(name);
        }
        return builder.build();
    }

    // === Helper methods ===

    private TypeStructure defaultClassStructure() {
        return TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
    }

    private TypeStructure defaultInterfaceStructure() {
        return TypeStructure.builder(TypeNature.INTERFACE)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
    }

    private TypeStructure structureWithSetter() {
        return TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .methods(List.of(io.hexaglue.arch.model.Method.of("setValue", TypeRef.of("void"))))
                .build();
    }

    private ClassificationTrace defaultTrace(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test-criterion", "Created by TestModelBuilder");
    }
}
