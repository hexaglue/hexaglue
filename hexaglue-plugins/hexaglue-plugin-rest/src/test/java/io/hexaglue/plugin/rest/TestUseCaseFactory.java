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

package io.hexaglue.plugin.rest;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Factory for building test instances of domain model types.
 */
public final class TestUseCaseFactory {

    private TestUseCaseFactory() {
        /* prevent instantiation */
    }

    /**
     * Creates a QUERY use case.
     *
     * @param name the use case name (e.g., "getAccount")
     * @return a query use case with non-void return
     */
    public static UseCase query(String name) {
        return UseCase.of(Method.of(name, TypeRef.of("java.lang.Object")), UseCaseType.QUERY);
    }

    /**
     * Creates a COMMAND use case.
     *
     * @param name the use case name (e.g., "closeAccount")
     * @return a command use case with void return
     */
    public static UseCase command(String name) {
        return UseCase.of(Method.of(name, TypeRef.of("void")), UseCaseType.COMMAND);
    }

    /**
     * Creates a COMMAND_QUERY use case.
     *
     * @param name the use case name (e.g., "openAccount")
     * @return a command-query use case with non-void return
     */
    public static UseCase commandQuery(String name) {
        return UseCase.of(Method.of(name, TypeRef.of("java.lang.Object")), UseCaseType.COMMAND_QUERY);
    }

    /**
     * Creates a COMMAND use case with parameters.
     *
     * @param name   the use case name
     * @param params the method parameters
     * @return a command use case with the given parameters
     */
    public static UseCase commandWithParams(String name, List<Parameter> params) {
        Method method = new Method(
                name,
                TypeRef.of("void"),
                params,
                Set.of(),
                List.of(),
                Optional.empty(),
                List.of(),
                Set.of(),
                OptionalInt.empty(),
                Optional.empty());
        return UseCase.of(method, UseCaseType.COMMAND);
    }

    /**
     * Creates a COMMAND_QUERY use case with parameters.
     *
     * @param name       the use case name
     * @param returnType the return type
     * @param params     the method parameters
     * @return a command-query use case with the given parameters and return type
     */
    public static UseCase commandQueryWithParams(String name, TypeRef returnType, List<Parameter> params) {
        Method method = new Method(
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
        return UseCase.of(method, UseCaseType.COMMAND_QUERY);
    }

    /**
     * Creates a QUERY use case with parameters.
     *
     * @param name       the use case name
     * @param returnType the return type
     * @param params     the method parameters
     * @return a query use case with the given parameters
     */
    public static UseCase queryWithParams(String name, TypeRef returnType, List<Parameter> params) {
        Method method = new Method(
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
        return UseCase.of(method, UseCaseType.QUERY);
    }

    /**
     * Creates an Identifier domain type.
     *
     * @param qualifiedName the fully qualified name (e.g., "com.acme.CustomerId")
     * @param wrappedType   the wrapped type (e.g., "java.lang.Long")
     * @return an Identifier
     */
    public static Identifier identifier(String qualifiedName, String wrappedType) {
        return Identifier.of(
                TypeId.of(qualifiedName),
                TypeStructure.builder(TypeNature.RECORD).build(),
                ClassificationTrace.highConfidence(ElementKind.IDENTIFIER, "test", "test identifier"),
                TypeRef.of(wrappedType));
    }

    /**
     * Creates a single-field ValueObject.
     *
     * @param qualifiedName the fully qualified name
     * @param fieldName     the field name
     * @param fieldType     the field type
     * @return a ValueObject with one field
     */
    public static ValueObject singleFieldValueObject(String qualifiedName, String fieldName, String fieldType) {
        Field field = Field.of(fieldName, TypeRef.of(fieldType));
        TypeStructure structure =
                TypeStructure.builder(TypeNature.RECORD).fields(List.of(field)).build();
        return ValueObject.of(
                TypeId.of(qualifiedName),
                structure,
                ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "test VO"));
    }

    /**
     * Creates a multi-field ValueObject.
     *
     * @param qualifiedName the fully qualified name
     * @param fields        the fields
     * @return a ValueObject with multiple fields
     */
    public static ValueObject multiFieldValueObject(String qualifiedName, List<Field> fields) {
        TypeStructure structure =
                TypeStructure.builder(TypeNature.RECORD).fields(fields).build();
        return ValueObject.of(
                TypeId.of(qualifiedName),
                structure,
                ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "test VO"));
    }

    /**
     * Creates a DomainIndex from the given arch types.
     *
     * @param types the arch types to register
     * @return a DomainIndex
     */
    public static DomainIndex domainIndex(ArchType... types) {
        TypeRegistry.Builder builder = TypeRegistry.builder();
        for (ArchType type : types) {
            builder.add(type);
        }
        return DomainIndex.from(builder.build());
    }

    /**
     * Creates a DrivingPort with the given use cases.
     *
     * @param qualifiedName the fully qualified port name
     * @param useCases      the use cases
     * @return a driving port
     */
    public static DrivingPort drivingPort(String qualifiedName, List<UseCase> useCases) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = TypeStructure.builder(TypeNature.INTERFACE).build();
        ClassificationTrace trace =
                ClassificationTrace.highConfidence(ElementKind.DRIVING_PORT, "test", "test driving port");
        return DrivingPort.of(id, structure, trace, useCases, List.of(), List.of());
    }
}
