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

package io.hexaglue.core.enrichment;

import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.enrichment.SemanticLabel;
import java.util.*;

/**
 * Detects behavioral patterns in domain methods.
 *
 * <p>This enricher analyzes method signatures and naming conventions to identify:
 * <ul>
 *   <li><b>Factory methods</b>: Static methods returning the declaring type</li>
 *   <li><b>Invariant validators</b>: Methods with validation naming patterns</li>
 *   <li><b>Collection managers</b>: Methods managing entity collections</li>
 *   <li><b>Getters/Setters</b>: Simple accessor/mutator methods</li>
 *   <li><b>Lifecycle methods</b>: State transition methods</li>
 * </ul>
 *
 * <p>This is a built-in enricher that runs during the enrichment phase.
 */
public class BehavioralPatternEnricher {

    private static final Set<String> VALIDATION_PREFIXES = Set.of("validate", "check", "ensure", "verify", "assert");
    private static final Set<String> COLLECTION_MANAGER_PREFIXES = Set.of("add", "remove", "delete", "clear");
    private static final Set<String> LIFECYCLE_VERBS =
            Set.of("activate", "deactivate", "enable", "disable", "cancel", "complete", "submit", "approve", "reject");

    private final ApplicationGraph graph;

    public BehavioralPatternEnricher(ApplicationGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
    }

    /**
     * Enriches methods with behavioral pattern labels.
     *
     * @return map of method identifiers to semantic labels
     */
    public Map<String, Set<SemanticLabel>> enrichMethods() {
        Map<String, Set<SemanticLabel>> labels = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            List<MethodNode> methods = graph.methodsOf(type);

            for (MethodNode method : methods) {
                Set<SemanticLabel> methodLabels = analyzeMethod(type, method);
                if (!methodLabels.isEmpty()) {
                    labels.put(method.qualifiedName(), methodLabels);
                }
            }
        }

        return labels;
    }

    /**
     * Enriches types with behavioral pattern labels.
     *
     * @return map of type identifiers to semantic labels
     */
    public Map<String, Set<SemanticLabel>> enrichTypes() {
        Map<String, Set<SemanticLabel>> labels = new HashMap<>();

        for (TypeNode type : graph.typeNodes()) {
            Set<SemanticLabel> typeLabels = analyzeType(type);
            if (!typeLabels.isEmpty()) {
                labels.put(type.qualifiedName(), typeLabels);
            }
        }

        return labels;
    }

    private Set<SemanticLabel> analyzeMethod(TypeNode type, MethodNode method) {
        Set<SemanticLabel> labels = new HashSet<>();

        // Factory method detection
        if (isFactoryMethod(type, method)) {
            labels.add(SemanticLabel.FACTORY_METHOD);
        }

        // Invariant validator detection
        if (isInvariantValidator(method)) {
            labels.add(SemanticLabel.INVARIANT_VALIDATOR);
        }

        // Collection manager detection
        if (isCollectionManager(method)) {
            labels.add(SemanticLabel.COLLECTION_MANAGER);
        }

        // Lifecycle method detection
        if (isLifecycleMethod(method)) {
            labels.add(SemanticLabel.LIFECYCLE_METHOD);
        }

        // Getter/Setter detection
        if (method.looksLikeGetter()) {
            labels.add(SemanticLabel.GETTER);
        } else if (method.looksLikeSetter()) {
            labels.add(SemanticLabel.SETTER);
        }

        // Command/Event handler detection
        if (isCommandHandler(method)) {
            labels.add(SemanticLabel.COMMAND_HANDLER);
        }

        if (isEventHandler(method)) {
            labels.add(SemanticLabel.EVENT_HANDLER);
        }

        return labels;
    }

    private Set<SemanticLabel> analyzeType(TypeNode type) {
        Set<SemanticLabel> labels = new HashSet<>();

        // Immutable type detection
        if (isImmutableType(type)) {
            labels.add(SemanticLabel.IMMUTABLE_TYPE);
        }

        // Side-effect free detection (records are typically side-effect free)
        if (isSideEffectFree(type)) {
            labels.add(SemanticLabel.SIDE_EFFECT_FREE);
        }

        // Event publisher detection
        if (isEventPublisher(type)) {
            labels.add(SemanticLabel.EVENT_PUBLISHER);
        }

        return labels;
    }

    // === Factory method detection ===

    /**
     * Detects if a method is a factory method.
     *
     * <p>Factory methods are static methods that return an instance of the declaring type.
     */
    private boolean isFactoryMethod(TypeNode type, MethodNode method) {
        if (!method.modifiers().contains(JavaModifier.STATIC)) {
            return false;
        }

        // Check if return type matches declaring type
        String returnTypeName = method.returnType().rawQualifiedName();
        return returnTypeName.equals(type.qualifiedName());
    }

    // === Invariant validator detection ===

    /**
     * Detects if a method is an invariant validator.
     *
     * <p>Validators typically:
     * <ul>
     *   <li>Start with validation verbs (validate, check, ensure, verify)</li>
     *   <li>May throw exceptions</li>
     *   <li>Return void or boolean</li>
     * </ul>
     */
    private boolean isInvariantValidator(MethodNode method) {
        String name = method.simpleName().toLowerCase();

        // Check naming convention
        boolean hasValidationPrefix = VALIDATION_PREFIXES.stream().anyMatch(name::startsWith);
        if (!hasValidationPrefix) {
            return false;
        }

        // Validators typically return void or boolean
        String returnType = method.returnType().rawQualifiedName();
        return returnType.equals("void") || returnType.equals("boolean");
    }

    // === Collection manager detection ===

    /**
     * Detects if a method manages entity collections.
     *
     * <p>Collection managers:
     * <ul>
     *   <li>Start with add/remove/delete/clear</li>
     *   <li>Have parameters suggesting entity manipulation</li>
     *   <li>Typically return void</li>
     * </ul>
     */
    private boolean isCollectionManager(MethodNode method) {
        String name = method.simpleName().toLowerCase();

        boolean hasCollectionPrefix = COLLECTION_MANAGER_PREFIXES.stream().anyMatch(name::startsWith);
        if (!hasCollectionPrefix) {
            return false;
        }

        // Usually has at least one parameter (the item to add/remove)
        return method.parameterCount() >= 1;
    }

    // === Lifecycle method detection ===

    /**
     * Detects lifecycle state transition methods.
     *
     * <p>Lifecycle methods change entity state (activate, cancel, complete, etc.)
     */
    private boolean isLifecycleMethod(MethodNode method) {
        String name = method.simpleName().toLowerCase();
        return LIFECYCLE_VERBS.contains(name);
    }

    // === Command/Event handler detection ===

    /**
     * Detects command handler methods (CQRS pattern).
     */
    private boolean isCommandHandler(MethodNode method) {
        // Check for @CommandHandler annotation or handle* naming
        boolean hasCommandAnnotation =
                method.annotations().stream().anyMatch(a -> a.qualifiedName().endsWith("CommandHandler"));

        boolean hasHandlePrefix = method.simpleName().toLowerCase().startsWith("handle");

        return hasCommandAnnotation || hasHandlePrefix;
    }

    /**
     * Detects event handler methods.
     */
    private boolean isEventHandler(MethodNode method) {
        // Check for event handler annotations
        return method.annotations().stream()
                .anyMatch(a -> a.qualifiedName().endsWith("EventHandler")
                        || a.qualifiedName().endsWith("EventListener"));
    }

    // === Type-level detection ===

    /**
     * Detects if a type is immutable.
     *
     * <p>A type is considered immutable if all its fields are final.
     */
    private boolean isImmutableType(TypeNode type) {
        var fields = graph.fieldsOf(type);
        if (fields.isEmpty()) {
            return false;
        }

        // All fields must be final
        return fields.stream().allMatch(f -> f.modifiers().contains(JavaModifier.FINAL));
    }

    /**
     * Detects if a type is side-effect free.
     *
     * <p>Records and types with only final fields are considered side-effect free.
     */
    private boolean isSideEffectFree(TypeNode type) {
        // Records are typically side-effect free
        if (type.isRecord()) {
            return true;
        }

        // Types with all final fields are side-effect free
        return isImmutableType(type);
    }

    /**
     * Detects if a type publishes domain events.
     *
     * <p>Event publishers typically have methods that return or accept event types.
     */
    private boolean isEventPublisher(TypeNode type) {
        var methods = graph.methodsOf(type);

        // Check for methods returning event types or accepting event listeners
        return methods.stream().anyMatch(m -> {
            String returnType = m.returnType().rawQualifiedName();
            return returnType.contains("Event") || returnType.contains("DomainEvent");
        });
    }
}
