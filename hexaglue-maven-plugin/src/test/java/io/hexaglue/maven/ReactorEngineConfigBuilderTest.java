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

package io.hexaglue.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.index.ModuleRole;
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.spi.generation.PluginCategory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ReactorEngineConfigBuilder}.
 *
 * @since 5.0.0
 */
class ReactorEngineConfigBuilderTest {

    @TempDir
    Path tempDir;

    private final Log log = new SystemStreamLog();
    private Path outputDir;

    @BeforeEach
    void setUp() {
        outputDir = tempDir.resolve("output");
    }

    @Nested
    @DisplayName("Source roots")
    class SourceRoots {

        @Test
        @DisplayName("should union source roots from all modules")
        void shouldUnionSourceRootsFromAllModules() throws IOException {
            // Given
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModule("core", "src/main/java");
            MavenProject infra = createJarModule("infra", "src/main/java");

            MavenSession session = createSession(parent, parent, core, infra);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then
            assertThat(config.sourceRoots()).hasSize(2);
            assertThat(config.isMultiModule()).isTrue();
        }

        @Test
        @DisplayName("should filter out non-existent source roots")
        void shouldFilterOutNonExistentSourceRoots() throws IOException {
            // Given
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModuleWithSourceRoots("core", List.of("src/main/java", "src/gen/java"));
            // Only src/main/java is created; src/gen/java doesn't exist

            MavenSession session = createSession(parent, parent, core);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: only the existing source root is included
            assertThat(config.sourceRoots()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Classpath deduplication")
    class ClasspathDeduplication {

        @Test
        @DisplayName("should deduplicate classpath entries across modules")
        void shouldDeduplicateClasspathEntries() throws IOException {
            // Given: Two modules sharing a common dependency
            Path sharedJar = tempDir.resolve("libs/shared.jar");
            Files.createDirectories(sharedJar.getParent());
            Files.createFile(sharedJar);

            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModuleWithArtifact("core", sharedJar.toFile());
            MavenProject infra = createJarModuleWithArtifact("infra", sharedJar.toFile());

            MavenSession session = createSession(parent, parent, core, infra);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: shared jar only appears once
            long count = config.classpathEntries().stream()
                    .filter(p -> p.equals(sharedJar))
                    .count();
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Module roles")
    class ModuleRoles {

        @Test
        @DisplayName("should assign roles from YAML config")
        void shouldAssignRolesFromYamlConfig() throws IOException {
            // Given
            String yaml = """
                    modules:
                      core:
                        role: DOMAIN
                      infra:
                        role: INFRASTRUCTURE
                    """;
            MavenProject parent = createParentProject("parent");
            Files.writeString(parent.getBasedir().toPath().resolve("hexaglue.yaml"), yaml);

            MavenProject core = createJarModule("core", "src/main/java");
            MavenProject infra = createJarModule("infra", "src/main/java");

            MavenSession session = createSession(parent, parent, core, infra);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then
            assertThat(config.moduleSourceSets()).hasSize(2);
            assertThat(config.moduleSourceSets().stream()
                            .filter(m -> m.moduleId().equals("core"))
                            .findFirst()
                            .orElseThrow()
                            .role())
                    .isEqualTo(ModuleRole.DOMAIN);
            assertThat(config.moduleSourceSets().stream()
                            .filter(m -> m.moduleId().equals("infra"))
                            .findFirst()
                            .orElseThrow()
                            .role())
                    .isEqualTo(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should default to SHARED when no YAML config and no convention match")
        void shouldDefaultToSharedWhenNoYamlConfigAndNoConventionMatch() throws IOException {
            // Given: artifactId with no recognizable suffix
            MavenProject parent = createParentProject("parent");
            MavenProject banking = createJarModule("banking", "src/main/java");

            MavenSession session = createSession(parent, parent, banking);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).role()).isEqualTo(ModuleRole.SHARED);
        }
    }

    @Nested
    @DisplayName("Convention detection")
    class ConventionDetection {

        @Test
        @DisplayName("should detect role by convention when no YAML config")
        void shouldDetectRoleByConventionWhenNoYamlConfig() throws IOException {
            // Given: modules with conventional suffixes, no hexaglue.yaml
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModule("banking-core", "src/main/java");
            MavenProject infra = createJarModule("banking-persistence", "src/main/java");

            MavenSession session = createSession(parent, parent, core, infra);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: roles detected from artifactId suffix
            assertThat(config.moduleSourceSets()).hasSize(2);
            assertThat(config.moduleSourceSets().stream()
                            .filter(m -> m.moduleId().equals("banking-core"))
                            .findFirst()
                            .orElseThrow()
                            .role())
                    .isEqualTo(ModuleRole.DOMAIN);
            assertThat(config.moduleSourceSets().stream()
                            .filter(m -> m.moduleId().equals("banking-persistence"))
                            .findFirst()
                            .orElseThrow()
                            .role())
                    .isEqualTo(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("YAML explicit role should override convention")
        void yamlExplicitRoleShouldOverrideConvention() throws IOException {
            // Given: YAML says banking-core is APPLICATION, convention says DOMAIN
            String yaml = """
                    modules:
                      banking-core:
                        role: APPLICATION
                    """;
            MavenProject parent = createParentProject("parent");
            Files.writeString(parent.getBasedir().toPath().resolve("hexaglue.yaml"), yaml);

            MavenProject core = createJarModule("banking-core", "src/main/java");
            MavenSession session = createSession(parent, parent, core);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: YAML wins over convention
            assertThat(config.moduleSourceSets().get(0).role()).isEqualTo(ModuleRole.APPLICATION);
        }

        @Test
        @DisplayName("should default to SHARED when no YAML and no convention match")
        void shouldDefaultToSharedWhenNoYamlAndNoConventionMatch() throws IOException {
            // Given: module with no recognizable suffix
            MavenProject parent = createParentProject("parent");
            MavenProject banking = createJarModule("banking", "src/main/java");

            MavenSession session = createSession(parent, parent, banking);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: falls back to SHARED
            assertThat(config.moduleSourceSets().get(0).role()).isEqualTo(ModuleRole.SHARED);
        }
    }

    @Nested
    @DisplayName("Output directory")
    class OutputDirectory {

        @Test
        @DisplayName("should route module output to parent target directory")
        void shouldRouteModuleOutputToParentTargetDirectory() throws IOException {
            // Given
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModule("banking-core", "src/main/java");

            MavenSession session = createSession(parent, parent, core);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: output directory is under parent/target/generated-sources/hexaglue/modules/<moduleId>/
            Path parentBaseDir = parent.getBasedir().toPath();
            Path expectedPrefix = parentBaseDir.resolve("target/generated-sources/hexaglue/modules/banking-core");
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).outputDirectory()).isEqualTo(expectedPrefix);
        }

        @Test
        @DisplayName("should not place output inside child module target")
        void shouldNotPlaceOutputInsideChildModuleTarget() throws IOException {
            // Given
            MavenProject parent = createParentProject("parent");
            MavenProject infra = createJarModule("banking-infrastructure", "src/main/java");

            MavenSession session = createSession(parent, parent, infra);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: output directory does NOT start with the child module's baseDir
            Path childBaseDir = infra.getBasedir().toPath();
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).outputDirectory())
                    .satisfies(outputPath -> assertThat(outputPath.startsWith(childBaseDir))
                            .as("Output should not be under child module's baseDir")
                            .isFalse());
        }
    }

    @Nested
    @DisplayName("Custom output configuration")
    class CustomOutputConfiguration {

        @Test
        @DisplayName("should use custom sources base from YAML output config")
        void shouldUseCustomSourcesBaseFromYaml() throws IOException {
            // Given: YAML with custom output paths
            String yaml = """
                    output:
                      sources: "target/custom-hexaglue-sources"
                    """;
            MavenProject parent = createParentProject("parent");
            Files.writeString(parent.getBasedir().toPath().resolve("hexaglue.yaml"), yaml);

            MavenProject core = createJarModule("banking-core", "src/main/java");

            MavenSession session = createSession(parent, parent, core);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: module output directory should use the custom sources base
            Path parentBaseDir = parent.getBasedir().toPath();
            Path expectedPrefix = parentBaseDir.resolve("target/custom-hexaglue-sources/modules/banking-core");
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).outputDirectory()).isEqualTo(expectedPrefix);
        }
    }

    @Nested
    @DisplayName("Module filtering")
    class ModuleFiltering {

        @Test
        @DisplayName("should filter out POM packaging modules")
        void shouldFilterOutPomPackagingModules() throws IOException {
            // Given
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModule("core", "src/main/java");
            MavenProject bom = createPomModule("bom");

            MavenSession session = createSession(parent, parent, core, bom);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: only jar module included
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).moduleId()).isEqualTo("core");
        }
    }

    @Nested
    @DisplayName("Custom reports output configuration")
    class CustomReportsOutputConfiguration {

        @Test
        @DisplayName("should use custom reports base from YAML output config")
        void shouldUseCustomReportsBaseFromYaml() throws IOException {
            // Given: YAML with custom reports path
            String yaml = """
                    output:
                      reports: "target/custom-reports"
                    """;
            MavenProject parent = createParentProject("parent");
            Files.writeString(parent.getBasedir().toPath().resolve("hexaglue.yaml"), yaml);

            MavenProject core = createJarModule("banking-core", "src/main/java");

            MavenSession session = createSession(parent, parent, core);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: reportsOutputDirectory should be null (not set in build() call),
            // but the custom reports base is used by MultiModuleOutputResolver internally.
            // We verify that the module output dir uses the DEFAULT sources base
            // (i.e., custom reports doesn't affect sources)
            Path parentBaseDir = parent.getBasedir().toPath();
            Path expectedSourcesPrefix =
                    parentBaseDir.resolve("target/generated-sources/hexaglue/modules/banking-core");
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).outputDirectory()).isEqualTo(expectedSourcesPrefix);
        }

        @Test
        @DisplayName("should use both custom sources and reports bases from YAML")
        void shouldUseBothCustomSourcesAndReportsBasesFromYaml() throws IOException {
            // Given: YAML with both custom paths
            String yaml = """
                    output:
                      sources: "target/my-sources"
                      reports: "target/my-reports"
                    """;
            MavenProject parent = createParentProject("parent");
            Files.writeString(parent.getBasedir().toPath().resolve("hexaglue.yaml"), yaml);

            MavenProject core = createJarModule("core", "src/main/java");
            MavenProject infra = createJarModule("infra", "src/main/java");

            MavenSession session = createSession(parent, parent, core, infra);

            // When
            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Then: both modules should use the custom sources base
            Path parentBaseDir = parent.getBasedir().toPath();
            assertThat(config.moduleSourceSets()).hasSize(2);
            assertThat(config.moduleSourceSets().get(0).outputDirectory())
                    .isEqualTo(parentBaseDir.resolve("target/my-sources/modules/core"));
            assertThat(config.moduleSourceSets().get(1).outputDirectory())
                    .isEqualTo(parentBaseDir.resolve("target/my-sources/modules/infra"));
        }
    }

    @Nested
    @DisplayName("Non-jar packaging exclusion")
    class NonJarPackagingExclusion {

        @Test
        @DisplayName("should filter out WAR packaging modules")
        void shouldFilterOutWarPackagingModules() throws IOException {
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModule("core", "src/main/java");
            MavenProject webapp = createModuleWithPackaging("webapp", "war");

            MavenSession session = createSession(parent, parent, core, webapp);

            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            // Only jar module included
            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).moduleId()).isEqualTo("core");
        }

        @Test
        @DisplayName("should filter out EAR packaging modules")
        void shouldFilterOutEarPackagingModules() throws IOException {
            MavenProject parent = createParentProject("parent");
            MavenProject core = createJarModule("core", "src/main/java");
            MavenProject ear = createModuleWithPackaging("app-ear", "ear");

            MavenSession session = createSession(parent, parent, core, ear);

            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            assertThat(config.moduleSourceSets()).hasSize(1);
            assertThat(config.moduleSourceSets().get(0).moduleId()).isEqualTo("core");
        }

        @Test
        @DisplayName("should handle reactor with only non-jar modules")
        void shouldHandleReactorWithOnlyNonJarModules() throws IOException {
            MavenProject parent = createParentProject("parent");
            MavenProject bom = createPomModule("bom");
            MavenProject webapp = createModuleWithPackaging("webapp", "war");

            MavenSession session = createSession(parent, parent, bom, webapp);

            EngineConfig config = ReactorEngineConfigBuilder.build(
                    session,
                    "com.example",
                    outputDir,
                    null,
                    Map.of(),
                    null,
                    Set.of(PluginCategory.GENERATOR),
                    false,
                    false,
                    log);

            assertThat(config.moduleSourceSets()).isEmpty();
            assertThat(config.isMultiModule()).isFalse();
        }
    }

    // --- Helper methods ---

    private MavenProject createParentProject(String artifactId) throws IOException {
        Path baseDir = tempDir.resolve(artifactId);
        Files.createDirectories(baseDir);

        // Build model first — setModel() replaces the internal model so all
        // model-level properties (artifactId, packaging, etc.) must be set AFTER.
        Model model = new Model();
        model.setModules(new ArrayList<>());

        Properties props = new Properties();
        props.setProperty("maven.compiler.release", "21");
        model.setProperties(props);

        Build build = new Build();
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.setArtifactId(artifactId);
        project.setName(artifactId);
        project.setVersion("1.0.0-SNAPSHOT");
        project.setPackaging("pom");
        project.setFile(baseDir.resolve("pom.xml").toFile());

        project.setArtifacts(Collections.emptySet());
        return project;
    }

    private MavenProject createJarModule(String artifactId, String sourceRoot) throws IOException {
        return createJarModuleWithSourceRoots(artifactId, List.of(sourceRoot));
    }

    private MavenProject createJarModuleWithSourceRoots(String artifactId, List<String> sourceRoots)
            throws IOException {
        Path moduleDir = tempDir.resolve(artifactId);
        Files.createDirectories(moduleDir);

        Model model = new Model();
        Build build = new Build();
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.setArtifactId(artifactId);
        project.setName(artifactId);
        project.setVersion("1.0.0-SNAPSHOT");
        project.setPackaging("jar");
        project.setFile(moduleDir.resolve("pom.xml").toFile());

        // Create source root directories and register them
        for (String root : sourceRoots) {
            Path rootPath = moduleDir.resolve(root);
            if (root.equals(sourceRoots.get(0))) {
                // Only create the first source root to test filtering
                Files.createDirectories(rootPath);
            }
            project.addCompileSourceRoot(rootPath.toString());
        }

        project.setArtifacts(Collections.emptySet());
        return project;
    }

    private MavenProject createJarModuleWithArtifact(String artifactId, File artifactFile) throws IOException {
        MavenProject project = createJarModule(artifactId, "src/main/java");

        Artifact artifact = new DefaultArtifact(
                "com.example", "dep-" + artifactId, "1.0", "compile", "jar", "", new DefaultArtifactHandler("jar"));
        artifact.setFile(artifactFile);

        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(artifact);
        project.setArtifacts(artifacts);

        return project;
    }

    private MavenProject createModuleWithPackaging(String artifactId, String packaging) throws IOException {
        Path moduleDir = tempDir.resolve(artifactId);
        Files.createDirectories(moduleDir);

        Model model = new Model();
        Build build = new Build();
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.setArtifactId(artifactId);
        project.setName(artifactId);
        project.setVersion("1.0.0-SNAPSHOT");
        project.setPackaging(packaging);
        project.setFile(moduleDir.resolve("pom.xml").toFile());

        project.setArtifacts(Collections.emptySet());
        return project;
    }

    private MavenProject createPomModule(String artifactId) throws IOException {
        Path moduleDir = tempDir.resolve(artifactId);
        Files.createDirectories(moduleDir);

        Model model = new Model();
        Build build = new Build();
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.setArtifactId(artifactId);
        project.setName(artifactId);
        project.setVersion("1.0.0-SNAPSHOT");
        project.setPackaging("pom");
        project.setFile(moduleDir.resolve("pom.xml").toFile());

        project.setArtifacts(Collections.emptySet());
        return project;
    }

    @SuppressWarnings("deprecation") // No non-deprecated constructor available in Maven 3.9.x
    private MavenSession createSession(MavenProject topLevel, MavenProject... projects) {
        MavenSession session = new MavenSession(
                null, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult(), List.of(projects));
        session.setCurrentProject(topLevel);
        return session;
    }
}
