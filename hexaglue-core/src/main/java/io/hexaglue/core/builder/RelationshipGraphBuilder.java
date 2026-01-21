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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.arch.model.graph.RelationshipGraph;
import io.hexaglue.syntax.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a {@link RelationshipGraph} from classified architectural types.
 *
 * <p>This builder analyzes the type registry and constructs relationships
 * between architectural elements based on their structural dependencies
 * and domain relationships.</p>
 *
 * <h2>Relationship Types Built</h2>
 * <ul>
 *   <li><strong>CONTAINS</strong> - Aggregate → Entity/ValueObject (from aggregate boundary)</li>
 *   <li><strong>EMITS</strong> - Aggregate → DomainEvent (from aggregate.domainEvents())</li>
 *   <li><strong>PERSISTS</strong> - Repository → Aggregate (from drivenPort.managedAggregate())</li>
 *   <li><strong>DEPENDS_ON</strong> - Type → Type (via field references)</li>
 *   <li><strong>IMPLEMENTS</strong> - Type → Interface (via structure.interfaces())</li>
 *   <li><strong>EXTENDS</strong> - Type → Superclass (via structure.superclass())</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * RelationshipGraphBuilder builder = new RelationshipGraphBuilder();
 * RelationshipGraph graph = builder.build(typeRegistry);
 *
 * // Navigate relationships
 * graph.from(aggregateId)
 *     .filter(r -> r.type() == RelationType.CONTAINS)
 *     .forEach(System.out::println);
 * }</pre>
 *
 * @since 5.0.0
 */
public final class RelationshipGraphBuilder {

    /**
     * Creates a new RelationshipGraphBuilder.
     */
    public RelationshipGraphBuilder() {}

    /**
     * Builds a RelationshipGraph from the given type registry.
     *
     * @param registry the type registry containing all classified types
     * @return a new RelationshipGraph with all detected relationships
     * @throws NullPointerException if registry is null
     */
    public RelationshipGraph build(TypeRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");

        RelationshipGraph.Builder graphBuilder = RelationshipGraph.builder();

        // Process all types
        registry.all(ArchType.class).forEach(type -> processType(type, registry, graphBuilder));

        return graphBuilder.build();
    }

    private void processType(ArchType type, TypeRegistry registry, RelationshipGraph.Builder graphBuilder) {
        if (type instanceof AggregateRoot aggregate) {
            processAggregateRoot(aggregate, graphBuilder);
        }

        if (type instanceof DrivenPort drivenPort) {
            processDrivenPort(drivenPort, registry, graphBuilder);
        }

        if (type instanceof DomainService domainService) {
            processDomainService(domainService, registry, graphBuilder);
        }

        if (type instanceof Entity entity) {
            processEntity(entity, registry, graphBuilder);
        }

        // Process common relationships for all types
        processInheritance(type, registry, graphBuilder);
        processDependencies(type, registry, graphBuilder);
    }

    /**
     * Processes an AggregateRoot to extract CONTAINS and EMITS relationships.
     */
    private void processAggregateRoot(AggregateRoot aggregate, RelationshipGraph.Builder graphBuilder) {
        TypeId aggregateId = aggregate.id();

        // CONTAINS: Aggregate -> Entity
        aggregate
                .entities()
                .forEach(entityRef -> graphBuilder.add(aggregateId, toTypeId(entityRef), RelationType.CONTAINS));

        // CONTAINS: Aggregate -> ValueObject
        aggregate
                .valueObjects()
                .forEach(voRef -> graphBuilder.add(aggregateId, toTypeId(voRef), RelationType.CONTAINS));

        // EMITS: Aggregate -> DomainEvent
        aggregate
                .domainEvents()
                .forEach(eventRef -> graphBuilder.add(aggregateId, toTypeId(eventRef), RelationType.EMITS));
    }

    /**
     * Processes a DrivenPort to extract PERSISTS relationships.
     */
    private void processDrivenPort(
            DrivenPort drivenPort, TypeRegistry registry, RelationshipGraph.Builder graphBuilder) {
        // PERSISTS: Repository -> Aggregate
        if (drivenPort.isRepository() && drivenPort.hasAggregate()) {
            drivenPort.managedAggregate().ifPresent(aggregateRef -> {
                TypeId aggregateId = toTypeId(aggregateRef);
                // Verify the aggregate exists in the registry
                if (registry.get(aggregateId).isPresent()) {
                    graphBuilder.add(drivenPort.id(), aggregateId, RelationType.PERSISTS);
                }
            });
        }
    }

    /**
     * Processes a DomainService to extract DEPENDS_ON relationships for injected ports.
     */
    private void processDomainService(
            DomainService domainService, TypeRegistry registry, RelationshipGraph.Builder graphBuilder) {
        TypeId serviceId = domainService.id();

        // DEPENDS_ON: DomainService -> Injected Port
        domainService.injectedPorts().forEach(portRef -> {
            TypeId portId = toTypeId(portRef);
            if (registry.get(portId).isPresent()) {
                graphBuilder.add(serviceId, portId, RelationType.DEPENDS_ON);
            }
        });
    }

    /**
     * Processes an Entity to extract owning aggregate relationship.
     *
     * <p>Note: The CONTAINS relationship is already created from the aggregate side
     * via processAggregateRoot. This method is kept for potential future enhancements
     * like inverse relationship tracking.</p>
     */
    @SuppressWarnings("unused") // Reserved for future inverse relationship tracking
    private void processEntity(Entity entity, TypeRegistry registry, RelationshipGraph.Builder graphBuilder) {
        // If entity has an owning aggregate, the CONTAINS relationship is already created
        // from the aggregate side. This method is a placeholder for potential future
        // enhancements like inverse relationship tracking or validation.
    }

    /**
     * Processes inheritance relationships (EXTENDS and IMPLEMENTS).
     */
    private void processInheritance(ArchType type, TypeRegistry registry, RelationshipGraph.Builder graphBuilder) {
        TypeId typeId = type.id();

        // EXTENDS: Type -> Superclass
        type.structure().superClass().ifPresent(superRef -> {
            TypeId superId = toTypeId(superRef);
            // Only add if superclass is a known classified type
            if (registry.get(superId).isPresent()) {
                graphBuilder.add(typeId, superId, RelationType.EXTENDS);
            }
        });

        // IMPLEMENTS: Type -> Interface
        type.structure().interfaces().forEach(ifaceRef -> {
            TypeId ifaceId = toTypeId(ifaceRef);
            // Only add if interface is a known classified type
            if (registry.get(ifaceId).isPresent()) {
                graphBuilder.add(typeId, ifaceId, RelationType.IMPLEMENTS);
            }
        });
    }

    /**
     * Processes field dependencies to extract DEPENDS_ON relationships.
     */
    private void processDependencies(ArchType type, TypeRegistry registry, RelationshipGraph.Builder graphBuilder) {
        TypeId typeId = type.id();

        // DEPENDS_ON: Type -> Field type (if field type is a classified type)
        type.structure().fields().forEach(field -> {
            Optional<TypeId> dependencyId = extractDependencyType(field, registry);
            dependencyId.ifPresent(depId -> {
                // Avoid self-references and already-handled relationships
                if (!depId.equals(typeId)) {
                    graphBuilder.add(typeId, depId, RelationType.DEPENDS_ON);
                }
            });
        });
    }

    /**
     * Extracts the dependency type from a field.
     *
     * <p>For collection types, extracts the element type.
     * For regular types, returns the field type.</p>
     */
    private Optional<TypeId> extractDependencyType(Field field, TypeRegistry registry) {
        // First, try element type for collections
        Optional<TypeRef> elementType = field.elementType();
        if (elementType.isPresent()) {
            TypeId elementId = toTypeId(elementType.get());
            if (registry.get(elementId).isPresent()) {
                return Optional.of(elementId);
            }
        }

        // Then, try the field type itself
        TypeId fieldTypeId = toTypeId(field.type());
        if (registry.get(fieldTypeId).isPresent()) {
            return Optional.of(fieldTypeId);
        }

        return Optional.empty();
    }

    /**
     * Converts a TypeRef to a TypeId.
     */
    private TypeId toTypeId(TypeRef typeRef) {
        return TypeId.of(typeRef.qualifiedName());
    }
}
