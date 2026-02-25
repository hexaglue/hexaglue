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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MojoSourceRootsResolver}.
 *
 * @since 6.0.0
 */
class MojoSourceRootsResolverTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("resolveSourceRoots")
    class ResolveSourceRoots {

        @Test
        @DisplayName("should return standard source roots when no delombok output exists")
        void shouldReturnStandardSourceRootsWhenNoDelombok() {
            MavenProject project = createProject();

            List<Path> sourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project);

            assertThat(sourceRoots).containsExactly(Path.of(project.getBuild().getSourceDirectory()));
        }

        @Test
        @DisplayName("should substitute delombok output for original source directory")
        void shouldSubstituteDelombokOutput() throws IOException {
            MavenProject project = createProject();

            Path delombokDir =
                    Path.of(project.getBuild().getDirectory(), MojoSourceRootsResolver.DELOMBOK_OUTPUT_SUBDIR);
            Files.createDirectories(delombokDir);

            List<Path> sourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project);

            assertThat(sourceRoots).containsExactly(delombokDir);
        }

        @Test
        @DisplayName("should preserve additional source roots alongside delombok substitution")
        void shouldPreserveAdditionalSourceRoots() throws IOException {
            MavenProject project = createProject();

            Path delombokDir =
                    Path.of(project.getBuild().getDirectory(), MojoSourceRootsResolver.DELOMBOK_OUTPUT_SUBDIR);
            Files.createDirectories(delombokDir);

            Path generatedSources = tempDir.resolve("target/generated-sources/annotations");
            Files.createDirectories(generatedSources);
            project.addCompileSourceRoot(generatedSources.toString());

            List<Path> sourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project);

            assertThat(sourceRoots).containsExactly(delombokDir, generatedSources);
        }

        @Test
        @DisplayName("should filter out non-existing directories when delombok is active")
        void shouldFilterNonExistingDirectoriesWithDelombok() throws IOException {
            MavenProject project = createProject();

            Path delombokDir =
                    Path.of(project.getBuild().getDirectory(), MojoSourceRootsResolver.DELOMBOK_OUTPUT_SUBDIR);
            Files.createDirectories(delombokDir);

            // Add a source root that doesn't exist on disk
            project.addCompileSourceRoot(tempDir.resolve("nonexistent").toString());

            List<Path> sourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project);

            assertThat(sourceRoots).containsExactly(delombokDir);
        }

        @Test
        @DisplayName("should return all compile source roots without filtering when no delombok")
        void shouldReturnAllSourceRootsWithoutFiltering() {
            MavenProject project = createProject();
            Path additionalRoot = tempDir.resolve("extra-sources");
            project.addCompileSourceRoot(additionalRoot.toString());

            List<Path> sourceRoots = MojoSourceRootsResolver.resolveSourceRoots(project);

            assertThat(sourceRoots).contains(Path.of(project.getBuild().getSourceDirectory()), additionalRoot);
        }
    }

    @Nested
    @DisplayName("buildPathRemapping")
    class BuildPathRemapping {

        @Test
        @DisplayName("should return empty map when no delombok output exists")
        void shouldReturnEmptyMapWhenNoDelombok() {
            MavenProject project = createProject();

            Map<Path, Path> remapping = MojoSourceRootsResolver.buildPathRemapping(project);

            assertThat(remapping).isEmpty();
        }

        @Test
        @DisplayName("should return remapping from delombok to original source directory")
        void shouldReturnRemappingWhenDelombokExists() throws IOException {
            MavenProject project = createProject();

            Path delombokDir =
                    Path.of(project.getBuild().getDirectory(), MojoSourceRootsResolver.DELOMBOK_OUTPUT_SUBDIR);
            Files.createDirectories(delombokDir);

            Map<Path, Path> remapping = MojoSourceRootsResolver.buildPathRemapping(project);

            Path originalSourceDir = Path.of(project.getBuild().getSourceDirectory());
            assertThat(remapping).hasSize(1);
            assertThat(remapping).containsKey(delombokDir.toAbsolutePath().normalize());
            assertThat(remapping.get(delombokDir.toAbsolutePath().normalize()))
                    .isEqualTo(originalSourceDir.toAbsolutePath().normalize());
        }
    }

    // --- Helper methods ---

    private MavenProject createProject() {
        Path sourceDir = tempDir.resolve("src/main/java");
        Path targetDir = tempDir.resolve("target");

        Model model = new Model();
        Build build = new Build();
        build.setSourceDirectory(sourceDir.toString());
        build.setDirectory(targetDir.toString());
        build.setOutputDirectory(targetDir.resolve("classes").toString());
        model.setBuild(build);

        MavenProject project = new MavenProject(model);
        project.addCompileSourceRoot(sourceDir.toString());
        return project;
    }
}
