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

package io.hexaglue.core.graph.model.edges;

import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.EdgeProof;
import io.hexaglue.core.graph.model.NodeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a field access edge with read/write classification.
 *
 * <p>Field access edges capture how methods interact with fields, which is useful for:
 * <ul>
 *   <li>Detecting encapsulation violations (direct field access vs getters/setters)</li>
 *   <li>Understanding state mutation patterns</li>
 *   <li>Analyzing immutability and side effects</li>
 *   <li>Identifying data flow dependencies</li>
 * </ul>
 *
 * <p>Access types:
 * <ul>
 *   <li><b>READ</b> - field is read (value = field.value)</li>
 *   <li><b>WRITE</b> - field is written (field.value = newValue)</li>
 *   <li><b>READ_WRITE</b> - field is both read and written (field.value++)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * FieldAccessEdge edge = FieldAccessEdge.read(
 *     NodeId.method("com.example.Order", "getTotal", ""),
 *     NodeId.field("com.example.Order", "items")
 * );
 *
 * // edge.accessType() == AccessType.READ
 * // edge.isReadAccess() == true
 * // edge.isWriteAccess() == false
 * }</pre>
 *
 * @param from the source node (accessor method)
 * @param to the target node (field being accessed)
 * @param accessType the type of access (READ, WRITE, READ_WRITE)
 * @since 3.0.0
 */
public record FieldAccessEdge(NodeId from, NodeId to, AccessType accessType) implements TypedEdge {

    /**
     * Classification of field access operations.
     */
    public enum AccessType {
        /** Field is read only. */
        READ,

        /** Field is written only. */
        WRITE,

        /** Field is both read and written (e.g., field++, field += value). */
        READ_WRITE;

        /**
         * Returns true if this access type includes reading.
         *
         * @return true for READ or READ_WRITE
         */
        public boolean isRead() {
            return this == READ || this == READ_WRITE;
        }

        /**
         * Returns true if this access type includes writing.
         *
         * @return true for WRITE or READ_WRITE
         */
        public boolean isWrite() {
            return this == WRITE || this == READ_WRITE;
        }
    }

    /**
     * EdgeKind for field access edges - this is a derived edge.
     */
    private static final EdgeKind FIELD_ACCESS_KIND = EdgeKind.REFERENCES;

    public FieldAccessEdge {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(accessType, "accessType cannot be null");
    }

    /**
     * Creates a READ field access edge.
     *
     * @param from the accessor method
     * @param to the field being read
     * @return the field access edge
     */
    public static FieldAccessEdge read(NodeId from, NodeId to) {
        return new FieldAccessEdge(from, to, AccessType.READ);
    }

    /**
     * Creates a WRITE field access edge.
     *
     * @param from the accessor method
     * @param to the field being written
     * @return the field access edge
     */
    public static FieldAccessEdge write(NodeId from, NodeId to) {
        return new FieldAccessEdge(from, to, AccessType.WRITE);
    }

    /**
     * Creates a READ_WRITE field access edge.
     *
     * @param from the accessor method
     * @param to the field being read and written
     * @return the field access edge
     */
    public static FieldAccessEdge readWrite(NodeId from, NodeId to) {
        return new FieldAccessEdge(from, to, AccessType.READ_WRITE);
    }

    @Override
    public EdgeKind kind() {
        return FIELD_ACCESS_KIND;
    }

    @Override
    public Map<String, Object> metadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("edgeType", "FIELD_ACCESS");
        meta.put("accessType", accessType.name());
        meta.put("isRead", accessType.isRead());
        meta.put("isWrite", accessType.isWrite());
        return Map.copyOf(meta);
    }

    @Override
    public Edge toEdge() {
        String via = String.format("field-access(%s, type=%s)", to.value(), accessType.name());
        EdgeProof proof = new EdgeProof(from, via, "FIELD_ACCESS");
        return Edge.derived(from, to, kind(), proof);
    }

    /**
     * Returns true if this access includes reading the field.
     *
     * @return true for READ or READ_WRITE
     */
    public boolean isReadAccess() {
        return accessType.isRead();
    }

    /**
     * Returns true if this access includes writing to the field.
     *
     * @return true for WRITE or READ_WRITE
     */
    public boolean isWriteAccess() {
        return accessType.isWrite();
    }

    /**
     * Returns true if this is a pure read access (no write).
     *
     * @return true for READ only
     */
    public boolean isReadOnly() {
        return accessType == AccessType.READ;
    }

    /**
     * Returns true if this is a pure write access (no read).
     *
     * @return true for WRITE only
     */
    public boolean isWriteOnly() {
        return accessType == AccessType.WRITE;
    }

    /**
     * Returns true if this is a read-modify-write access.
     *
     * @return true for READ_WRITE
     */
    public boolean isReadModifyWrite() {
        return accessType == AccessType.READ_WRITE;
    }
}
