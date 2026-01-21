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

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds {@link DomainService} instances from classification results.
 *
 * <p>This builder transforms a classified {@link TypeNode} into a {@link DomainService}
 * by constructing the type structure and classification trace.</p>
 *
 * <h2>Injected Ports Detection (since 5.0.0)</h2>
 * <p>The builder detects ports injected via fields by looking for field types that
 * are classified as DRIVING_PORT or DRIVEN_PORT. Common patterns include:</p>
 * <ul>
 *   <li>Fields with types ending in "Port" (e.g., InventoryPort)</li>
 *   <li>Fields with types ending in "Repository" (e.g., OrderRepository)</li>
 *   <li>Fields with types ending in "Gateway" (e.g., PaymentGateway)</li>
 * </ul>
 *
 * <h2>Operations Detection (since 5.0.0)</h2>
 * <p>The builder extracts business operations by filtering out:</p>
 * <ul>
 *   <li>Getters and setters (GETTER, SETTER roles)</li>
 *   <li>Object methods (equals, hashCode, toString)</li>
 *   <li>Static methods</li>
 *   <li>Private methods</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainServiceBuilder builder = new DomainServiceBuilder(structureBuilder, traceConverter);
 * DomainService service = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 * @since 5.0.0 added injected ports and operations detection
 */
public final class DomainServiceBuilder {

    private static final Set<String> PORT_TYPE_SUFFIXES =
            Set.of("Port", "Repository", "Gateway", "Publisher", "Notifier", "Client", "Service");

    private static final Set<MethodRole> EXCLUDED_ROLES =
            Set.of(MethodRole.GETTER, MethodRole.SETTER, MethodRole.OBJECT_METHOD);

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;

    /**
     * Creates a new DomainServiceBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @throws NullPointerException if any argument is null
     */
    public DomainServiceBuilder(TypeStructureBuilder structureBuilder, ClassificationTraceConverter traceConverter) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
    }

    /**
     * Builds a DomainService from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built DomainService
     * @throws NullPointerException if any argument is null
     */
    public DomainService build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);

        List<TypeRef> injectedPorts = detectInjectedPorts(structure, context);
        List<Method> operations = extractOperations(structure);

        return DomainService.of(id, structure, trace, injectedPorts, operations);
    }

    /**
     * Detects ports injected via fields.
     *
     * <p>Detection is based on field type names matching port patterns
     * or being classified as ports.</p>
     *
     * @param structure the type structure
     * @param context the builder context
     * @return list of injected port types
     * @since 5.0.0
     */
    private List<TypeRef> detectInjectedPorts(TypeStructure structure, BuilderContext context) {
        return structure.fields().stream()
                .map(Field::type)
                .filter(type -> isPortType(type, context))
                .toList();
    }

    /**
     * Checks if a type is a port type.
     *
     * @param type the type to check
     * @param context the builder context
     * @return true if the type is a port
     */
    private boolean isPortType(TypeRef type, BuilderContext context) {
        String simpleName = type.simpleName();

        // Check if classified as a port
        if (isClassifiedAsPort(type.qualifiedName(), context)) {
            return true;
        }

        // Check naming conventions
        return PORT_TYPE_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }

    /**
     * Checks if a type is classified as a port (DRIVING_PORT or DRIVEN_PORT).
     *
     * @param qualifiedName the type name
     * @param context the builder context
     * @return true if classified as a port
     */
    private boolean isClassifiedAsPort(String qualifiedName, BuilderContext context) {
        return context.isClassifiedAs(qualifiedName, "DRIVING_PORT")
                || context.isClassifiedAs(qualifiedName, "DRIVEN_PORT");
    }

    /**
     * Extracts business operations from the type structure.
     *
     * <p>Filters out getters, setters, object methods, static methods,
     * and private methods.</p>
     *
     * @param structure the type structure
     * @return list of business operation methods
     * @since 5.0.0
     */
    private List<Method> extractOperations(TypeStructure structure) {
        return structure.methods().stream().filter(this::isBusinessOperation).toList();
    }

    /**
     * Checks if a method is a business operation (not an accessor or object method).
     *
     * @param method the method to check
     * @return true if the method is a business operation
     */
    private boolean isBusinessOperation(Method method) {
        // Exclude static and private methods
        if (method.isStatic() || !method.isPublic()) {
            return false;
        }

        // Exclude getters, setters, and object methods
        return method.roles().stream().noneMatch(EXCLUDED_ROLES::contains);
    }
}
