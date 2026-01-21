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

import io.hexaglue.arch.AnalysisMetadata;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.UnclassifiedType;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.Identifier;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.ports.PortClassification;
import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.TypeForm;
import io.hexaglue.syntax.TypeSyntax;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builds an ArchitecturalModel from a SyntaxProvider.
 *
 * <p>The builder orchestrates the classification process:</p>
 * <ol>
 *   <li>Index types for efficient lookups (repository types, subtypes)</li>
 *   <li>Build classification context</li>
 *   <li>Run DomainClassifier on classes/records</li>
 *   <li>Run PortClassifier on interfaces</li>
 *   <li>Create ArchElement instances</li>
 *   <li>Build and return ArchitecturalModel</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SyntaxProvider syntax = SpoonSyntaxProvider.builder()
 *     .basePackage("com.example")
 *     .sourceDirectory(Path.of("src/main/java"))
 *     .build();
 *
 * ArchitecturalModel model = ArchitecturalModelBuilder.builder(syntax)
 *     .projectName("MyProject")
 *     .build();
 *
 * model.aggregates().forEach(System.out::println);
 * }</pre>
 *
 * @since 4.0.0
 */
public final class ArchitecturalModelBuilder {

    private final SyntaxProvider syntaxProvider;
    private final DomainClassifier domainClassifier;
    private final PortClassifier portClassifier;
    private final String projectName;
    private final String basePackage;

    // v5 model components (optional - may be null for legacy usage)
    private final TypeRegistry typeRegistry;
    private final ClassificationReport classificationReport;
    private final DomainIndex domainIndex;
    private final PortIndex portIndex;

    private ArchitecturalModelBuilder(Builder builder) {
        this.syntaxProvider = builder.syntaxProvider;
        this.domainClassifier = builder.domainClassifier;
        this.portClassifier = builder.portClassifier;
        this.projectName = builder.projectName;
        this.basePackage = builder.basePackage;
        this.typeRegistry = builder.typeRegistry;
        this.classificationReport = builder.classificationReport;
        this.domainIndex = builder.domainIndex;
        this.portIndex = builder.portIndex;
    }

    /**
     * Builds the architectural model.
     *
     * @return a new ArchitecturalModel
     */
    public ArchitecturalModel build() {
        Instant startTime = Instant.now();

        // Build indices
        Set<String> repositoryDominantTypes = buildRepositoryTypeIndex();
        Map<String, Set<String>> subtypeIndex = buildSubtypeIndex();

        // Create classification context
        ClassificationContext context = ClassificationContext.builder(syntaxProvider)
                .repositoryDominantTypes(repositoryDominantTypes)
                .subtypeIndex(subtypeIndex)
                .build();

        // Create model builder
        ProjectContext project = ProjectContext.forTesting(projectName, basePackage);
        ArchitecturalModel.Builder modelBuilder = ArchitecturalModel.builder(project);

        // Classify all types
        int classifiedCount = 0;
        int unclassifiedCount = 0;

        List<TypeSyntax> allTypes = syntaxProvider.types().toList();
        for (TypeSyntax type : allTypes) {
            ClassificationTrace trace = classifyType(type, context);

            if (trace.classifiedAs() == ElementKind.UNCLASSIFIED) {
                modelBuilder.add(new UnclassifiedType(
                        ElementId.of(type.qualifiedName()),
                        trace.winningCriterion().explanation(),
                        type,
                        trace));
                unclassifiedCount++;
            } else {
                modelBuilder.add(createArchElement(type, trace));
                classifiedCount++;
            }
        }

        // Build metadata
        int totalTypes = classifiedCount + unclassifiedCount;
        AnalysisMetadata metadata =
                AnalysisMetadata.now(startTime, syntaxProvider.metadata().parserName(), totalTypes);

        // Add v5 components if provided
        if (typeRegistry != null) {
            modelBuilder.typeRegistry(typeRegistry);
        }
        if (classificationReport != null) {
            modelBuilder.classificationReport(classificationReport);
        }
        if (domainIndex != null) {
            modelBuilder.domainIndex(domainIndex);
        }
        if (portIndex != null) {
            modelBuilder.portIndex(portIndex);
        }

        return modelBuilder.build(metadata);
    }

    /**
     * Classifies a type using the appropriate classifier.
     */
    private ClassificationTrace classifyType(TypeSyntax type, ClassificationContext context) {
        // Interfaces go to port classifier
        if (type.form() == TypeForm.INTERFACE) {
            return portClassifier.classify(type, context);
        }
        // Classes, records, enums go to domain classifier
        return domainClassifier.classify(type, context);
    }

    /**
     * Creates an ArchElement based on the classification result.
     */
    private io.hexaglue.arch.ArchElement createArchElement(TypeSyntax type, ClassificationTrace trace) {
        ElementKind kind = trace.classifiedAs();

        return switch (kind) {
            case AGGREGATE_ROOT -> createDomainEntity(type, trace, ElementKind.AGGREGATE_ROOT);
            case ENTITY -> createDomainEntity(type, trace, ElementKind.ENTITY);
            case VALUE_OBJECT -> createValueObject(type, trace);
            case IDENTIFIER -> createIdentifier(type, trace);
            case DOMAIN_EVENT -> createDomainEvent(type, trace);
            case DRIVING_PORT -> createDrivingPort(type, trace);
            case DRIVEN_PORT -> createDrivenPort(type, trace);
            default -> new UnclassifiedType(ElementId.of(type.qualifiedName()), "Unhandled kind: " + kind, type, trace);
        };
    }

    private DomainEntity createDomainEntity(TypeSyntax type, ClassificationTrace trace, ElementKind entityKind) {
        // Find identity field - look for field named "id" or ending with "Id" that uses a value object type
        String identityField = null;
        io.hexaglue.syntax.TypeRef identityType = null;

        for (var field : type.fields()) {
            String fieldName = field.name().toLowerCase();
            if (fieldName.equals("id") || fieldName.endsWith("id")) {
                identityField = field.name();
                identityType = field.type();
                break;
            }
        }

        return new DomainEntity(
                ElementId.of(type.qualifiedName()),
                entityKind,
                identityField,
                identityType,
                Optional.empty(),
                List.of(),
                type,
                trace);
    }

    private ValueObject createValueObject(TypeSyntax type, ClassificationTrace trace) {
        List<String> fields = type.fields().stream().map(f -> f.name()).toList();
        return new ValueObject(ElementId.of(type.qualifiedName()), fields, type, trace);
    }

    private Identifier createIdentifier(TypeSyntax type, ClassificationTrace trace) {
        // Try to find the wrapped type from fields (e.g., UUID value)
        var wrappedType = type.fields().stream().findFirst().map(f -> f.type()).orElse(null);
        // Identifier name usually ends with "Id" - try to infer what it identifies
        String identifiesType = inferIdentifiedType(type.simpleName());
        return new Identifier(ElementId.of(type.qualifiedName()), wrappedType, identifiesType, type, trace);
    }

    private String inferIdentifiedType(String identifierName) {
        // OrderId -> Order, CustomerId -> Customer
        if (identifierName.endsWith("Id")) {
            return identifierName.substring(0, identifierName.length() - 2);
        }
        return null;
    }

    private DomainEvent createDomainEvent(TypeSyntax type, ClassificationTrace trace) {
        List<String> eventFields = type.fields().stream().map(f -> f.name()).toList();
        return new DomainEvent(ElementId.of(type.qualifiedName()), null, eventFields, type, trace);
    }

    private DrivingPort createDrivingPort(TypeSyntax type, ClassificationTrace trace) {
        return new DrivingPort(
                ElementId.of(type.qualifiedName()),
                PortClassification.USE_CASE,
                List.of(), // operations - populated later
                List.of(), // implementedBy - populated later
                type,
                trace);
    }

    private DrivenPort createDrivenPort(TypeSyntax type, ClassificationTrace trace) {
        // Determine if it's a repository based on naming
        PortClassification classification =
                type.simpleName().endsWith("Repository") ? PortClassification.REPOSITORY : PortClassification.GATEWAY;
        return new DrivenPort(
                ElementId.of(type.qualifiedName()),
                classification,
                List.of(), // operations - populated later
                Optional.empty(), // primaryManagedType - populated later
                List.of(), // managedTypes - populated later
                type,
                trace);
    }

    /**
     * Builds an index of types that are the primary type in repository interfaces.
     */
    private Set<String> buildRepositoryTypeIndex() {
        Set<String> repositoryTypes = new HashSet<>();

        syntaxProvider.types().forEach(type -> {
            if (type.form() == TypeForm.INTERFACE
                    && (type.simpleName().endsWith("Repository") || hasRepositoryAnnotation(type))) {
                // Extract the first type parameter as the dominant type
                if (!type.typeParameters().isEmpty()) {
                    // For generic Repository<T, ID>, T is the dominant type
                    // This is a simplification - in real impl we'd analyze type arguments
                }
                // Also check method signatures for return types
                type.methods().stream()
                        .filter(m -> !m.returnType().qualifiedName().startsWith("java."))
                        .filter(m -> !m.returnType().isPrimitive())
                        .forEach(m -> {
                            String returnType = m.returnType().qualifiedName();
                            // Skip common wrapper types
                            if (!returnType.startsWith("java.util.") && !returnType.startsWith("java.lang.")) {
                                repositoryTypes.add(returnType);
                            }
                        });
            }
        });

        return repositoryTypes;
    }

    private boolean hasRepositoryAnnotation(TypeSyntax type) {
        return type.annotations().stream().anyMatch(ann -> ann.simpleName().equals("Repository"));
    }

    /**
     * Builds an index of subtypes for each supertype.
     */
    private Map<String, Set<String>> buildSubtypeIndex() {
        Map<String, Set<String>> index = new HashMap<>();

        syntaxProvider.types().forEach(type -> {
            type.superType().ifPresent(superType -> {
                index.computeIfAbsent(superType.qualifiedName(), k -> new HashSet<>())
                        .add(type.qualifiedName());
            });

            for (var iface : type.interfaces()) {
                index.computeIfAbsent(iface.qualifiedName(), k -> new HashSet<>())
                        .add(type.qualifiedName());
            }
        });

        return index;
    }

    /**
     * Creates a new builder.
     *
     * @param syntaxProvider the syntax provider
     * @return a new builder
     */
    public static Builder builder(SyntaxProvider syntaxProvider) {
        return new Builder(syntaxProvider);
    }

    /**
     * Builder for ArchitecturalModelBuilder.
     */
    public static final class Builder {
        private final SyntaxProvider syntaxProvider;
        private DomainClassifier domainClassifier = DomainClassifiers.standard();
        private PortClassifier portClassifier = PortClassifiers.standard();
        private String projectName = "Unnamed Project";
        private String basePackage = "";

        // v5 model components
        private TypeRegistry typeRegistry;
        private ClassificationReport classificationReport;
        private DomainIndex domainIndex;
        private PortIndex portIndex;

        private Builder(SyntaxProvider syntaxProvider) {
            this.syntaxProvider = Objects.requireNonNull(syntaxProvider, "syntaxProvider must not be null");
        }

        /**
         * Sets a custom domain classifier.
         *
         * @param classifier the domain classifier
         * @return this builder
         */
        public Builder domainClassifier(DomainClassifier classifier) {
            this.domainClassifier = Objects.requireNonNull(classifier);
            return this;
        }

        /**
         * Sets a custom port classifier.
         *
         * @param classifier the port classifier
         * @return this builder
         */
        public Builder portClassifier(PortClassifier classifier) {
            this.portClassifier = Objects.requireNonNull(classifier);
            return this;
        }

        /**
         * Sets the project name.
         *
         * @param name the project name
         * @return this builder
         */
        public Builder projectName(String name) {
            this.projectName = name != null ? name : "Unnamed Project";
            return this;
        }

        /**
         * Sets the base package.
         *
         * @param basePackage the base package
         * @return this builder
         */
        public Builder basePackage(String basePackage) {
            this.basePackage = basePackage != null ? basePackage : "";
            return this;
        }

        /**
         * Sets the v5 type registry.
         *
         * @param registry the type registry from NewArchitecturalModelBuilder
         * @return this builder
         * @since 5.0.0
         */
        public Builder typeRegistry(TypeRegistry registry) {
            this.typeRegistry = registry;
            return this;
        }

        /**
         * Sets the v5 classification report.
         *
         * @param report the classification report from NewArchitecturalModelBuilder
         * @return this builder
         * @since 5.0.0
         */
        public Builder classificationReport(ClassificationReport report) {
            this.classificationReport = report;
            return this;
        }

        /**
         * Sets the v5 domain index.
         *
         * @param index the domain index from NewArchitecturalModelBuilder
         * @return this builder
         * @since 5.0.0
         */
        public Builder domainIndex(DomainIndex index) {
            this.domainIndex = index;
            return this;
        }

        /**
         * Sets the v5 port index.
         *
         * @param index the port index from NewArchitecturalModelBuilder
         * @return this builder
         * @since 5.0.0
         */
        public Builder portIndex(PortIndex index) {
            this.portIndex = index;
            return this;
        }

        /**
         * Builds the ArchitecturalModel.
         *
         * @return a new ArchitecturalModel
         */
        public ArchitecturalModel build() {
            return new ArchitecturalModelBuilder(this).build();
        }
    }
}
