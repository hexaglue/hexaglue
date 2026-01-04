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

package io.hexaglue.plugin.jpa;

import io.hexaglue.spi.ir.*;
import java.util.List;
import java.util.Optional;

/**
 * Test fixtures for JPA plugin tests.
 */
final class TestFixtures {

    private TestFixtures() {}

    // ===== Identity Fixtures =====

    static Identity wrappedUuidIdentity(String fieldName, String wrapperType) {
        return new Identity(
                fieldName,
                TypeRef.of(wrapperType),
                TypeRef.of("java.util.UUID"),
                IdentityStrategy.ASSIGNED,
                IdentityWrapperKind.RECORD);
    }

    static Identity rawUuidIdentity(String fieldName) {
        return new Identity(
                fieldName,
                TypeRef.of("java.util.UUID"),
                TypeRef.of("java.util.UUID"),
                IdentityStrategy.ASSIGNED,
                IdentityWrapperKind.NONE);
    }

    static Identity rawLongIdentity(String fieldName) {
        return new Identity(
                fieldName,
                TypeRef.of("java.lang.Long"),
                TypeRef.of("java.lang.Long"),
                IdentityStrategy.AUTO,
                IdentityWrapperKind.NONE);
    }

    static Identity sequenceIdentity(String fieldName) {
        return new Identity(
                fieldName,
                TypeRef.of("java.lang.Long"),
                TypeRef.of("java.lang.Long"),
                IdentityStrategy.SEQUENCE,
                IdentityWrapperKind.NONE);
    }

    // ===== TypeRef Fixtures =====

    static TypeRef stringType() {
        return TypeRef.of("java.lang.String");
    }

    static TypeRef longType() {
        return TypeRef.of("java.lang.Long");
    }

    static TypeRef intType() {
        return TypeRef.of("java.lang.Integer");
    }

    static TypeRef booleanPrimitiveType() {
        return TypeRef.of("boolean");
    }

    static TypeRef intPrimitiveType() {
        return TypeRef.of("int");
    }

    static TypeRef uuidType() {
        return TypeRef.of("java.util.UUID");
    }

    static TypeRef instantType() {
        return TypeRef.of("java.time.Instant");
    }

    static TypeRef bigDecimalType() {
        return TypeRef.of("java.math.BigDecimal");
    }

    static TypeRef listOf(TypeRef elementType) {
        return TypeRef.parameterized("java.util.List", Cardinality.COLLECTION, List.of(elementType));
    }

    static TypeRef optionalOf(TypeRef elementType) {
        return TypeRef.parameterized("java.util.Optional", Cardinality.OPTIONAL, List.of(elementType));
    }

    // ===== DomainProperty Fixtures =====

    static DomainProperty simpleProperty(String name, TypeRef type) {
        return new DomainProperty(name, type, Cardinality.SINGLE, Nullability.NON_NULL, false, false, null);
    }

    static DomainProperty identityProperty(String name, TypeRef type) {
        return new DomainProperty(name, type, Cardinality.SINGLE, Nullability.NON_NULL, true, false, null);
    }

    static DomainProperty embeddedProperty(String name, TypeRef type) {
        return new DomainProperty(name, type, Cardinality.SINGLE, Nullability.NULLABLE, false, true, null);
    }

    static DomainProperty collectionProperty(String name, TypeRef elementType) {
        return new DomainProperty(
                name, listOf(elementType), Cardinality.COLLECTION, Nullability.NON_NULL, false, false, null);
    }

    // ===== DomainRelation Fixtures =====

    static DomainRelation oneToManyRelation(String propertyName, String targetFqn) {
        return DomainRelation.oneToMany(propertyName, targetFqn, DomainKind.ENTITY);
    }

    static DomainRelation embeddedRelation(String propertyName, String targetFqn) {
        return DomainRelation.embedded(propertyName, targetFqn);
    }

    static DomainRelation elementCollectionRelation(String propertyName, String targetFqn) {
        return new DomainRelation(
                propertyName,
                RelationKind.ELEMENT_COLLECTION,
                targetFqn,
                DomainKind.VALUE_OBJECT,
                null,
                CascadeType.ALL,
                FetchType.LAZY,
                false);
    }

    static DomainRelation manyToOneRelation(String propertyName, String targetFqn) {
        return DomainRelation.manyToOne(propertyName, targetFqn);
    }

    // ===== DomainType Fixtures =====

    static DomainType simpleAggregateRoot(String name, String pkg) {
        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + "." + name + "Id")),
                List.of(
                        identityProperty("id", TypeRef.of(pkg + "." + name + "Id")),
                        simpleProperty("name", stringType())),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    static DomainType aggregateRootWithRelations(String name, String pkg, List<DomainRelation> relations) {
        // Build properties from relations - each relation needs a corresponding property
        List<DomainProperty> props = new java.util.ArrayList<>();
        props.add(identityProperty("id", TypeRef.of(pkg + "." + name + "Id")));
        props.add(simpleProperty("name", stringType()));

        // Add properties for each relation
        for (DomainRelation rel : relations) {
            TypeRef targetType = TypeRef.of(rel.targetTypeFqn());
            Cardinality card =
                    switch (rel.kind()) {
                        case ONE_TO_MANY, MANY_TO_MANY, ELEMENT_COLLECTION -> Cardinality.COLLECTION;
                        default -> Cardinality.SINGLE;
                    };
            boolean isEmbedded = rel.kind() == RelationKind.EMBEDDED;
            DomainProperty prop = new DomainProperty(
                    rel.propertyName(),
                    card == Cardinality.COLLECTION ? listOf(targetType) : targetType,
                    card,
                    Nullability.NULLABLE,
                    false,
                    isEmbedded,
                    null);
            props.add(prop);
        }

        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + "." + name + "Id")),
                props,
                relations,
                List.of(),
                SourceRef.unknown());
    }

    static DomainType simpleEntity(String name, String pkg) {
        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(rawLongIdentity("id")),
                List.of(identityProperty("id", longType()), simpleProperty("name", stringType())),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    static DomainType simpleValueObject(String name, String pkg) {
        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.VALUE_OBJECT,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(simpleProperty("value", stringType())),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    static DomainType moneyValueObject(String pkg) {
        return new DomainType(
                pkg + ".Money",
                "Money",
                pkg,
                DomainKind.VALUE_OBJECT,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(simpleProperty("amount", bigDecimalType()), simpleProperty("currency", stringType())),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    static DomainType orderAggregate(String pkg) {
        TypeRef orderLineType = TypeRef.of(pkg + ".OrderLine");
        TypeRef addressType = TypeRef.of(pkg + ".Address");

        List<DomainRelation> relations = List.of(
                oneToManyRelation("lines", pkg + ".OrderLine"), embeddedRelation("shippingAddress", pkg + ".Address"));

        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("customerId", uuidType()),
                collectionProperty("lines", orderLineType),
                embeddedProperty("shippingAddress", addressType),
                simpleProperty("status", stringType()));

        return new DomainType(
                pkg + ".Order",
                "Order",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".OrderId")),
                props,
                relations,
                List.of(),
                SourceRef.unknown());
    }

    static DomainType customerAggregate(String pkg) {
        TypeRef emailType = TypeRef.of(pkg + ".Email");
        TypeRef addressType = TypeRef.of(pkg + ".Address");

        List<DomainRelation> relations = List.of(
                embeddedRelation("email", pkg + ".Email"), embeddedRelation("billingAddress", pkg + ".Address"));

        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".CustomerId")),
                simpleProperty("firstName", stringType()),
                simpleProperty("lastName", stringType()),
                embeddedProperty("email", emailType),
                embeddedProperty("billingAddress", addressType),
                simpleProperty("createdAt", instantType()));

        return new DomainType(
                pkg + ".Customer",
                "Customer",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".CustomerId")),
                props,
                relations,
                List.of(),
                SourceRef.unknown());
    }

    // ===== Port Fixtures =====

    static Port simpleRepository(String entityName, String pkg, String idType) {
        return new Port(
                pkg + ".ports.out." + entityName + "Repository",
                entityName + "Repository",
                pkg + ".ports.out",
                PortKind.REPOSITORY,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                List.of(pkg + "." + entityName),
                List.of(
                        new PortMethod("save", pkg + "." + entityName, List.of(pkg + "." + entityName)),
                        new PortMethod(
                                "findById", "java.util.Optional<" + pkg + "." + entityName + ">", List.of(idType)),
                        new PortMethod("findAll", "java.util.List<" + pkg + "." + entityName + ">", List.of()),
                        new PortMethod("delete", "void", List.of(pkg + "." + entityName))),
                List.of(),
                SourceRef.unknown());
    }

    static Port repositoryWithCustomMethods(String entityName, String pkg, String idType) {
        return new Port(
                pkg + ".ports.out." + entityName + "Repository",
                entityName + "Repository",
                pkg + ".ports.out",
                PortKind.REPOSITORY,
                PortDirection.DRIVEN,
                ConfidenceLevel.HIGH,
                List.of(pkg + "." + entityName),
                List.of(
                        new PortMethod("save", pkg + "." + entityName, List.of(pkg + "." + entityName)),
                        new PortMethod(
                                "findById", "java.util.Optional<" + pkg + "." + entityName + ">", List.of(idType)),
                        new PortMethod("findAll", "java.util.List<" + pkg + "." + entityName + ">", List.of()),
                        new PortMethod("delete", "void", List.of(pkg + "." + entityName)),
                        new PortMethod(
                                "findByEmail",
                                "java.util.Optional<" + pkg + "." + entityName + ">",
                                List.of(pkg + ".Email")),
                        new PortMethod("existsByEmail", "boolean", List.of(pkg + ".Email")),
                        new PortMethod(
                                "findByCustomerId",
                                "java.util.List<" + pkg + "." + entityName + ">",
                                List.of(pkg + ".CustomerId"))),
                List.of(),
                SourceRef.unknown());
    }

    static Port drivingPort(String name, String pkg, List<PortMethod> methods) {
        return new Port(
                pkg + ".ports.in." + name,
                name,
                pkg + ".ports.in",
                PortKind.USE_CASE,
                PortDirection.DRIVING,
                ConfidenceLevel.HIGH,
                List.of(),
                methods,
                List.of(),
                SourceRef.unknown());
    }
}
