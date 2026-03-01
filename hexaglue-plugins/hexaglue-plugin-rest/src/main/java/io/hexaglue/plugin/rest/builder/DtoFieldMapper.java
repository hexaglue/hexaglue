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

package io.hexaglue.plugin.rest.builder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ProjectionKind;
import io.hexaglue.plugin.rest.model.ValidationKind;
import io.hexaglue.plugin.rest.util.NamingConventions;
import io.hexaglue.syntax.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Maps use case parameters to DTO field specifications.
 *
 * <p>Resolves domain types (Identifiers, Value Objects) from the {@link DomainIndex}
 * and produces appropriate field specs with validation annotations and projection kinds.
 *
 * @since 3.1.0
 */
public final class DtoFieldMapper {

    private static final Set<String> PRIMITIVES =
            Set.of("int", "long", "boolean", "double", "float", "byte", "short", "char");

    private DtoFieldMapper() {
        /* utility class */
    }

    /**
     * Maps a use case parameter to DTO field spec(s) for a request DTO.
     *
     * <p>Returns multiple specs for multi-field VOs (flattening).
     *
     * @param param       the use case parameter
     * @param domainIndex the domain index for identifier/VO lookup
     * @param config      the plugin configuration
     * @return list of DTO field specs (may contain multiple entries for flattened VOs)
     */
    public static List<DtoFieldSpec> mapForRequest(Parameter param, DomainIndex domainIndex, RestConfig config) {
        TypeRef paramType = param.type();
        String paramName = param.name();

        // 1. Check if Identifier
        Optional<Identifier> identifier = findIdentifier(paramType, domainIndex);
        if (identifier.isPresent()) {
            TypeName unwrapped = toTypeName(identifier.get().wrappedType());
            ValidationKind validation = resolveValidation(identifier.get().wrappedType());
            return List.of(new DtoFieldSpec(
                    paramName, unwrapped, paramName, null, validation, ProjectionKind.IDENTITY_UNWRAP));
        }

        // 2. Check if ValueObject
        Optional<ValueObject> valueObject = findValueObject(paramType, domainIndex);
        if (valueObject.isPresent()) {
            return mapValueObject(paramName, valueObject.get(), config);
        }

        // 3. Direct types (String, enum, primitive, wrapper)
        return List.of(mapDirect(paramName, paramType));
    }

    /**
     * Maps a domain type field to DTO field spec(s) for a response DTO.
     *
     * <p>Returns multiple specs for multi-field VOs (flattening with prefix).
     * Returns empty list for AUDIT/TECHNICAL fields.
     *
     * <p>Accessor style depends on the parent type nature: record types use
     * record-style accessors ({@code fieldName()}), while class types use
     * JavaBean-style getters ({@code getFieldName()}).
     *
     * @param field           the domain type field
     * @param domainIndex     the domain index
     * @param config          the plugin configuration
     * @param parentStructure the structure of the type that declares this field
     * @return list of DTO field specs
     * @since 3.1.0
     */
    public static List<DtoFieldSpec> mapForResponse(
            Field field, DomainIndex domainIndex, RestConfig config, TypeStructure parentStructure) {
        // 1. Skip AUDIT and TECHNICAL fields
        if (field.roles().stream().anyMatch(r -> r == FieldRole.AUDIT || r == FieldRole.TECHNICAL)) {
            return List.of();
        }

        String fieldName = field.name();
        TypeRef fieldType = field.type();

        // 2. Identity field: unwrap via .value()
        if (field.isIdentity()) {
            return mapResponseIdentityField(fieldName, fieldType, domainIndex, parentStructure);
        }

        // 3. Aggregate reference: unwrap identifier via .value()
        if (field.hasRole(FieldRole.AGGREGATE_REFERENCE)) {
            return mapResponseAggregateReference(fieldName, fieldType, domainIndex, parentStructure);
        }

        // 4. Check if ValueObject
        Optional<ValueObject> vo = findValueObject(fieldType, domainIndex);
        if (vo.isPresent()) {
            return mapResponseValueObject(fieldName, fieldType, vo.get(), config, parentStructure);
        }

        // 5. Check if Identifier (non-identity, non-aggregate-ref)
        Optional<Identifier> id = findIdentifier(fieldType, domainIndex);
        if (id.isPresent()) {
            TypeName unwrapped = toTypeName(id.get().wrappedType());
            String accessor = accessorWithValue(fieldName, parentStructure);
            return List.of(new DtoFieldSpec(
                    fieldName, unwrapped, fieldName, accessor, ValidationKind.NONE, ProjectionKind.IDENTITY_UNWRAP));
        }

        // 6. Direct (String, enum, primitive, wrapper)
        TypeName javaType = toTypeName(fieldType);
        String accessor = accessorFor(fieldName, fieldType, parentStructure);
        return List.of(
                new DtoFieldSpec(fieldName, javaType, fieldName, accessor, ValidationKind.NONE, ProjectionKind.DIRECT));
    }

    /**
     * Generates the accessor expression for a field, taking the parent type nature into account.
     *
     * <p>Records use {@code fieldName()}, non-record classes use JavaBean-style getters.
     * For primitive {@code boolean} fields in classes, the prefix is {@code is} instead of
     * {@code get}, following JavaBean conventions.
     *
     * @param fieldName       the field name
     * @param fieldType       the type of the field
     * @param parentStructure the structure of the declaring type
     * @return the accessor expression string
     */
    private static String accessorFor(String fieldName, TypeRef fieldType, TypeStructure parentStructure) {
        if (parentStructure.isRecord()) {
            return fieldName + "()";
        }
        if ("boolean".equals(fieldType.qualifiedName())) {
            return "is" + NamingConventions.capitalize(fieldName) + "()";
        }
        return "get" + NamingConventions.capitalize(fieldName) + "()";
    }

    /**
     * Generates a chained accessor expression ending in {@code .value()}.
     *
     * <p>Records use {@code fieldName().value()}, non-record classes use
     * {@code getFieldName().value()}.
     *
     * @param fieldName       the field name
     * @param parentStructure the structure of the declaring type
     * @return the chained accessor expression string
     */
    private static String accessorWithValue(String fieldName, TypeStructure parentStructure) {
        if (parentStructure.isRecord()) {
            return fieldName + "().value()";
        }
        return "get" + NamingConventions.capitalize(fieldName) + "().value()";
    }

    private static List<DtoFieldSpec> mapResponseIdentityField(
            String fieldName, TypeRef fieldType, DomainIndex domainIndex, TypeStructure parentStructure) {
        Optional<Identifier> identifier = findIdentifier(fieldType, domainIndex);
        if (identifier.isPresent()) {
            TypeName unwrapped = toTypeName(identifier.get().wrappedType());
            String accessor = accessorWithValue(fieldName, parentStructure);
            return List.of(new DtoFieldSpec(
                    fieldName, unwrapped, fieldName, accessor, ValidationKind.NONE, ProjectionKind.IDENTITY_UNWRAP));
        }
        // Identity without Identifier type: direct mapping
        TypeName javaType = toTypeName(fieldType);
        String accessor = accessorFor(fieldName, fieldType, parentStructure);
        return List.of(
                new DtoFieldSpec(fieldName, javaType, fieldName, accessor, ValidationKind.NONE, ProjectionKind.DIRECT));
    }

    private static List<DtoFieldSpec> mapResponseAggregateReference(
            String fieldName, TypeRef fieldType, DomainIndex domainIndex, TypeStructure parentStructure) {
        Optional<Identifier> identifier = findIdentifier(fieldType, domainIndex);
        if (identifier.isPresent()) {
            TypeName unwrapped = toTypeName(identifier.get().wrappedType());
            String accessor = accessorWithValue(fieldName, parentStructure);
            return List.of(new DtoFieldSpec(
                    fieldName,
                    unwrapped,
                    fieldName,
                    accessor,
                    ValidationKind.NONE,
                    ProjectionKind.AGGREGATE_REFERENCE));
        }
        // Fallback: direct
        TypeName javaType = toTypeName(fieldType);
        String accessor = accessorFor(fieldName, fieldType, parentStructure);
        return List.of(
                new DtoFieldSpec(fieldName, javaType, fieldName, accessor, ValidationKind.NONE, ProjectionKind.DIRECT));
    }

    private static List<DtoFieldSpec> mapResponseValueObject(
            String fieldName, TypeRef fieldType, ValueObject vo, RestConfig config, TypeStructure parentStructure) {
        if (vo.isSingleValue()) {
            Field wrappedField = vo.wrappedField().orElseThrow();
            TypeName wrappedTypeName = toTypeName(wrappedField.type());
            String accessor = accessorWithValue(fieldName, parentStructure);
            return List.of(new DtoFieldSpec(
                    fieldName,
                    wrappedTypeName,
                    fieldName,
                    accessor,
                    ValidationKind.NONE,
                    ProjectionKind.IDENTITY_UNWRAP));
        }

        if (config.flattenValueObjects()) {
            List<DtoFieldSpec> fields = new ArrayList<>();
            for (Field f : vo.structure().fields()) {
                TypeName subType = toTypeName(f.type());
                String prefixedName = fieldName + NamingConventions.capitalize(f.name());
                // VO sub-fields are always records in hexaglue; only the top-level accessor
                // from the parent type needs to respect the parent's accessor style.
                String accessor = accessorFor(fieldName, fieldType, parentStructure) + "." + f.name() + "()";
                fields.add(new DtoFieldSpec(
                        prefixedName,
                        subType,
                        fieldName,
                        accessor,
                        ValidationKind.NONE,
                        ProjectionKind.VALUE_OBJECT_FLATTEN));
            }
            return fields;
        }

        // Non-flattened multi-field VO: NESTED_DTO
        TypeName voType = toTypeName(TypeRef.of(vo.id().qualifiedName()));
        String accessor = accessorFor(fieldName, fieldType, parentStructure);
        return List.of(new DtoFieldSpec(
                fieldName, voType, fieldName, accessor, ValidationKind.NONE, ProjectionKind.NESTED_DTO));
    }

    /**
     * Converts a TypeRef to a JavaPoet TypeName.
     *
     * @param typeRef the type reference
     * @return the corresponding JavaPoet TypeName
     */
    public static TypeName toTypeName(TypeRef typeRef) {
        return switch (typeRef.qualifiedName()) {
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "boolean" -> TypeName.BOOLEAN;
            case "double" -> TypeName.DOUBLE;
            case "float" -> TypeName.FLOAT;
            case "byte" -> TypeName.BYTE;
            case "short" -> TypeName.SHORT;
            case "char" -> TypeName.CHAR;
            case "void" -> TypeName.VOID;
            default -> {
                String qn = typeRef.qualifiedName();
                int lastDot = qn.lastIndexOf('.');
                if (lastDot < 0) {
                    yield ClassName.bestGuess(qn);
                }
                yield ClassName.get(qn.substring(0, lastDot), qn.substring(lastDot + 1));
            }
        };
    }

    /**
     * Converts a TypeRef to a JavaPoet ClassName (for non-primitive types).
     *
     * @param typeRef the type reference
     * @return the corresponding JavaPoet ClassName
     */
    static ClassName toClassName(TypeRef typeRef) {
        String qn = typeRef.qualifiedName();
        int lastDot = qn.lastIndexOf('.');
        if (lastDot < 0) {
            return ClassName.bestGuess(qn);
        }
        return ClassName.get(qn.substring(0, lastDot), qn.substring(lastDot + 1));
    }

    /**
     * Finds an Identifier in the domain index matching the given type ref.
     *
     * @param typeRef     the type reference to look up
     * @param domainIndex the domain index
     * @return the matching Identifier, or empty
     */
    public static Optional<Identifier> findIdentifier(TypeRef typeRef, DomainIndex domainIndex) {
        TypeId typeId = TypeId.of(typeRef.qualifiedName());
        return domainIndex.identifiers().filter(id -> id.id().equals(typeId)).findFirst();
    }

    /**
     * Finds a ValueObject in the domain index matching the given type ref.
     *
     * @param typeRef     the type reference to look up
     * @param domainIndex the domain index
     * @return the matching ValueObject, or empty
     */
    public static Optional<ValueObject> findValueObject(TypeRef typeRef, DomainIndex domainIndex) {
        TypeId typeId = TypeId.of(typeRef.qualifiedName());
        return domainIndex.valueObjects().filter(vo -> vo.id().equals(typeId)).findFirst();
    }

    private static List<DtoFieldSpec> mapValueObject(String paramName, ValueObject vo, RestConfig config) {
        if (vo.isSingleValue()) {
            // Single-field VO: unwrap like identifier
            Field wrappedField = vo.wrappedField().orElseThrow();
            TypeName fieldType = toTypeName(wrappedField.type());
            ValidationKind validation = resolveValidation(wrappedField.type());
            return List.of(new DtoFieldSpec(
                    paramName, fieldType, paramName, null, validation, ProjectionKind.IDENTITY_UNWRAP));
        }

        if (config.flattenValueObjects()) {
            // Multi-field VO: flatten
            List<DtoFieldSpec> fields = new ArrayList<>();
            for (Field f : vo.structure().fields()) {
                TypeName fieldType = toTypeName(f.type());
                ValidationKind validation = resolveValidation(f.type());
                fields.add(new DtoFieldSpec(
                        f.name(), fieldType, paramName, null, validation, ProjectionKind.VALUE_OBJECT_FLATTEN));
            }
            return fields;
        }

        // Non-flattened multi-field VO: treat as direct reference
        TypeName voType = toTypeName(TypeRef.of(vo.id().qualifiedName()));
        return List.of(
                new DtoFieldSpec(paramName, voType, paramName, null, ValidationKind.NOT_NULL, ProjectionKind.DIRECT));
    }

    private static DtoFieldSpec mapDirect(String paramName, TypeRef paramType) {
        TypeName javaType = toTypeName(paramType);
        ValidationKind validation = resolveValidation(paramType);
        return new DtoFieldSpec(paramName, javaType, paramName, null, validation, ProjectionKind.DIRECT);
    }

    private static ValidationKind resolveValidation(TypeRef typeRef) {
        String qualifiedName = typeRef.qualifiedName();
        if (PRIMITIVES.contains(qualifiedName)) {
            return ValidationKind.NONE;
        }
        if ("java.lang.String".equals(qualifiedName)) {
            return ValidationKind.NOT_BLANK;
        }
        return ValidationKind.NOT_NULL;
    }
}
