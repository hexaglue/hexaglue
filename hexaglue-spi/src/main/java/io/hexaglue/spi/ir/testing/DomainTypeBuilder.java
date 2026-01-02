package io.hexaglue.spi.ir.testing;

import io.hexaglue.spi.ir.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fluent builder for creating {@link DomainType} instances in tests.
 *
 * <p>Example usage:
 * <pre>{@code
 * DomainType order = DomainTypeBuilder.aggregateRoot("com.example.Order")
 *     .withIdentity("id", "com.example.OrderId", "java.util.UUID")
 *     .withProperty("status", "com.example.OrderStatus")
 *     .withCollectionProperty("items", "com.example.LineItem")
 *     .build();
 * }</pre>
 */
public final class DomainTypeBuilder {

    private String qualifiedName;
    private String simpleName;
    private String packageName;
    private DomainKind kind = DomainKind.ENTITY;
    private ConfidenceLevel confidence = ConfidenceLevel.HIGH;
    private JavaConstruct construct = JavaConstruct.CLASS;
    private Identity identity;
    private final List<DomainProperty> properties = new ArrayList<>();
    private final List<DomainRelation> relations = new ArrayList<>();
    private final List<String> annotations = new ArrayList<>();
    private SourceRef sourceRef;

    public DomainTypeBuilder() {}

    // =========================================================================
    // Factory methods for common DDD types
    // =========================================================================

    /**
     * Creates a builder for an aggregate root.
     */
    public static DomainTypeBuilder aggregateRoot(String qualifiedName) {
        return new DomainTypeBuilder().qualifiedName(qualifiedName).kind(DomainKind.AGGREGATE_ROOT);
    }

    /**
     * Creates a builder for an entity.
     */
    public static DomainTypeBuilder entity(String qualifiedName) {
        return new DomainTypeBuilder().qualifiedName(qualifiedName).kind(DomainKind.ENTITY);
    }

    /**
     * Creates a builder for a value object.
     */
    public static DomainTypeBuilder valueObject(String qualifiedName) {
        return new DomainTypeBuilder()
                .qualifiedName(qualifiedName)
                .kind(DomainKind.VALUE_OBJECT)
                .construct(JavaConstruct.RECORD);
    }

    /**
     * Creates a builder for an identifier type.
     */
    public static DomainTypeBuilder identifier(String qualifiedName) {
        return new DomainTypeBuilder()
                .qualifiedName(qualifiedName)
                .kind(DomainKind.IDENTIFIER)
                .construct(JavaConstruct.RECORD);
    }

    /**
     * Creates a builder for a domain event.
     */
    public static DomainTypeBuilder domainEvent(String qualifiedName) {
        return new DomainTypeBuilder()
                .qualifiedName(qualifiedName)
                .kind(DomainKind.DOMAIN_EVENT)
                .construct(JavaConstruct.RECORD);
    }

    /**
     * Creates a builder for a domain service.
     */
    public static DomainTypeBuilder domainService(String qualifiedName) {
        return new DomainTypeBuilder().qualifiedName(qualifiedName).kind(DomainKind.DOMAIN_SERVICE);
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Sets the qualified name and derives simple name and package.
     */
    public DomainTypeBuilder qualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
        int lastDot = qualifiedName.lastIndexOf('.');
        this.simpleName = lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        this.packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
        return this;
    }

    /**
     * Sets the domain kind.
     */
    public DomainTypeBuilder kind(DomainKind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Sets the confidence level.
     */
    public DomainTypeBuilder confidence(ConfidenceLevel confidence) {
        this.confidence = confidence;
        return this;
    }

    /**
     * Sets the Java construct type.
     */
    public DomainTypeBuilder construct(JavaConstruct construct) {
        this.construct = construct;
        return this;
    }

    /**
     * Sets this as a record type.
     */
    public DomainTypeBuilder asRecord() {
        this.construct = JavaConstruct.RECORD;
        return this;
    }

    /**
     * Sets this as an enum type.
     */
    public DomainTypeBuilder asEnum() {
        this.construct = JavaConstruct.ENUM;
        return this;
    }

    // =========================================================================
    // Identity
    // =========================================================================

    /**
     * Adds identity with a wrapped type.
     *
     * @param fieldName the identity field name
     * @param wrapperType the wrapper type (e.g., "com.example.OrderId")
     * @param unwrappedType the underlying type (e.g., "java.util.UUID")
     */
    public DomainTypeBuilder withIdentity(String fieldName, String wrapperType, String unwrappedType) {
        this.identity = new Identity(
                fieldName,
                TypeRef.of(wrapperType),
                TypeRef.of(unwrappedType),
                IdentityStrategy.ASSIGNED,
                IdentityWrapperKind.RECORD);
        return this;
    }

    /**
     * Adds identity with an unwrapped type.
     *
     * @param fieldName the identity field name
     * @param type the identity type (e.g., "java.util.UUID")
     */
    public DomainTypeBuilder withUnwrappedIdentity(String fieldName, String type) {
        this.identity = new Identity(
                fieldName, TypeRef.of(type), TypeRef.of(type), IdentityStrategy.ASSIGNED, IdentityWrapperKind.NONE);
        return this;
    }

    /**
     * Adds identity with UUID type and specified generation strategy.
     */
    public DomainTypeBuilder withUuidIdentity(String fieldName, IdentityStrategy strategy) {
        this.identity = new Identity(
                fieldName,
                TypeRef.of("java.util.UUID"),
                TypeRef.of("java.util.UUID"),
                strategy,
                IdentityWrapperKind.NONE);
        return this;
    }

    /**
     * Adds a Long identity field.
     */
    public DomainTypeBuilder withLongIdentity(String fieldName, IdentityStrategy strategy) {
        this.identity = new Identity(
                fieldName,
                TypeRef.of("java.lang.Long"),
                TypeRef.of("java.lang.Long"),
                strategy,
                IdentityWrapperKind.NONE);
        return this;
    }

    // =========================================================================
    // Properties
    // =========================================================================

    /**
     * Adds a simple property.
     */
    public DomainTypeBuilder withProperty(String name, String typeFqn) {
        properties.add(new DomainProperty(name, TypeRef.of(typeFqn), Cardinality.SINGLE, Nullability.NON_NULL, false));
        return this;
    }

    /**
     * Adds a nullable property.
     */
    public DomainTypeBuilder withNullableProperty(String name, String typeFqn) {
        properties.add(new DomainProperty(name, TypeRef.of(typeFqn), Cardinality.SINGLE, Nullability.NULLABLE, false));
        return this;
    }

    /**
     * Adds an optional property.
     */
    public DomainTypeBuilder withOptionalProperty(String name, String elementTypeFqn) {
        TypeRef type = TypeRef.parameterized("java.util.Optional", TypeRef.of(elementTypeFqn));
        properties.add(new DomainProperty(name, type, Cardinality.OPTIONAL, Nullability.NON_NULL, false));
        return this;
    }

    /**
     * Adds a collection property.
     */
    public DomainTypeBuilder withCollectionProperty(String name, String elementTypeFqn) {
        TypeRef type = TypeRef.parameterized("java.util.List", TypeRef.of(elementTypeFqn));
        properties.add(new DomainProperty(name, type, Cardinality.COLLECTION, Nullability.NON_NULL, false));
        return this;
    }

    /**
     * Adds an embedded value object property.
     */
    public DomainTypeBuilder withEmbeddedProperty(String name, String typeFqn) {
        RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.EMBEDDED, typeFqn);
        properties.add(new DomainProperty(
                name, TypeRef.of(typeFqn), Cardinality.SINGLE, Nullability.NON_NULL, false, true, relationInfo));
        return this;
    }

    /**
     * Adds an embedded collection property.
     */
    public DomainTypeBuilder withEmbeddedCollectionProperty(String name, String elementTypeFqn) {
        TypeRef type = TypeRef.parameterized("java.util.List", TypeRef.of(elementTypeFqn));
        RelationInfo relationInfo = RelationInfo.unidirectional(RelationKind.ELEMENT_COLLECTION, elementTypeFqn);
        properties.add(new DomainProperty(
                name, type, Cardinality.COLLECTION, Nullability.NON_NULL, false, true, relationInfo));
        return this;
    }

    // =========================================================================
    // Relations
    // =========================================================================

    /**
     * Adds a one-to-many relation.
     */
    public DomainTypeBuilder withOneToManyRelation(String fieldName, String targetTypeFqn) {
        relations.add(DomainRelation.oneToMany(fieldName, targetTypeFqn, DomainKind.ENTITY));
        return this;
    }

    /**
     * Adds a many-to-one relation.
     */
    public DomainTypeBuilder withManyToOneRelation(String fieldName, String targetTypeFqn) {
        relations.add(DomainRelation.manyToOne(fieldName, targetTypeFqn));
        return this;
    }

    /**
     * Adds an embedded relation.
     */
    public DomainTypeBuilder withEmbeddedRelation(String fieldName, String targetTypeFqn) {
        relations.add(DomainRelation.embedded(fieldName, targetTypeFqn));
        return this;
    }

    // =========================================================================
    // Annotations
    // =========================================================================

    /**
     * Adds an annotation.
     */
    public DomainTypeBuilder withAnnotation(String annotationFqn) {
        annotations.add(annotationFqn);
        return this;
    }

    /**
     * Adds a jMolecules @AggregateRoot annotation.
     */
    public DomainTypeBuilder withJMoleculesAggregateRoot() {
        return withAnnotation("org.jmolecules.ddd.annotation.AggregateRoot");
    }

    /**
     * Adds a jMolecules @Entity annotation.
     */
    public DomainTypeBuilder withJMoleculesEntity() {
        return withAnnotation("org.jmolecules.ddd.annotation.Entity");
    }

    /**
     * Adds a jMolecules @ValueObject annotation.
     */
    public DomainTypeBuilder withJMoleculesValueObject() {
        return withAnnotation("org.jmolecules.ddd.annotation.ValueObject");
    }

    // =========================================================================
    // Source Reference
    // =========================================================================

    /**
     * Sets the source reference.
     */
    public DomainTypeBuilder withSourceRef(String filePath, int line) {
        this.sourceRef = SourceRef.ofLine(filePath, line);
        return this;
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Builds the DomainType.
     */
    public DomainType build() {
        if (qualifiedName == null) {
            throw new IllegalStateException("qualifiedName is required");
        }

        SourceRef ref = sourceRef != null ? sourceRef : SourceRef.ofLine(simpleName + ".java", 1);

        return new DomainType(
                qualifiedName,
                simpleName,
                packageName,
                kind,
                confidence,
                construct,
                Optional.ofNullable(identity),
                List.copyOf(properties),
                List.copyOf(relations),
                List.copyOf(annotations),
                ref);
    }
}
