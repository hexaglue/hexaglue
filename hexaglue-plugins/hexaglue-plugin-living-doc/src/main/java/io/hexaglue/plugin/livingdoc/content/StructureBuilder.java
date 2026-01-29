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

package io.hexaglue.plugin.livingdoc.content;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.TypeRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Builds a package tree representation from the architectural model.
 *
 * <p>Produces a text-based tree showing the project structure with each type
 * annotated by its architectural kind. The output is designed to be embedded
 * in a Markdown code block.
 *
 * <p>Results are computed and cached at construction time (immutable).
 *
 * @since 5.0.0
 */
public final class StructureBuilder {

    private final String packageTree;

    /**
     * Creates a structure builder that analyzes all types in the model.
     *
     * @param model the architectural model
     * @throws IllegalStateException if the model does not contain a TypeRegistry
     * @since 5.0.0
     */
    public StructureBuilder(ArchitecturalModel model) {
        Objects.requireNonNull(model, "model must not be null");

        TypeRegistry registry = model.typeRegistry()
                .orElseThrow(() -> new IllegalStateException("TypeRegistry required for structure building"));

        this.packageTree = buildTree(registry);
    }

    /**
     * Returns the rendered package tree.
     *
     * <p>The tree is indented using ASCII-art connectors and each type
     * is annotated with its architectural kind as a comment.
     *
     * @return the package tree as plain text, or an empty string if the model has no types
     * @since 5.0.0
     */
    public String renderPackageTree() {
        return packageTree;
    }

    /**
     * Builds the tree from the registry.
     */
    private static String buildTree(TypeRegistry registry) {
        // Collect types grouped by package, sorted
        Map<String, List<TypeInfo>> byPackage = registry.all()
                .map(type -> new TypeInfo(type.simpleName(), type.packageName(), type.kind()))
                .collect(Collectors.groupingBy(TypeInfo::packageName, TreeMap::new, Collectors.toList()));

        if (byPackage.isEmpty()) {
            return "";
        }

        // Sort types within each package
        byPackage.values().forEach(list -> list.sort(Comparator.comparing(TypeInfo::simpleName)));

        // Build the package tree as a node structure
        PackageNode root = new PackageNode("");
        for (Map.Entry<String, List<TypeInfo>> entry : byPackage.entrySet()) {
            String[] segments = entry.getKey().split("\\.");
            PackageNode current = root;
            for (String segment : segments) {
                current = current.getOrCreateChild(segment);
            }
            current.types.addAll(entry.getValue());
        }

        // Render from the root, skipping the empty root node
        StringBuilder sb = new StringBuilder();
        List<PackageNode> topLevel = new ArrayList<>(root.children.values());
        renderChildren(sb, topLevel, "");
        return sb.toString();
    }

    /**
     * Renders a list of children with tree connectors.
     */
    private static void renderChildren(StringBuilder sb, List<PackageNode> children, String prefix) {
        for (int i = 0; i < children.size(); i++) {
            PackageNode child = children.get(i);
            boolean isLast = (i == children.size() - 1);
            String connector = isLast ? "+-- " : "+-- ";
            String childPrefix = isLast ? "    " : "|   ";

            sb.append(prefix).append(connector).append(child.name).append("/\n");

            // Render types in this package
            String typePrefix = prefix + childPrefix;
            List<TypeInfo> types = child.types;
            List<PackageNode> subChildren = new ArrayList<>(child.children.values());

            List<Object> items = new ArrayList<>();
            items.addAll(types);
            items.addAll(subChildren);

            for (int j = 0; j < items.size(); j++) {
                Object item = items.get(j);
                boolean isLastItem = (j == items.size() - 1);
                String itemConnector = isLastItem ? "+-- " : "+-- ";
                String itemPrefix = isLastItem ? "    " : "|   ";

                if (item instanceof TypeInfo typeInfo) {
                    sb.append(typePrefix)
                            .append(itemConnector)
                            .append(typeInfo.simpleName())
                            .append(".java")
                            .append("    # ")
                            .append(GlossaryBuilder.formatKindLabel(typeInfo.kind()))
                            .append("\n");
                } else if (item instanceof PackageNode packageNode) {
                    // Render subpackage recursively
                    sb.append(typePrefix)
                            .append(itemConnector)
                            .append(packageNode.name)
                            .append("/\n");
                    String subPrefix = typePrefix + itemPrefix;
                    List<Object> subItems = new ArrayList<>();
                    subItems.addAll(packageNode.types);
                    subItems.addAll(new ArrayList<>(packageNode.children.values()));
                    renderSubItems(sb, subItems, subPrefix, packageNode);
                }
            }
        }
    }

    /**
     * Renders items within a subpackage.
     */
    private static void renderSubItems(StringBuilder sb, List<Object> items, String prefix, PackageNode parentNode) {
        // Instead of duplicating logic, render the node's contents
        List<TypeInfo> types = parentNode.types;
        List<PackageNode> subChildren = new ArrayList<>(parentNode.children.values());

        for (int i = 0; i < types.size(); i++) {
            TypeInfo typeInfo = types.get(i);
            boolean isLast = (i == types.size() - 1) && subChildren.isEmpty();
            String connector = isLast ? "+-- " : "+-- ";
            sb.append(prefix)
                    .append(connector)
                    .append(typeInfo.simpleName())
                    .append(".java")
                    .append("    # ")
                    .append(GlossaryBuilder.formatKindLabel(typeInfo.kind()))
                    .append("\n");
        }

        renderChildren(sb, subChildren, prefix);
    }

    /**
     * A node in the package tree.
     */
    private static final class PackageNode {
        final String name;
        final TreeMap<String, PackageNode> children = new TreeMap<>();
        final List<TypeInfo> types = new ArrayList<>();

        PackageNode(String name) {
            this.name = name;
        }

        PackageNode getOrCreateChild(String childName) {
            return children.computeIfAbsent(childName, PackageNode::new);
        }
    }

    /**
     * Lightweight representation of a type for tree building.
     */
    private record TypeInfo(String simpleName, String packageName, ArchKind kind) {}
}
