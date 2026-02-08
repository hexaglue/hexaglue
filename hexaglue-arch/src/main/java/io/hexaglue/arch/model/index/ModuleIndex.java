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

package io.hexaglue.arch.model.index;

import io.hexaglue.arch.model.TypeId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Index mapping types to their containing module in a multi-module project.
 *
 * <p>The ModuleIndex is the bridge between the unified architectural model and
 * the physical module layout. It answers questions like "which module does this
 * type belong to?" and "which types live in this module?".</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ModuleIndex index = model.moduleIndex().orElseThrow();
 *
 * // Find which module owns a type
 * index.moduleOf(TypeId.of("com.example.Order"))
 *     .ifPresent(mod -> System.out.println("Order is in: " + mod.moduleId()));
 *
 * // List all types in a module
 * index.typesInModule("banking-core")
 *     .forEach(typeId -> System.out.println("  " + typeId.simpleName()));
 *
 * // Find all infrastructure modules
 * index.modulesByRole(ModuleRole.INFRASTRUCTURE)
 *     .forEach(mod -> System.out.println("Infra module: " + mod.moduleId()));
 * }</pre>
 *
 * @since 5.0.0
 */
public final class ModuleIndex {

    private final Map<TypeId, ModuleDescriptor> typeToModule;
    private final Map<String, ModuleDescriptor> modulesById;

    private ModuleIndex(Map<TypeId, ModuleDescriptor> typeToModule, Map<String, ModuleDescriptor> modulesById) {
        this.typeToModule = Map.copyOf(typeToModule);
        this.modulesById = Map.copyOf(modulesById);
    }

    /**
     * Creates a new builder for constructing a ModuleIndex.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the module that contains the given type.
     *
     * @param typeId the type identifier
     * @return an optional containing the module descriptor, or empty if the type is not assigned to any module
     * @throws NullPointerException if typeId is null
     */
    public Optional<ModuleDescriptor> moduleOf(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        return Optional.ofNullable(typeToModule.get(typeId));
    }

    /**
     * Returns all type identifiers assigned to the given module.
     *
     * @param moduleId the module identifier
     * @return a stream of type identifiers in the module
     * @throws NullPointerException if moduleId is null
     */
    public Stream<TypeId> typesInModule(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        return typeToModule.entrySet().stream()
                .filter(entry -> entry.getValue().moduleId().equals(moduleId))
                .map(Map.Entry::getKey);
    }

    /**
     * Returns all registered modules.
     *
     * @return a stream of all module descriptors
     */
    public Stream<ModuleDescriptor> modules() {
        return modulesById.values().stream();
    }

    /**
     * Returns all modules with the given architectural role.
     *
     * @param role the module role to filter by
     * @return a stream of matching module descriptors
     * @throws NullPointerException if role is null
     */
    public Stream<ModuleDescriptor> modulesByRole(ModuleRole role) {
        Objects.requireNonNull(role, "role must not be null");
        return modulesById.values().stream().filter(m -> m.role() == role);
    }

    /**
     * Returns the number of registered modules.
     *
     * @return the module count
     */
    public int size() {
        return modulesById.size();
    }

    /**
     * Builder for {@link ModuleIndex}.
     *
     * @since 5.0.0
     */
    public static final class Builder {
        private final Map<TypeId, ModuleDescriptor> typeToModule = new HashMap<>();
        private final Map<String, ModuleDescriptor> modulesById = new HashMap<>();

        private Builder() {}

        /**
         * Registers a module in the index.
         *
         * @param module the module descriptor to register
         * @return this builder
         * @throws NullPointerException if module is null
         */
        public Builder addModule(ModuleDescriptor module) {
            Objects.requireNonNull(module, "module must not be null");
            modulesById.put(module.moduleId(), module);
            return this;
        }

        /**
         * Assigns a type to a module.
         *
         * @param typeId the type identifier
         * @param module the module descriptor
         * @return this builder
         * @throws NullPointerException if any argument is null
         */
        public Builder assignType(TypeId typeId, ModuleDescriptor module) {
            Objects.requireNonNull(typeId, "typeId must not be null");
            Objects.requireNonNull(module, "module must not be null");
            typeToModule.put(typeId, module);
            return this;
        }

        /**
         * Builds the ModuleIndex.
         *
         * @return a new immutable ModuleIndex
         */
        public ModuleIndex build() {
            return new ModuleIndex(typeToModule, modulesById);
        }
    }
}
