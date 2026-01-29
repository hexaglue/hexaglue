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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Utility class for building v5 architectural models in tests.
 *
 * <p>This builder provides factory methods for creating v5 ArchType instances
 * and constructing complete ArchitecturalModel instances with TypeRegistry,
 * DomainIndex, and PortIndex.</p>
 *
 * @since 5.0.0
 */
public final class V5TestModelBuilder {

    private V5TestModelBuilder() {
        // Utility class
    }

    // === Model Creation ===

    /**
     * Creates an ArchitecturalModel from the given project context and arch types.
     *
     * @param project the project context
     * @param types the architectural types to include
     * @return a complete ArchitecturalModel with TypeRegistry, DomainIndex, and PortIndex
     */
    public static ArchitecturalModel createModel(ProjectContext project, ArchType... types) {
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        Arrays.stream(types).forEach(registryBuilder::add);
        TypeRegistry registry = registryBuilder.build();

        DomainIndex domainIndex = DomainIndex.from(registry);
        PortIndex portIndex = PortIndex.from(registry);

        return ArchitecturalModel.builder(project)
                .typeRegistry(registry)
                .domainIndex(domainIndex)
                .portIndex(portIndex)
                .build();
    }

    // === Classification Trace Factory ===

    /**
     * Creates a high confidence classification trace.
     *
     * @param kind the element kind
     * @return a ClassificationTrace
     */
    public static ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    // === TypeStructure Factory ===

    /**
     * Creates an empty TypeStructure for a record.
     *
     * @return a TypeStructure for an empty record
     */
    public static TypeStructure emptyRecordStructure() {
        return TypeStructure.builder(TypeNature.RECORD).build();
    }

    /**
     * Creates an empty TypeStructure for a class.
     *
     * @return a TypeStructure for an empty class
     */
    public static TypeStructure emptyClassStructure() {
        return TypeStructure.builder(TypeNature.CLASS).build();
    }

    /**
     * Creates an empty TypeStructure for an interface.
     *
     * @return a TypeStructure for an empty interface
     */
    public static TypeStructure emptyInterfaceStructure() {
        return TypeStructure.builder(TypeNature.INTERFACE).build();
    }

    /**
     * Creates a TypeStructure with the given fields.
     *
     * @param nature the type nature
     * @param fields the fields
     * @return a TypeStructure
     */
    public static TypeStructure structureWithFields(TypeNature nature, List<Field> fields) {
        return TypeStructure.builder(nature).fields(fields).build();
    }

    /**
     * Creates a TypeStructure with the given methods.
     *
     * @param nature the type nature
     * @param methods the methods
     * @return a TypeStructure
     */
    public static TypeStructure structureWithMethods(TypeNature nature, List<Method> methods) {
        return TypeStructure.builder(nature).methods(methods).build();
    }

    // === AggregateRoot Factory ===

    /**
     * Creates an AggregateRoot with the given qualified name and identity field.
     *
     * @param qualifiedName the fully qualified name
     * @param idFieldName the name of the identity field
     * @param idType the type of the identity field
     * @return an AggregateRoot
     */
    public static AggregateRoot aggregateRoot(String qualifiedName, String idFieldName, TypeRef idType) {
        Field idField = Field.builder(idFieldName, idType)
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure =
                TypeStructure.builder(TypeNature.CLASS).fields(List.of(idField)).build();

        return AggregateRoot.builder(
                        TypeId.of(qualifiedName), structure, highConfidence(ElementKind.AGGREGATE_ROOT), idField)
                .build();
    }

    /**
     * Creates a minimal AggregateRoot with just a name.
     *
     * @param qualifiedName the fully qualified name
     * @return an AggregateRoot with a default "id" field of type UUID
     */
    public static AggregateRoot aggregateRoot(String qualifiedName) {
        return aggregateRoot(qualifiedName, "id", TypeRef.of("java.util.UUID"));
    }

    // === Entity Factory ===

    /**
     * Creates an Entity with the given qualified name.
     *
     * @param qualifiedName the fully qualified name
     * @return an Entity
     */
    public static Entity entity(String qualifiedName) {
        return Entity.of(TypeId.of(qualifiedName), emptyClassStructure(), highConfidence(ElementKind.ENTITY));
    }

    /**
     * Creates an Entity with the given qualified name and identity field.
     *
     * @param qualifiedName the fully qualified name
     * @param idFieldName the name of the identity field
     * @param idType the type of the identity field
     * @return an Entity
     */
    public static Entity entity(String qualifiedName, String idFieldName, TypeRef idType) {
        Field idField = Field.builder(idFieldName, idType)
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure =
                TypeStructure.builder(TypeNature.CLASS).fields(List.of(idField)).build();

        return Entity.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.ENTITY), idField);
    }

    // === ValueObject Factory ===

    /**
     * Creates a ValueObject with the given qualified name and field names.
     *
     * @param qualifiedName the fully qualified name
     * @param fieldNames the names of the fields (all typed as String)
     * @return a ValueObject
     */
    public static ValueObject valueObject(String qualifiedName, List<String> fieldNames) {
        List<Field> fields = fieldNames.stream()
                .map(name -> Field.of(name, TypeRef.of("java.lang.String")))
                .toList();

        TypeStructure structure =
                TypeStructure.builder(TypeNature.RECORD).fields(fields).build();

        return ValueObject.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.VALUE_OBJECT));
    }

    /**
     * Creates a ValueObject with the given qualified name and typed fields.
     *
     * @param qualifiedName the fully qualified name
     * @param fields the fields
     * @return a ValueObject
     */
    public static ValueObject valueObject(String qualifiedName, Field... fields) {
        TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                .fields(Arrays.asList(fields))
                .build();

        return ValueObject.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.VALUE_OBJECT));
    }

    // === Identifier Factory ===

    /**
     * Creates an Identifier with the given qualified name and wrapped type.
     *
     * @param qualifiedName the fully qualified name
     * @param wrappedType the type that this identifier wraps (e.g., UUID, Long)
     * @return an Identifier
     */
    public static Identifier identifier(String qualifiedName, TypeRef wrappedType) {
        Field valueField =
                Field.builder("value", wrappedType).wrappedType(wrappedType).build();

        TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                .fields(List.of(valueField))
                .build();

        return Identifier.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.IDENTIFIER), wrappedType);
    }

    /**
     * Creates an Identifier wrapping UUID.
     *
     * @param qualifiedName the fully qualified name
     * @return an Identifier
     */
    public static Identifier identifier(String qualifiedName) {
        return identifier(qualifiedName, TypeRef.of("java.util.UUID"));
    }

    // === DomainEvent Factory ===

    /**
     * Creates a DomainEvent with the given qualified name.
     *
     * @param qualifiedName the fully qualified name
     * @return a DomainEvent
     */
    public static DomainEvent domainEvent(String qualifiedName) {
        return DomainEvent.of(
                TypeId.of(qualifiedName), emptyRecordStructure(), highConfidence(ElementKind.DOMAIN_EVENT));
    }

    /**
     * Creates a DomainEvent with fields.
     *
     * @param qualifiedName the fully qualified name
     * @param fields the event fields
     * @return a DomainEvent
     */
    public static DomainEvent domainEvent(String qualifiedName, List<Field> fields) {
        TypeStructure structure =
                TypeStructure.builder(TypeNature.RECORD).fields(fields).build();

        return DomainEvent.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.DOMAIN_EVENT));
    }

    // === DomainService Factory ===

    /**
     * Creates a DomainService with the given qualified name.
     *
     * @param qualifiedName the fully qualified name
     * @return a DomainService
     */
    public static DomainService domainService(String qualifiedName) {
        return DomainService.of(
                TypeId.of(qualifiedName), emptyClassStructure(), highConfidence(ElementKind.DOMAIN_SERVICE));
    }

    // === ApplicationService Factory ===

    /**
     * Creates an ApplicationService with the given qualified name.
     *
     * @param qualifiedName the fully qualified name
     * @return an ApplicationService
     */
    public static ApplicationService applicationService(String qualifiedName) {
        return ApplicationService.of(
                TypeId.of(qualifiedName), emptyClassStructure(), highConfidence(ElementKind.APPLICATION_SERVICE));
    }

    // === DrivingPort Factory ===

    /**
     * Creates a DrivingPort with the given qualified name and methods.
     *
     * @param qualifiedName the fully qualified name
     * @param methods the port methods
     * @return a DrivingPort
     */
    public static DrivingPort drivingPort(String qualifiedName, List<Method> methods) {
        TypeStructure structure =
                TypeStructure.builder(TypeNature.INTERFACE).methods(methods).build();

        return DrivingPort.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.DRIVING_PORT));
    }

    /**
     * Creates a DrivingPort with no methods.
     *
     * @param qualifiedName the fully qualified name
     * @return a DrivingPort
     */
    public static DrivingPort drivingPort(String qualifiedName) {
        return DrivingPort.of(
                TypeId.of(qualifiedName), emptyInterfaceStructure(), highConfidence(ElementKind.DRIVING_PORT));
    }

    // === DrivenPort Factory ===

    /**
     * Creates a DrivenPort with the given qualified name, type, and methods.
     *
     * @param qualifiedName the fully qualified name
     * @param portType the type of driven port (REPOSITORY, GATEWAY, etc.)
     * @param methods the port methods
     * @return a DrivenPort
     */
    public static DrivenPort drivenPort(String qualifiedName, DrivenPortType portType, List<Method> methods) {
        TypeStructure structure =
                TypeStructure.builder(TypeNature.INTERFACE).methods(methods).build();

        return DrivenPort.of(TypeId.of(qualifiedName), structure, highConfidence(ElementKind.DRIVEN_PORT), portType);
    }

    /**
     * Creates a DrivenPort with no methods.
     *
     * @param qualifiedName the fully qualified name
     * @param portType the type of driven port
     * @return a DrivenPort
     */
    public static DrivenPort drivenPort(String qualifiedName, DrivenPortType portType) {
        return DrivenPort.of(
                TypeId.of(qualifiedName), emptyInterfaceStructure(), highConfidence(ElementKind.DRIVEN_PORT), portType);
    }

    /**
     * Creates a repository DrivenPort with a managed aggregate.
     *
     * @param qualifiedName the fully qualified name
     * @param aggregateType the aggregate type managed by this repository
     * @param methods the repository methods
     * @return a DrivenPort
     */
    public static DrivenPort repository(String qualifiedName, TypeRef aggregateType, List<Method> methods) {
        TypeStructure structure =
                TypeStructure.builder(TypeNature.INTERFACE).methods(methods).build();

        return DrivenPort.repository(
                TypeId.of(qualifiedName), structure, highConfidence(ElementKind.DRIVEN_PORT), aggregateType);
    }

    // === Method Factory ===

    /**
     * Creates a Method with the given name and return type.
     *
     * @param name the method name
     * @param returnType the return type
     * @return a Method
     */
    public static Method method(String name, TypeRef returnType) {
        return Method.of(name, returnType);
    }

    /**
     * Creates a Method with the given name, return type, and parameters.
     *
     * @param name the method name
     * @param returnType the return type
     * @param paramTypes the parameter types
     * @return a Method
     */
    public static Method method(String name, TypeRef returnType, List<TypeRef> paramTypes) {
        List<Parameter> params = paramTypes.stream()
                .map(type -> Parameter.of("param" + paramTypes.indexOf(type), type))
                .toList();

        return new Method(
                name,
                returnType,
                params,
                Set.of(),
                List.of(),
                Optional.empty(),
                List.of(),
                Set.of(),
                OptionalInt.empty(),
                Optional.empty());
    }

    /**
     * TypeRef representing void return type.
     */
    private static final TypeRef VOID_TYPE = TypeRef.of("void");

    /**
     * Creates a void Method with the given name and parameters.
     *
     * @param name the method name
     * @param paramTypes the parameter types
     * @return a Method
     */
    public static Method voidMethod(String name, List<TypeRef> paramTypes) {
        return method(name, VOID_TYPE, paramTypes);
    }

    // === Field Factory ===

    /**
     * Creates a Field with the given name and type.
     *
     * @param name the field name
     * @param type the field type
     * @return a Field
     */
    public static Field field(String name, TypeRef type) {
        return Field.of(name, type);
    }

    /**
     * Creates a Field with the given name and type as a String type name.
     *
     * @param name the field name
     * @param typeName the type name
     * @return a Field
     */
    public static Field field(String name, String typeName) {
        return Field.of(name, TypeRef.of(typeName));
    }
}
