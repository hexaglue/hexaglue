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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MojoClasspathBuilder}.
 *
 * @since 6.0.0
 */
class MojoClasspathBuilderTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("buildClasspath")
    class BuildClasspath {

        @Test
        @DisplayName("should include target/classes when directory exists")
        void shouldIncludeTargetClassesWhenExists() throws IOException {
            Path outputDir = tempDir.resolve("target/classes");
            Files.createDirectories(outputDir);

            MavenProject project = createProject(outputDir);
            project.setArtifacts(Collections.emptySet());

            List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

            assertThat(classpath).containsExactly(outputDir);
        }

        @Test
        @DisplayName("should not include target/classes when directory does not exist")
        void shouldNotIncludeTargetClassesWhenMissing() {
            Path outputDir = tempDir.resolve("target/classes");

            MavenProject project = createProject(outputDir);
            project.setArtifacts(Collections.emptySet());

            List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

            assertThat(classpath).isEmpty();
        }

        @Test
        @DisplayName("should include dependency artifacts")
        void shouldIncludeDependencyArtifacts() throws IOException {
            Path outputDir = tempDir.resolve("target/classes");
            Path jarFile = tempDir.resolve("deps/some-lib.jar");
            Files.createDirectories(jarFile.getParent());
            Files.createFile(jarFile);

            MavenProject project = createProject(outputDir);
            project.setArtifacts(createArtifacts(jarFile.toFile()));

            List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

            assertThat(classpath).containsExactly(jarFile);
        }

        @Test
        @DisplayName("should include target/classes before dependency artifacts")
        void shouldIncludeTargetClassesBeforeDependencies() throws IOException {
            Path outputDir = tempDir.resolve("target/classes");
            Files.createDirectories(outputDir);

            Path jarFile = tempDir.resolve("deps/some-lib.jar");
            Files.createDirectories(jarFile.getParent());
            Files.createFile(jarFile);

            MavenProject project = createProject(outputDir);
            project.setArtifacts(createArtifacts(jarFile.toFile()));

            List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

            assertThat(classpath).hasSize(2);
            assertThat(classpath.get(0)).isEqualTo(outputDir);
            assertThat(classpath.get(1)).isEqualTo(jarFile);
        }

        @Test
        @DisplayName("should skip artifacts with null file")
        void shouldSkipArtifactsWithNullFile() throws IOException {
            Path outputDir = tempDir.resolve("target/classes");
            Files.createDirectories(outputDir);

            Artifact artifact = new DefaultArtifact(
                    "com.example", "unresolved", "1.0", "compile", "jar", "", new DefaultArtifactHandler("jar"));
            // artifact.setFile(null) — file is not set

            MavenProject project = createProject(outputDir);
            project.setArtifacts(Set.of(artifact));

            List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

            assertThat(classpath).containsExactly(outputDir);
        }

        @Test
        @DisplayName("should return immutable list")
        void shouldReturnImmutableList() {
            Path outputDir = tempDir.resolve("target/classes");

            MavenProject project = createProject(outputDir);
            project.setArtifacts(Collections.emptySet());

            List<Path> classpath = MojoClasspathBuilder.buildClasspath(project);

            assertThat(classpath).isUnmodifiable();
        }
    }

    // --- Helper methods ---

    private MavenProject createProject(Path outputDir) {
        Model model = new Model();
        Build build = new Build();
        build.setOutputDirectory(outputDir.toString());
        model.setBuild(build);
        return new MavenProject(model);
    }

    private Set<Artifact> createArtifacts(File... files) {
        Set<Artifact> artifacts = new HashSet<>();
        int i = 0;
        for (File file : files) {
            Artifact artifact = new DefaultArtifact(
                    "com.example", "dep-" + i, "1.0", "compile", "jar", "", new DefaultArtifactHandler("jar"));
            artifact.setFile(file);
            artifacts.add(artifact);
            i++;
        }
        return artifacts;
    }
}
