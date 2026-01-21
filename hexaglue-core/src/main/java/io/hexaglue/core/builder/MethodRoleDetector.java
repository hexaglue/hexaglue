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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Detects the semantic roles of methods during ArchType construction.
 *
 * <p>This detector analyzes method signatures, annotations, and naming patterns
 * to determine what role(s) a method plays within the domain model. A method may have
 * multiple roles.</p>
 *
 * <h2>Detected Roles</h2>
 * <ul>
 *   <li>{@link MethodRole#GETTER} - Property accessor (getX/isX, no params, non-void return)</li>
 *   <li>{@link MethodRole#SETTER} - Property mutator (setX, 1 param, void return)</li>
 *   <li>{@link MethodRole#FACTORY} - Static factory method (static, returns own type)</li>
 *   <li>{@link MethodRole#OBJECT_METHOD} - equals, hashCode, toString</li>
 *   <li>{@link MethodRole#LIFECYCLE} - Lifecycle callbacks (@PostConstruct, @PreDestroy, init, destroy)</li>
 *   <li>{@link MethodRole#VALIDATION} - Validation methods (validate*, check*, ensure*, verify*)</li>
 *   <li>{@link MethodRole#COMMAND} - CQRS commands (void return with params, modifies state)</li>
 *   <li>{@link MethodRole#QUERY} - CQRS queries (non-void return, read-only)</li>
 *   <li>{@link MethodRole#BUSINESS} - Other business methods</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MethodRoleDetector detector = new MethodRoleDetector();
 * Set<MethodRole> roles = detector.detect(methodNode, declaringType);
 * }</pre>
 *
 * @since 5.0.0
 */
public final class MethodRoleDetector {

    private static final Set<String> LIFECYCLE_ANNOTATIONS = Set.of(
            "javax.annotation.PostConstruct",
            "jakarta.annotation.PostConstruct",
            "javax.annotation.PreDestroy",
            "jakarta.annotation.PreDestroy",
            "org.springframework.beans.factory.InitializingBean",
            "org.springframework.beans.factory.DisposableBean");

    private static final Set<String> OBJECT_METHOD_NAMES = Set.of("equals", "hashCode", "toString");

    private static final Set<String> LIFECYCLE_METHOD_NAMES =
            Set.of("init", "destroy", "close", "dispose", "shutdown", "cleanup");

    private static final Set<String> FACTORY_METHOD_NAMES =
            Set.of("of", "from", "create", "build", "newInstance", "getInstance", "valueOf");

    /**
     * Creates a new MethodRoleDetector.
     */
    public MethodRoleDetector() {
        // Stateless detector
    }

    /**
     * Detects the semantic roles of a method.
     *
     * @param method the method to analyze
     * @param declaringType the type that declares this method (may be null for context-free detection)
     * @return the set of detected roles (may be empty)
     */
    public Set<MethodRole> detect(MethodNode method, TypeNode declaringType) {
        Set<MethodRole> roles = EnumSet.noneOf(MethodRole.class);

        String name = method.simpleName();
        boolean isVoid = method.isVoid();
        boolean hasParams = method.parameterCount() > 0;

        // GETTER: getX/isX, no params, non-void return
        if (isGetter(method)) {
            roles.add(MethodRole.GETTER);
        }

        // SETTER: setX, 1 param, void return
        if (isSetter(method)) {
            roles.add(MethodRole.SETTER);
        }

        // FACTORY: static, returns own type or is named like factory
        if (isFactory(method, declaringType)) {
            roles.add(MethodRole.FACTORY);
        }

        // OBJECT_METHOD: equals, hashCode, toString
        if (isObjectMethod(method)) {
            roles.add(MethodRole.OBJECT_METHOD);
        }

        // LIFECYCLE: @PostConstruct/@PreDestroy or named init/destroy/close/dispose
        if (isLifecycleMethod(method)) {
            roles.add(MethodRole.LIFECYCLE);
        }

        // VALIDATION: validate*, check*, ensure*, verify*, is* (boolean return)
        if (isValidationMethod(method)) {
            roles.add(MethodRole.VALIDATION);
        }

        // If none of the above special roles detected, determine COMMAND/QUERY/BUSINESS
        if (roles.isEmpty()) {
            if (isVoid && hasParams) {
                roles.add(MethodRole.COMMAND);
            } else if (!isVoid && !hasParams && isQueryLikeName(name)) {
                roles.add(MethodRole.QUERY);
            } else if (!isVoid && hasParams && isQueryLikeName(name)) {
                roles.add(MethodRole.QUERY);
            } else {
                roles.add(MethodRole.BUSINESS);
            }
        }

        return roles.isEmpty() ? Set.of() : Set.copyOf(roles);
    }

    private boolean isGetter(MethodNode method) {
        if (method.parameterCount() != 0 || method.isVoid()) {
            return false;
        }

        String name = method.simpleName();

        // getX pattern
        if (name.startsWith("get") && name.length() > 3) {
            return Character.isUpperCase(name.charAt(3));
        }

        // isX pattern for boolean
        if (name.startsWith("is") && name.length() > 2) {
            String returnType = method.returnType().rawQualifiedName();
            if ("boolean".equals(returnType) || "java.lang.Boolean".equals(returnType)) {
                return Character.isUpperCase(name.charAt(2));
            }
        }

        return false;
    }

    private boolean isSetter(MethodNode method) {
        if (method.parameterCount() != 1 || !method.isVoid()) {
            return false;
        }

        String name = method.simpleName();
        if (name.startsWith("set") && name.length() > 3) {
            return Character.isUpperCase(name.charAt(3));
        }

        return false;
    }

    private boolean isFactory(MethodNode method, TypeNode declaringType) {
        if (!method.modifiers().contains(JavaModifier.STATIC)) {
            return false;
        }

        String name = method.simpleName();

        // Named like a factory method
        if (FACTORY_METHOD_NAMES.contains(name)) {
            return true;
        }

        // Returns the declaring type
        if (declaringType != null) {
            String returnType = method.returnType().rawQualifiedName();
            return returnType.equals(declaringType.qualifiedName());
        }

        return false;
    }

    private boolean isObjectMethod(MethodNode method) {
        String name = method.simpleName();

        if (!OBJECT_METHOD_NAMES.contains(name)) {
            return false;
        }

        // Verify signature
        switch (name) {
            case "equals":
                return method.parameterCount() == 1
                        && method.parameters().get(0).type().rawQualifiedName().equals("java.lang.Object");
            case "hashCode":
                return method.parameterCount() == 0
                        && method.returnType().rawQualifiedName().equals("int");
            case "toString":
                return method.parameterCount() == 0
                        && method.returnType().rawQualifiedName().equals("java.lang.String");
            default:
                return false;
        }
    }

    private boolean isLifecycleMethod(MethodNode method) {
        // Check annotations
        if (hasAnyAnnotation(method.annotations(), LIFECYCLE_ANNOTATIONS)) {
            return true;
        }

        // Check naming pattern
        String lowerName = method.simpleName().toLowerCase();
        return LIFECYCLE_METHOD_NAMES.contains(lowerName);
    }

    private boolean isValidationMethod(MethodNode method) {
        String name = method.simpleName().toLowerCase();

        // validate*, check*, ensure*, verify*
        if (name.startsWith("validate")
                || name.startsWith("check")
                || name.startsWith("ensure")
                || name.startsWith("verify")) {
            return true;
        }

        // is* with boolean return (and not a getter pattern for single field)
        if (name.startsWith("is") && name.length() > 2) {
            String returnType = method.returnType().rawQualifiedName();
            if ("boolean".equals(returnType) || "java.lang.Boolean".equals(returnType)) {
                // Check if it's a validation like isValid, isComplete, not a getter like isActive field
                String suffix = name.substring(2).toLowerCase();
                return suffix.equals("valid")
                        || suffix.equals("complete")
                        || suffix.equals("empty")
                        || suffix.equals("present")
                        || suffix.equals("null")
                        || suffix.equals("blank")
                        || suffix.contains("valid")
                        || suffix.contains("check");
            }
        }

        return false;
    }

    private boolean isQueryLikeName(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.startsWith("get")
                || lowerName.startsWith("find")
                || lowerName.startsWith("list")
                || lowerName.startsWith("search")
                || lowerName.startsWith("fetch")
                || lowerName.startsWith("load")
                || lowerName.startsWith("query")
                || lowerName.startsWith("read")
                || lowerName.startsWith("count")
                || lowerName.startsWith("exists");
    }

    private boolean hasAnyAnnotation(List<AnnotationRef> annotations, Set<String> annotationNames) {
        return annotations.stream().anyMatch(a -> annotationNames.contains(a.qualifiedName()));
    }
}
