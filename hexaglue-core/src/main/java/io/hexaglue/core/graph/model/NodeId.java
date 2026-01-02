package io.hexaglue.core.graph.model;

import java.util.Objects;

/**
 * Stable identifier for a node in the application graph.
 *
 * <p>Format: {@code kind:qualified-name} where kind is one of:
 * <ul>
 *   <li>{@code type:} - for types (classes, interfaces, records, enums)</li>
 *   <li>{@code field:} - for fields (format: type-fqn#field-name)</li>
 *   <li>{@code method:} - for methods (format: type-fqn#method-name(param-types))</li>
 *   <li>{@code ctor:} - for constructors (format: type-fqn#&lt;init&gt;(param-types))</li>
 * </ul>
 *
 * @param value the unique identifier string
 */
public record NodeId(String value) implements Comparable<NodeId> {

    public NodeId {
        Objects.requireNonNull(value, "NodeId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("NodeId value cannot be blank");
        }
    }

    /**
     * Creates a NodeId for a type.
     *
     * @param qualifiedName the fully qualified name of the type
     * @return the node id
     */
    public static NodeId type(String qualifiedName) {
        return new NodeId("type:" + qualifiedName);
    }

    /**
     * Creates a NodeId for a field.
     *
     * @param typeQualifiedName the fully qualified name of the declaring type
     * @param fieldName the field name
     * @return the node id
     */
    public static NodeId field(String typeQualifiedName, String fieldName) {
        return new NodeId("field:" + typeQualifiedName + "#" + fieldName);
    }

    /**
     * Creates a NodeId for a method.
     *
     * @param typeQualifiedName the fully qualified name of the declaring type
     * @param methodName the method name
     * @param parameterTypes the parameter types as a comma-separated string
     * @return the node id
     */
    public static NodeId method(String typeQualifiedName, String methodName, String parameterTypes) {
        return new NodeId("method:" + typeQualifiedName + "#" + methodName + "(" + parameterTypes + ")");
    }

    /**
     * Creates a NodeId for a constructor.
     *
     * @param typeQualifiedName the fully qualified name of the declaring type
     * @param parameterTypes the parameter types as a comma-separated string
     * @return the node id
     */
    public static NodeId constructor(String typeQualifiedName, String parameterTypes) {
        return new NodeId("ctor:" + typeQualifiedName + "#<init>(" + parameterTypes + ")");
    }

    /**
     * Returns the kind prefix (type, field, method, ctor).
     */
    public String kind() {
        int colonIndex = value.indexOf(':');
        return colonIndex > 0 ? value.substring(0, colonIndex) : "";
    }

    /**
     * Returns true if this is a type node id.
     */
    public boolean isType() {
        return value.startsWith("type:");
    }

    /**
     * Returns true if this is a field node id.
     */
    public boolean isField() {
        return value.startsWith("field:");
    }

    /**
     * Returns true if this is a method node id.
     */
    public boolean isMethod() {
        return value.startsWith("method:");
    }

    /**
     * Returns true if this is a constructor node id.
     */
    public boolean isConstructor() {
        return value.startsWith("ctor:");
    }

    /**
     * Returns true if this is a member node id (field, method, or constructor).
     */
    public boolean isMember() {
        return isField() || isMethod() || isConstructor();
    }

    @Override
    public int compareTo(NodeId other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
