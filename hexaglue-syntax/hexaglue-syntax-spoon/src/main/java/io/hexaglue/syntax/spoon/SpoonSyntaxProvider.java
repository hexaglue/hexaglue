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

package io.hexaglue.syntax.spoon;

import io.hexaglue.syntax.SyntaxCapabilities;
import io.hexaglue.syntax.SyntaxMetadata;
import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.TypeSyntax;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

/**
 * Spoon-based implementation of {@link SyntaxProvider}.
 *
 * <p>Uses Spoon to parse Java source code and provide AST abstractions.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SyntaxProvider provider = SpoonSyntaxProvider.builder()
 *     .basePackage("com.example")
 *     .sourceDirectory(Path.of("src/main/java"))
 *     .build();
 *
 * provider.types().forEach(type -> {
 *     System.out.println(type.qualifiedName());
 * });
 * }</pre>
 *
 * @since 4.0.0
 */
public final class SpoonSyntaxProvider implements SyntaxProvider {

    private final String basePackage;
    private final List<Path> sourcePaths;
    private final CtModel model;
    private final SyntaxMetadata metadata;
    private final SyntaxCapabilities capabilities;
    private final Map<String, SpoonTypeSyntax> typeCache;

    private SpoonSyntaxProvider(Builder builder) {
        this.basePackage = builder.basePackage;
        this.sourcePaths = List.copyOf(builder.sourcePaths);
        this.capabilities = SyntaxCapabilities.spoon();
        this.typeCache = new ConcurrentHashMap<>();

        // Build Spoon model
        this.model = buildModel(builder);

        // Create metadata
        int typeCount = (int) this.types().count();
        this.metadata = new SyntaxMetadata(basePackage, sourcePaths, typeCount, Instant.now(), "Spoon");
    }

    private CtModel buildModel(Builder builder) {
        Launcher launcher = new Launcher();

        // Configure environment
        launcher.getEnvironment().setNoClasspath(builder.noClasspath);
        launcher.getEnvironment().setComplianceLevel(builder.javaVersion);
        launcher.getEnvironment().setAutoImports(true);

        // Add source directories
        for (Path path : sourcePaths) {
            launcher.addInputResource(path.toString());
        }

        // Build and return model
        launcher.buildModel();
        return launcher.getModel();
    }

    @Override
    public Stream<TypeSyntax> types() {
        return model.getAllTypes().stream()
                .filter(this::isInScope)
                .map(this::wrapType)
                .map(t -> (TypeSyntax) t);
    }

    @Override
    public Optional<TypeSyntax> type(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        // Check cache first
        SpoonTypeSyntax cached = typeCache.get(qualifiedName);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Search in model
        return model.getAllTypes().stream()
                .filter(t -> t.getQualifiedName().equals(qualifiedName))
                .filter(this::isInScope)
                .findFirst()
                .map(this::wrapType)
                .map(t -> (TypeSyntax) t);
    }

    @Override
    public SyntaxMetadata metadata() {
        return metadata;
    }

    @Override
    public SyntaxCapabilities capabilities() {
        return capabilities;
    }

    // ===== Helper methods =====

    /**
     * Checks if a type is in the analysis scope.
     */
    private boolean isInScope(CtType<?> type) {
        if (basePackage == null || basePackage.isEmpty()) {
            return true;
        }
        String qualifiedName = type.getQualifiedName();
        return qualifiedName.startsWith(basePackage);
    }

    /**
     * Wraps a Spoon type in SpoonTypeSyntax and caches it.
     */
    private SpoonTypeSyntax wrapType(CtType<?> ctType) {
        return typeCache.computeIfAbsent(ctType.getQualifiedName(), key -> new SpoonTypeSyntax(ctType));
    }

    // ===== Builder =====

    /**
     * Creates a new builder for SpoonSyntaxProvider.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SpoonSyntaxProvider}.
     */
    public static final class Builder {

        private String basePackage = "";
        private final List<Path> sourcePaths = new ArrayList<>();
        private boolean noClasspath = true;
        private int javaVersion = 17;

        private Builder() {}

        /**
         * Sets the base package to analyze.
         *
         * <p>Only types in this package and its subpackages will be included.</p>
         *
         * @param basePackage the base package (e.g., "com.example")
         * @return this builder
         */
        public Builder basePackage(String basePackage) {
            this.basePackage = basePackage != null ? basePackage : "";
            return this;
        }

        /**
         * Adds a source directory to analyze.
         *
         * @param path the path to the source directory
         * @return this builder
         */
        public Builder sourceDirectory(Path path) {
            Objects.requireNonNull(path, "path");
            this.sourcePaths.add(path);
            return this;
        }

        /**
         * Adds multiple source directories to analyze.
         *
         * @param paths the paths to the source directories
         * @return this builder
         */
        public Builder sourceDirectories(List<Path> paths) {
            Objects.requireNonNull(paths, "paths");
            this.sourcePaths.addAll(paths);
            return this;
        }

        /**
         * Sets whether to run without classpath resolution.
         *
         * <p>When true (default), Spoon can analyze code without compiling it.</p>
         *
         * @param noClasspath true to disable classpath resolution
         * @return this builder
         */
        public Builder noClasspath(boolean noClasspath) {
            this.noClasspath = noClasspath;
            return this;
        }

        /**
         * Sets the Java version for compliance level.
         *
         * @param version the Java version (e.g., 17)
         * @return this builder
         */
        public Builder javaVersion(int version) {
            this.javaVersion = version;
            return this;
        }

        /**
         * Builds the SpoonSyntaxProvider.
         *
         * @return a new SpoonSyntaxProvider
         * @throws IllegalStateException if no source directories were added
         */
        public SpoonSyntaxProvider build() {
            if (sourcePaths.isEmpty()) {
                throw new IllegalStateException("At least one source directory must be specified");
            }
            return new SpoonSyntaxProvider(this);
        }
    }
}
