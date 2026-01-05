/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Graph model data structures - nodes, edges, and identifiers.
 *
 * <h2>Node Hierarchy</h2>
 * <pre>
 *                 Node (abstract)
 *                   │
 *      ┌────────────┴────────────┐
 *      │                         │
 *  TypeNode                 MemberNode
 *                                │
 *                 ┌──────────────┼──────────────┐
 *                 │              │              │
 *            FieldNode     MethodNode   ConstructorNode
 * </pre>
 *
 * <h2>Edge Types</h2>
 * <ul>
 *   <li><b>RAW</b> - Extracted directly from AST (EXTENDS, IMPLEMENTS, FIELD_TYPE, etc.)</li>
 *   <li><b>DERIVED</b> - Computed from RAW edges (USES_IN_SIGNATURE, REFERENCES, etc.)</li>
 * </ul>
 *
 * <h2>NodeId Format</h2>
 * <ul>
 *   <li>{@code type:com.example.Order} - Type nodes</li>
 *   <li>{@code field:com.example.Order#id} - Field nodes</li>
 *   <li>{@code method:com.example.Order#getId()} - Method nodes</li>
 * </ul>
 *
 * @see io.hexaglue.core.graph.model.TypeNode Type representation
 * @see io.hexaglue.core.graph.model.Edge Relationship representation
 * @see io.hexaglue.core.graph.model.NodeId Unique node identifier
 */
package io.hexaglue.core.graph.model;
