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

package io.hexaglue.core.classification.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link YamlCriteriaProfile}.
 */
@DisplayName("YamlCriteriaProfile")
class YamlCriteriaProfileTest {

    // =========================================================================
    // fromString()
    // =========================================================================

    @Nested
    @DisplayName("fromString()")
    class FromStringTest {

        @Test
        @DisplayName("should parse valid YAML with priorities")
        void shouldParseValidYaml() {
            String yaml =
                    """
                    priorities:
                      explicit-entity: 100
                      repository-dominant: 85
                      has-identity: 65
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("explicit-entity")).hasValue(100);
            assertThat(profile.priorityFor("repository-dominant")).hasValue(85);
            assertThat(profile.priorityFor("has-identity")).hasValue(65);
        }

        @Test
        @DisplayName("should return empty for non-configured criteria")
        void shouldReturnEmptyForNonConfigured() {
            String yaml =
                    """
                    priorities:
                      explicit-entity: 100
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("unknown-criteria")).isEmpty();
        }

        @Test
        @DisplayName("should handle empty priorities section")
        void shouldHandleEmptyPriorities() {
            String yaml = "priorities:";

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("any-criteria")).isEmpty();
        }

        @Test
        @DisplayName("should handle empty YAML")
        void shouldHandleEmptyYaml() {
            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString("");

            assertThat(profile.priorityFor("any-criteria")).isEmpty();
        }

        @Test
        @DisplayName("should handle YAML without priorities key")
        void shouldHandleYamlWithoutPrioritiesKey() {
            String yaml =
                    """
                    other-key:
                      some-value: 100
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("any-criteria")).isEmpty();
        }

        @Test
        @DisplayName("should support negative priorities")
        void shouldSupportNegativePriorities() {
            String yaml =
                    """
                    priorities:
                      disabled-criteria: -1
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("disabled-criteria")).hasValue(-1);
        }

        @Test
        @DisplayName("should support zero priority")
        void shouldSupportZeroPriority() {
            String yaml =
                    """
                    priorities:
                      zero-priority: 0
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("zero-priority")).hasValue(0);
        }

        @Test
        @DisplayName("should convert floating point numbers to integers")
        void shouldConvertFloatToInt() {
            String yaml =
                    """
                    priorities:
                      float-priority: 75.9
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("float-priority")).hasValue(75);
        }

        @Test
        @DisplayName("should throw on invalid priority value")
        void shouldThrowOnInvalidValue() {
            String yaml =
                    """
                    priorities:
                      invalid: "not a number"
                    """;

            assertThatThrownBy(() -> YamlCriteriaProfile.fromString(yaml))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid")
                    .hasMessageContaining("expected integer");
        }

        @Test
        @DisplayName("should throw on invalid priorities structure")
        void shouldThrowOnInvalidStructure() {
            String yaml =
                    """
                    priorities: "not a map"
                    """;

            assertThatThrownBy(() -> YamlCriteriaProfile.fromString(yaml))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a map");
        }
    }

    // =========================================================================
    // fromResource()
    // =========================================================================

    @Nested
    @DisplayName("fromResource()")
    class FromResourceTest {

        @Test
        @DisplayName("should load default.yaml from classpath")
        void shouldLoadDefaultProfile() {
            YamlCriteriaProfile profile = YamlCriteriaProfile.fromResource("profiles/default.yaml");

            // Verify some default priorities using stable IDs
            assertThat(profile.priorityFor("domain.explicit.aggregateRoot")).hasValue(100);
            assertThat(profile.priorityFor("domain.structural.repositoryDominant")).hasValue(80);
            assertThat(profile.source()).isEqualTo("resource:profiles/default.yaml");
        }

        @Test
        @DisplayName("should load strict.yaml from classpath")
        void shouldLoadStrictProfile() {
            YamlCriteriaProfile profile = YamlCriteriaProfile.fromResource("profiles/strict.yaml");

            // Verify strict priorities - heuristics are lowered (using stable IDs)
            assertThat(profile.priorityFor("domain.explicit.aggregateRoot")).hasValue(100);
            assertThat(profile.priorityFor("domain.structural.repositoryDominant")).hasValue(60);
            assertThat(profile.priorityFor("domain.naming.domainEvent")).hasValue(40);
        }

        @Test
        @DisplayName("should load annotation-only.yaml from classpath")
        void shouldLoadAnnotationOnlyProfile() {
            YamlCriteriaProfile profile = YamlCriteriaProfile.fromResource("profiles/annotation-only.yaml");

            // Verify annotation-only priorities - heuristics are disabled (using stable IDs)
            assertThat(profile.priorityFor("domain.explicit.aggregateRoot")).hasValue(100);
            assertThat(profile.priorityFor("domain.structural.repositoryDominant")).hasValue(-1);
            assertThat(profile.priorityFor("domain.naming.domainEvent")).hasValue(-1);
        }

        @Test
        @DisplayName("should throw on missing resource")
        void shouldThrowOnMissingResource() {
            assertThatThrownBy(() -> YamlCriteriaProfile.fromResource("profiles/nonexistent.yaml"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Resource not found");
        }
    }

    // =========================================================================
    // fromPath()
    // =========================================================================

    @Nested
    @DisplayName("fromPath()")
    class FromPathTest {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should load profile from file path")
        void shouldLoadFromPath() throws IOException {
            String yaml =
                    """
                    priorities:
                      custom-criteria: 150
                    """;
            Path file = tempDir.resolve("custom.yaml");
            Files.writeString(file, yaml);

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromPath(file);

            assertThat(profile.priorityFor("custom-criteria")).hasValue(150);
            assertThat(profile.source()).contains("custom.yaml");
        }

        @Test
        @DisplayName("should throw on missing file")
        void shouldThrowOnMissingFile() {
            Path nonexistent = tempDir.resolve("nonexistent.yaml");

            assertThatThrownBy(() -> YamlCriteriaProfile.fromPath(nonexistent))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to load profile");
        }
    }

    // =========================================================================
    // fromInputStream() and fromReader()
    // =========================================================================

    @Nested
    @DisplayName("fromInputStream() and fromReader()")
    class StreamAndReaderTest {

        @Test
        @DisplayName("should load from InputStream")
        void shouldLoadFromInputStream() {
            String yaml =
                    """
                    priorities:
                      stream-criteria: 88
                    """;
            ByteArrayInputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromInputStream(is, "test-stream");

            assertThat(profile.priorityFor("stream-criteria")).hasValue(88);
            assertThat(profile.source()).isEqualTo("test-stream");
        }

        @Test
        @DisplayName("should load from Reader")
        void shouldLoadFromReader() {
            String yaml =
                    """
                    priorities:
                      reader-criteria: 77
                    """;
            StringReader reader = new StringReader(yaml);

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromReader(reader, "test-reader");

            assertThat(profile.priorityFor("reader-criteria")).hasValue(77);
            assertThat(profile.source()).isEqualTo("test-reader");
        }
    }

    // =========================================================================
    // Integration with CriteriaProfile interface
    // =========================================================================

    @Nested
    @DisplayName("CriteriaProfile interface integration")
    class CriteriaProfileIntegrationTest {

        @Test
        @DisplayName("should work with resolvePriority()")
        void shouldWorkWithResolvePriority() {
            String yaml =
                    """
                    priorities:
                      test-criteria: 120
                    """;
            CriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);
            ClassificationCriteria<DomainKind> criteria = createCriteria("test-criteria", 80);

            // Override should win
            assertThat(profile.resolvePriority(criteria)).isEqualTo(120);
        }

        @Test
        @DisplayName("should fallback to criteria default when not overridden")
        void shouldFallbackToDefault() {
            String yaml =
                    """
                    priorities:
                      other-criteria: 120
                    """;
            CriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);
            ClassificationCriteria<DomainKind> criteria = createCriteria("test-criteria", 80);

            // Should use criteria's default
            assertThat(profile.resolvePriority(criteria)).isEqualTo(80);
        }
    }

    // =========================================================================
    // priorities() and toString()
    // =========================================================================

    @Nested
    @DisplayName("Accessors and toString()")
    class AccessorsTest {

        @Test
        @DisplayName("priorities() should return unmodifiable map")
        void prioritiesShouldReturnUnmodifiableMap() {
            String yaml =
                    """
                    priorities:
                      a: 100
                      b: 90
                    """;
            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorities()).hasSize(2).containsEntry("a", 100).containsEntry("b", 90);

            // Should be unmodifiable
            assertThatThrownBy(() -> profile.priorities().put("c", 80))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("toString() should include source and override count")
        void toStringShouldIncludeInfo() {
            String yaml =
                    """
                    priorities:
                      a: 100
                      b: 90
                    """;
            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.toString()).contains("string").contains("2");
        }
    }

    // =========================================================================
    // YAML with comments
    // =========================================================================

    @Nested
    @DisplayName("YAML comments handling")
    class CommentsTest {

        @Test
        @DisplayName("should ignore comments in YAML")
        void shouldIgnoreComments() {
            String yaml =
                    """
                    # This is a comment
                    priorities:
                      # Another comment
                      explicit-entity: 100  # Inline comment
                      # More comments
                      repository-dominant: 80
                    """;

            YamlCriteriaProfile profile = YamlCriteriaProfile.fromString(yaml);

            assertThat(profile.priorityFor("explicit-entity")).hasValue(100);
            assertThat(profile.priorityFor("repository-dominant")).hasValue(80);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private ClassificationCriteria<DomainKind> createCriteria(String name, int priority) {
        return new ClassificationCriteria<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public DomainKind targetKind() {
                return DomainKind.ENTITY;
            }

            @Override
            public MatchResult evaluate(TypeNode node, GraphQuery query) {
                return MatchResult.match(ConfidenceLevel.HIGH, "Test");
            }
        };
    }
}
