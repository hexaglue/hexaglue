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

package io.hexaglue.arch.builder;

import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.TypeSyntax;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Context providing additional information for classification decisions.
 *
 * <p>This context provides access to all types being analyzed and
 * pre-computed indices for efficient lookups during classification.</p>
 *
 * <h2>Available Information</h2>
 * <ul>
 *   <li>All types in scope via {@link #types()}</li>
 *   <li>Type lookup by qualified name via {@link #findType(String)}</li>
 *   <li>Repository-to-type mapping via {@link #repositoryDominantTypes()}</li>
 *   <li>Inheritance relationships via {@link #subtypesOf(String)}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // During classification, check if type is the primary type of a repository
 * if (context.repositoryDominantTypes().contains(type.qualifiedName())) {
 *     // High confidence that this is an aggregate root
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
public final class ClassificationContext {

    private final SyntaxProvider syntaxProvider;
    private final Set<String> repositoryDominantTypes;
    private final Map<String, Set<String>> subtypeIndex;

    /**
     * Creates a new ClassificationContext.
     *
     * @param syntaxProvider the syntax provider for type access
     * @param repositoryDominantTypes set of types that are primary types in repositories
     * @param subtypeIndex map from supertype qualified name to set of subtype qualified names
     */
    public ClassificationContext(
            SyntaxProvider syntaxProvider,
            Set<String> repositoryDominantTypes,
            Map<String, Set<String>> subtypeIndex) {
        this.syntaxProvider = Objects.requireNonNull(syntaxProvider, "syntaxProvider must not be null");
        this.repositoryDominantTypes =
                Set.copyOf(Objects.requireNonNull(repositoryDominantTypes, "repositoryDominantTypes must not be null"));
        this.subtypeIndex = Map.copyOf(Objects.requireNonNull(subtypeIndex, "subtypeIndex must not be null"));
    }

    /**
     * Returns all types in scope.
     *
     * @return a stream of all types
     */
    public Stream<TypeSyntax> types() {
        return syntaxProvider.types();
    }

    /**
     * Finds a type by its qualified name.
     *
     * @param qualifiedName the fully qualified type name
     * @return the type if found
     */
    public Optional<TypeSyntax> findType(String qualifiedName) {
        return syntaxProvider.type(qualifiedName);
    }

    /**
     * Returns the set of types that are primary types in repositories.
     *
     * <p>A type is considered a "repository dominant type" if it appears
     * as the primary type parameter in a repository interface. This is
     * strong evidence that the type is an aggregate root.</p>
     *
     * @return set of qualified names of repository dominant types
     */
    public Set<String> repositoryDominantTypes() {
        return repositoryDominantTypes;
    }

    /**
     * Returns the subtypes of a given type.
     *
     * @param supertypeQualifiedName the qualified name of the supertype
     * @return set of qualified names of direct subtypes
     */
    public Set<String> subtypesOf(String supertypeQualifiedName) {
        return subtypeIndex.getOrDefault(supertypeQualifiedName, Set.of());
    }

    /**
     * Checks if a type is a repository dominant type.
     *
     * @param qualifiedName the qualified name to check
     * @return true if the type is a primary type in a repository
     */
    public boolean isRepositoryDominantType(String qualifiedName) {
        return repositoryDominantTypes.contains(qualifiedName);
    }

    /**
     * Creates an empty context for testing.
     *
     * @param syntaxProvider the syntax provider
     * @return an empty context
     */
    public static ClassificationContext empty(SyntaxProvider syntaxProvider) {
        return new ClassificationContext(syntaxProvider, Set.of(), Map.of());
    }

    /**
     * Creates a builder for ClassificationContext.
     *
     * @param syntaxProvider the syntax provider
     * @return a new builder
     */
    public static Builder builder(SyntaxProvider syntaxProvider) {
        return new Builder(syntaxProvider);
    }

    /**
     * Builder for ClassificationContext.
     */
    public static final class Builder {

        private final SyntaxProvider syntaxProvider;
        private Set<String> repositoryDominantTypes = Set.of();
        private Map<String, Set<String>> subtypeIndex = Map.of();

        private Builder(SyntaxProvider syntaxProvider) {
            this.syntaxProvider = syntaxProvider;
        }

        /**
         * Sets the repository dominant types.
         *
         * @param types the set of repository dominant type qualified names
         * @return this builder
         */
        public Builder repositoryDominantTypes(Set<String> types) {
            this.repositoryDominantTypes = types;
            return this;
        }

        /**
         * Sets the subtype index.
         *
         * @param index the subtype index
         * @return this builder
         */
        public Builder subtypeIndex(Map<String, Set<String>> index) {
            this.subtypeIndex = index;
            return this;
        }

        /**
         * Builds the ClassificationContext.
         *
         * @return a new ClassificationContext
         */
        public ClassificationContext build() {
            return new ClassificationContext(syntaxProvider, repositoryDominantTypes, subtypeIndex);
        }
    }
}
