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

import io.hexaglue.arch.ElementKind;
import io.hexaglue.spi.ir.Cardinality;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.Nullability;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.ir.PortMethod;
import io.hexaglue.spi.ir.RelationInfo;
import io.hexaglue.spi.ir.RelationKind;
import io.hexaglue.spi.ir.SourceRef;
import io.hexaglue.spi.ir.TypeRef;
import java.util.List;
import java.util.Optional;

/**
 * Test fixtures for Living Documentation plugin tests.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ============ Identity Builders ============

    public static Identity wrappedIdentity(String fieldName, String typeName, String underlyingType) {
        TypeRef type = TypeRef.of("com.example.domain." + typeName);
        TypeRef underlying = TypeRef.of("java.util." + underlyingType);
        return Identity.wrapped(
                fieldName, type, underlying, IdentityStrategy.ASSIGNED, IdentityWrapperKind.RECORD, "value");
    }

    public static Identity unwrappedIdentity(String fieldName, String typeName) {
        TypeRef type = TypeRef.of("java.lang." + typeName);
        return Identity.unwrapped(fieldName, type, IdentityStrategy.AUTO);
    }

    public static Identity generatedIdentity(String fieldName) {
        TypeRef longType = TypeRef.of("java.lang.Long");
        return Identity.unwrapped(fieldName, longType, IdentityStrategy.AUTO);
    }

    public static Identity uuidIdentity(String fieldName, String wrapperTypeName) {
        TypeRef wrapperType = TypeRef.of("com.example.domain." + wrapperTypeName);
        TypeRef uuidType = TypeRef.of("java.util.UUID");
        return Identity.wrapped(
                fieldName, wrapperType, uuidType, IdentityStrategy.ASSIGNED, IdentityWrapperKind.RECORD, "value");
    }

    // ============ Property Builders ============

    public static DomainProperty simpleProperty(String name, String type) {
        return new DomainProperty(name, TypeRef.of(type), Cardinality.SINGLE, Nullability.NON_NULL, false, false, null);
    }

    public static DomainProperty nullableProperty(String name, String type) {
        return new DomainProperty(name, TypeRef.of(type), Cardinality.SINGLE, Nullability.NULLABLE, false, false, null);
    }

    public static DomainProperty collectionProperty(String name, String elementType) {
        TypeRef type = TypeRef.parameterized("java.util.List", TypeRef.of(elementType));
        return new DomainProperty(name, type, Cardinality.COLLECTION, Nullability.NON_NULL, false, false, null);
    }

    public static DomainProperty optionalProperty(String name, String elementType) {
        TypeRef type = TypeRef.parameterized("java.util.Optional", TypeRef.of(elementType));
        return new DomainProperty(name, type, Cardinality.OPTIONAL, Nullability.NON_NULL, false, false, null);
    }

    public static DomainProperty embeddedProperty(String name, String type) {
        return new DomainProperty(name, TypeRef.of(type), Cardinality.SINGLE, Nullability.NON_NULL, false, true, null);
    }

    public static DomainProperty propertyWithRelation(String name, String type, RelationKind kind, String targetType) {
        RelationInfo relationInfo = RelationInfo.unidirectional(kind, targetType);
        return new DomainProperty(
                name, TypeRef.of(type), Cardinality.SINGLE, Nullability.NON_NULL, false, false, relationInfo);
    }

    // ============ Relation Builders ============

    public static DomainRelation oneToManyRelation(String propertyName, String targetType, ElementKind targetKind) {
        return new DomainRelation(
                propertyName,
                RelationKind.ONE_TO_MANY,
                targetType,
                targetKind,
                null,
                CascadeType.ALL,
                FetchType.LAZY,
                true);
    }

    public static DomainRelation manyToOneRelation(
            String propertyName, String targetType, ElementKind targetKind, String mappedBy) {
        return new DomainRelation(
                propertyName,
                RelationKind.MANY_TO_ONE,
                targetType,
                targetKind,
                mappedBy,
                CascadeType.NONE,
                FetchType.EAGER,
                false);
    }

    public static DomainRelation embeddedRelation(String propertyName, String targetType, ElementKind targetKind) {
        return new DomainRelation(
                propertyName,
                RelationKind.EMBEDDED,
                targetType,
                targetKind,
                null,
                CascadeType.NONE,
                FetchType.EAGER,
                false);
    }

    // ============ Domain Type Builders ============

    public static DomainType aggregateRoot(
            String name,
            String packageName,
            Identity identity,
            List<DomainProperty> properties,
            List<DomainRelation> relations) {
        return new DomainType(
                packageName + "." + name,
                name,
                packageName,
                ElementKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(identity),
                properties,
                relations,
                List.of("jakarta.persistence.Entity", "jakarta.persistence.Table"),
                sourceRef(packageName, name, 10, 50));
    }

    public static DomainType entity(
            String name, String packageName, Identity identity, List<DomainProperty> properties) {
        return new DomainType(
                packageName + "." + name,
                name,
                packageName,
                ElementKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(identity),
                properties,
                List.of(),
                List.of("jakarta.persistence.Entity"),
                sourceRef(packageName, name, 15, 45));
    }

    public static DomainType valueObject(String name, String packageName, List<DomainProperty> properties) {
        return new DomainType(
                packageName + "." + name,
                name,
                packageName,
                ElementKind.VALUE_OBJECT,
                ConfidenceLevel.EXPLICIT,
                JavaConstruct.RECORD,
                Optional.empty(),
                properties,
                List.of(),
                List.of("jakarta.persistence.Embeddable"),
                sourceRef(packageName, name, 8, 20));
    }

    public static DomainType identifier(String name, String packageName, DomainProperty valueProperty) {
        return new DomainType(
                packageName + "." + name,
                name,
                packageName,
                ElementKind.IDENTIFIER,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(valueProperty),
                List.of(),
                List.of(),
                sourceRef(packageName, name, 5, 10));
    }

    // ============ Port Builders ============

    public static Port drivingPort(
            String name, String packageName, PortKind kind, List<PortMethod> methods, List<String> managedTypes) {
        return new Port(
                packageName + "." + name,
                name,
                packageName,
                kind,
                PortDirection.DRIVING,
                ConfidenceLevel.HIGH,
                managedTypes,
                managedTypes.isEmpty() ? null : managedTypes.get(0),
                methods,
                List.of("org.springframework.stereotype.Service"),
                sourceRef(packageName, name, 12, 35));
    }

    public static Port drivenPort(
            String name, String packageName, PortKind kind, List<PortMethod> methods, List<String> managedTypes) {
        return new Port(
                packageName + "." + name,
                name,
                packageName,
                kind,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                managedTypes,
                managedTypes.isEmpty() ? null : managedTypes.get(0),
                methods,
                List.of("org.springframework.stereotype.Repository"),
                sourceRef(packageName, name, 8, 25));
    }

    // ============ Port Method Builders ============

    public static PortMethod method(String name, String returnType, String... parameters) {
        return PortMethod.legacy(name, returnType, List.of(parameters));
    }

    public static PortMethod voidMethod(String name, String... parameters) {
        return PortMethod.legacy(name, "void", List.of(parameters));
    }

    // ============ Helper Methods ============

    private static SourceRef sourceRef(String packageName, String name, int lineStart, int lineEnd) {
        String filePath = "src/main/java/" + packageName.replace('.', '/') + "/" + name + ".java";
        return new SourceRef(filePath, lineStart, lineEnd);
    }
}
